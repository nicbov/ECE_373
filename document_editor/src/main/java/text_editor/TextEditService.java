package text_editor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Coordinates document edits, persistence, the section tree, and edit history.
 * Text positions are zero-based visible-text offsets, with end-exclusive ranges.
 */
public final class TextEditService {

    private record Patch(int offset, String expected, String replacement) { }

    private final Document document;
    private final FormatEditor formatEditor;

    public TextEditService(Document document) {
        this(document, new FormatEditor());
    }

    TextEditService(Document document, FormatEditor formatEditor) {
        if (document == null || formatEditor == null) {
            throw new IllegalArgumentException("Document and format editor are required.");
        }
        this.document = document;
        this.formatEditor = formatEditor;
    }

    /** Inserts visible, unformatted text at a paragraph-relative position. */
    public void addText(String paragraphId, int position, String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text is required.");
        }
        if (text.isEmpty()) {
            return;
        }
        editParagraph(paragraphId, raw -> insertVisible(raw, position, text));
    }

    /** Removes visible text from the paragraph-relative range [start, end). */
    public void removeText(String paragraphId, int start, int end) {
        editParagraph(paragraphId, raw -> removeVisible(raw, start, end));
    }

    public void addFormat(
            String paragraphId, int start, int end, TagType type, String argument) {
        editParagraph(paragraphId, raw -> formatEditor.addFormat(raw, start, end, type, argument));
    }

    public void removeFormat(String paragraphId, int start, int end, TagType type) {
        editParagraph(paragraphId, raw -> formatEditor.removeFormat(raw, start, end, type));
    }

    /** Adds a top-level section and returns its hierarchical label. */
    public String addSection(String name) {
        SectionTree candidate = currentTreeCopy();
        String label = candidate.addSection(new Section(name));
        commit(candidate);
        return label;
    }

    /** Adds a subsection to the section identified by its hierarchical label. */
    public String addSubSection(String parentSectionId, String name) {
        SectionTree candidate = currentTreeCopy();
        String label = candidate.addSubSection(new Section(name), parentSectionId);
        commit(candidate);
        return label;
    }

    public void removeSection(String sectionId) {
        SectionTree candidate = currentTreeCopy();
        candidate.removeSection(sectionId);
        commit(candidate);
    }

    /** Adds plain visible text as a paragraph and returns its hierarchical ID. */
    public String addParagraph(String sectionId, String text) {
        if (text == null) {
            throw new IllegalArgumentException("Paragraph text is required.");
        }
        SectionTree candidate = currentTreeCopy();
        Paragraph paragraph = new Paragraph(MarkupParser.escape(text));
        candidate.addParagraph(paragraph, sectionId);
        String paragraphId = paragraph.getParagraphID();
        commit(candidate);
        return paragraphId;
    }

    public void removeParagraph(String paragraphId) {
        SectionTree candidate = currentTreeCopy();
        Paragraph paragraph = candidate.findParagraph(paragraphId).orElseThrow(
                () -> new IllegalArgumentException("No paragraph with ID '" + paragraphId + "'"));
        int separator = paragraphId.lastIndexOf('#');
        String sectionId = paragraphId.substring(0, separator);
        Section section = candidate.find(sectionId).orElseThrow(
                () -> new IllegalArgumentException("No section with label '" + sectionId + "'"));
        int index = section.getParagraphs().indexOf(paragraph);
        section.removeParagraph(index + 1);
        commit(candidate);
    }

    /** Reverses the most recent document edit, if one is available. */
    public boolean undo() {
        StackHistoryService history = document.getHistoryService();
        synchronized (history) {
            Optional<StackHistoryService.Edit> next = history.peekUndo();
            if (next.isEmpty()) {
                return false;
            }
            restoreEdit(next.get().inverse(), next.get(), history::undo);
            return true;
        }
    }

    /** Reapplies the next undone document edit, if one is available. */
    public boolean redo() {
        StackHistoryService history = document.getHistoryService();
        synchronized (history) {
            Optional<StackHistoryService.Edit> next = history.peekRedo();
            if (next.isEmpty()) {
                return false;
            }
            restoreEdit(next.get().command(), next.get(), history::redo);
            return true;
        }
    }

    private void editParagraph(String paragraphId, UnaryOperator<String> change) {
        SectionTree candidate = currentTreeCopy();
        Paragraph paragraph = candidate.findParagraph(paragraphId).orElseThrow(
                () -> new IllegalArgumentException("No paragraph with ID '" + paragraphId + "'"));
        paragraph.setText(change.apply(paragraph.getText()));
        commit(candidate);
    }

    private SectionTree currentTreeCopy() {
        return DocumentMarkupCodec.copy(document.getSectionTree());
    }

    private void commit(SectionTree candidate) {
        String before = DocumentMarkupCodec.encode(document.getSectionTree());
        String after = DocumentMarkupCodec.encode(candidate);
        if (before.equals(after)) {
            return;
        }
        SectionTree validated = DocumentMarkupCodec.decode(after);
        document.getFileService().save(after);
        try {
            document.getHistoryService().record(new StackHistoryService.Edit(
                    encodePatch(createPatch(before, after)),
                    encodePatch(createPatch(after, before))));
        } catch (RuntimeException historyFailure) {
            restoreFile(before, historyFailure);
            throw historyFailure;
        }
        document.getSectionTree().replaceWith(validated);
        document.touch();
    }

    private void restoreEdit(
            String encodedPatch,
            StackHistoryService.Edit expectedEdit,
            java.util.function.Supplier<Optional<StackHistoryService.Edit>> historyTransition) {
        String current = document.getFileService().load();
        String restoredRaw = applyPatch(current, decodePatch(encodedPatch));
        SectionTree restored = DocumentMarkupCodec.decode(restoredRaw);
        document.getFileService().save(restoredRaw);
        try {
            Optional<StackHistoryService.Edit> transitioned = historyTransition.get();
            if (transitioned.isEmpty() || !transitioned.get().equals(expectedEdit)) {
                throw new IllegalStateException("Document history changed during undo/redo.");
            }
        } catch (RuntimeException historyFailure) {
            restoreFile(current, historyFailure);
            throw historyFailure;
        }
        document.getSectionTree().replaceWith(restored);
        document.touch();
    }

    private void restoreFile(String content, RuntimeException originalFailure) {
        try {
            document.getFileService().save(content);
        } catch (RuntimeException restoreFailure) {
            originalFailure.addSuppressed(restoreFailure);
        }
    }

    private static String insertVisible(String raw, int position, String text) {
        List<Token> tokens = MarkupParser.parse(raw);
        int visibleLength = MarkupParser.visibleText(tokens).length();
        if (position < 0 || position > visibleLength) {
            throw new IllegalArgumentException(
                    "Insertion position " + position + " is outside 0.." + visibleLength);
        }

        int visibleStart = 0;
        int rawOffset = raw.length();
        for (Token token : tokens) {
            if (token.kind() != Token.Kind.TEXT) {
                continue;
            }
            int tokenEnd = visibleStart + token.visible().length();
            if (position <= tokenEnd) {
                rawOffset = rawOffsetWithinText(raw, token, position - visibleStart);
                break;
            }
            visibleStart = tokenEnd;
        }
        return raw.substring(0, rawOffset) + MarkupParser.escape(text) + raw.substring(rawOffset);
    }

    private static int rawOffsetWithinText(String raw, Token token, int visibleOffset) {
        int rawOffset = token.rawStart();
        for (int i = 0; i < visibleOffset; i++) {
            char character = token.visible().charAt(i);
            rawOffset += character == '<' || character == '*' ? 2 : 1;
        }
        return rawOffset;
    }

    private static String removeVisible(String raw, int start, int end) {
        List<Token> tokens = MarkupParser.parse(raw);
        int visibleLength = MarkupParser.visibleText(tokens).length();
        if (start < 0 || end > visibleLength || start >= end) {
            throw new IllegalArgumentException(
                    "Range [" + start + "," + end + ") is outside 0.." + visibleLength);
        }

        StringBuilder result = new StringBuilder();
        int visibleStart = 0;
        for (Token token : tokens) {
            if (token.kind() != Token.Kind.TEXT) {
                result.append(raw, token.rawStart(), token.rawEnd());
                continue;
            }
            int tokenEnd = visibleStart + token.visible().length();
            int removeStart = Math.max(start, visibleStart);
            int removeEnd = Math.min(end, tokenEnd);
            if (removeStart >= removeEnd) {
                result.append(raw, token.rawStart(), token.rawEnd());
            } else {
                int localStart = removeStart - visibleStart;
                int localEnd = removeEnd - visibleStart;
                result.append(MarkupParser.escape(token.visible().substring(0, localStart)));
                result.append(MarkupParser.escape(token.visible().substring(localEnd)));
            }
            visibleStart = tokenEnd;
        }
        return result.toString();
    }

    private static Patch createPatch(String before, String after) {
        int prefix = 0;
        int sharedLength = Math.min(before.length(), after.length());
        while (prefix < sharedLength && before.charAt(prefix) == after.charAt(prefix)) {
            prefix++;
        }

        int beforeEnd = before.length();
        int afterEnd = after.length();
        while (beforeEnd > prefix && afterEnd > prefix
                && before.charAt(beforeEnd - 1) == after.charAt(afterEnd - 1)) {
            beforeEnd--;
            afterEnd--;
        }
        return new Patch(
                prefix,
                before.substring(prefix, beforeEnd),
                after.substring(prefix, afterEnd));
    }

    private static String encodePatch(Patch patch) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String expected = encoder.encodeToString(patch.expected().getBytes(StandardCharsets.UTF_16BE));
        String replacement = encoder.encodeToString(patch.replacement().getBytes(StandardCharsets.UTF_16BE));
        return "P\t" + patch.offset() + "\t" + expected + "\t" + replacement;
    }

    private static Patch decodePatch(String encoded) {
        String[] parts = encoded.split("\t", -1);
        if (parts.length != 4 || !parts[0].equals("P")) {
            throw new IllegalArgumentException("Invalid document edit in history.");
        }
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            return new Patch(
                    Integer.parseInt(parts[1]),
                    new String(decoder.decode(parts[2]), StandardCharsets.UTF_16BE),
                    new String(decoder.decode(parts[3]), StandardCharsets.UTF_16BE));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid document edit in history.", e);
        }
    }

    private static String applyPatch(String raw, Patch patch) {
        if (patch.offset() < 0 || patch.offset() > raw.length() - patch.expected().length()
                || !raw.regionMatches(patch.offset(), patch.expected(), 0, patch.expected().length())) {
            throw new IllegalStateException("Document content does not match the edit history.");
        }
        int end = patch.offset() + patch.expected().length();
        return raw.substring(0, patch.offset()) + patch.replacement() + raw.substring(end);
    }
}
