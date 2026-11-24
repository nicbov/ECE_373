package text_editor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import presentation.DocumentReadView;

/**
 * Coordinates document edits, persistence, the section tree, and edit history.
 * Text positions are zero-based visible-text offsets, with end-exclusive ranges.
 */
public final class TextEditService {

    private record Patch(int offset, String expected, String replacement) { }

    private record StyleFrame(TagType type, String previous) { }

    private record StyledText(String text, Map<TagType, String> style) { }

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

    // inserts visible text using formatting active at the raw insertion point
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

    // replaces one visible range as one saved and undoable edit
    public void replaceText(String paragraphId, int start, int end, String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text is required.");
        }
        editParagraph(paragraphId, raw -> replaceVisible(raw, start, end, text));
    }

    // replaces one visible range in a section title
    public void replaceSectionTitle(
            String sectionId, int start, int end, String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text is required.");
        }
        SectionTree candidate = currentTreeCopy();
        Section section = candidate.find(sectionId).orElseThrow(
                () -> new IllegalArgumentException("No section with label '" + sectionId + "'"));
        section.setName(replaceVisible(section.getName(), start, end, text));
        commit(candidate);
    }

    public void addFormat(
            String paragraphId, int start, int end, TagType type, String argument) {
        editParagraph(paragraphId, raw -> formatEditor.addFormat(raw, start, end, type, argument));
    }

    public void removeFormat(String paragraphId, int start, int end, TagType type) {
        editParagraph(paragraphId, raw -> formatEditor.removeFormat(raw, start, end, type));
    }

    /** Adds formatting to a visible range in a section title. */
    public void addSectionTitleFormat(
            String sectionId, int start, int end, TagType type, String argument) {
        editSectionTitle(
                sectionId,
                raw -> formatEditor.addFormat(raw, start, end, type, argument));
    }

    /** Removes formatting from a visible range in a section title. */
    public void removeSectionTitleFormat(
            String sectionId, int start, int end, TagType type) {
        editSectionTitle(
                sectionId,
                raw -> formatEditor.removeFormat(raw, start, end, type));
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

    /** Moves a section and its complete subtree. */
    public void moveSection(String sourceId, String destinationParentId, int index) {
        SectionTree candidate = currentTreeCopy();
        candidate.moveSection(sourceId, destinationParentId, index);
        commit(candidate);
    }

    /** Adds plain visible text as a paragraph and returns its hierarchical ID. */
    public String addParagraph(String sectionId, String text) {
        SectionTree candidate = currentTreeCopy();
        Section section = candidate.find(sectionId).orElseThrow(
                () -> new IllegalArgumentException("No section with label '" + sectionId + "'"));
        return insertParagraph(sectionId, section.countParagraphs() + 1, text);
    }

    // inserts at a one-based position and saves the complete change once
    public String insertParagraph(String sectionId, int index, String text) {
        if (text == null) {
            throw new IllegalArgumentException("Paragraph text is required.");
        }
        SectionTree candidate = currentTreeCopy();
        Paragraph paragraph = new Paragraph(MarkupParser.escape(text));
        candidate.insertParagraph(paragraph, sectionId, index);
        String paragraphId = paragraph.getParagraphID();
        commit(candidate);
        return paragraphId;
    }

    public void removeParagraph(String paragraphId) {
        SectionTree candidate = currentTreeCopy();
        candidate.removeParagraph(paragraphId);
        commit(candidate);
    }

    // moves a visible range using positions from before the edit begins
    public void moveText(
            String sourceParagraphId,
            int start,
            int end,
            String destinationParagraphId,
            int destinationPosition) {
        editDocument(tree -> {
            Paragraph source = requireParagraph(tree, sourceParagraphId);
            Paragraph destination = requireParagraph(tree, destinationParagraphId);
            List<StyledText> sourceRuns = styledText(source.getText());
            int sourceLength = styledLength(sourceRuns);
            checkRange(start, end, sourceLength);

            // the selected runs carry their formatting to the new position
            List<StyledText> moved = sliceStyled(sourceRuns, start, end);
            if (source == destination) {
                if (destinationPosition < 0 || destinationPosition > sourceLength) {
                    throw invalidPosition(destinationPosition, sourceLength);
                }
                if (destinationPosition > start && destinationPosition < end) {
                    throw new IllegalArgumentException(
                            "Destination position cannot be inside the moved range.");
                }
                if (destinationPosition == start || destinationPosition == end) {
                    return;
                }
                int adjustedPosition = destinationPosition > end
                        ? destinationPosition - (end - start)
                        : destinationPosition;
                List<StyledText> remaining = removeStyled(sourceRuns, start, end);
                source.setText(encodeStyled(insertStyled(
                        remaining, adjustedPosition, moved)));
                return;
            }

            List<StyledText> destinationRuns = styledText(destination.getText());
            int destinationLength = styledLength(destinationRuns);
            if (destinationPosition < 0 || destinationPosition > destinationLength) {
                throw invalidPosition(destinationPosition, destinationLength);
            }
            source.setText(encodeStyled(removeStyled(sourceRuns, start, end)));
            destination.setText(encodeStyled(insertStyled(
                    destinationRuns, destinationPosition, moved)));
        });
    }

    // splits one paragraph and returns the id of the new right paragraph
    public String splitParagraph(String paragraphId, int position) {
        SectionTree candidate = currentTreeCopy();
        Paragraph paragraph = requireParagraph(candidate, paragraphId);
        List<StyledText> runs = styledText(paragraph.getText());
        int length = styledLength(runs);
        if (position < 0 || position > length) {
            throw invalidPosition(position, length);
        }

        int hash = paragraphId.lastIndexOf('#');
        String sectionId = paragraphId.substring(0, hash);
        int paragraphPosition = Integer.parseInt(paragraphId.substring(hash + 1));
        paragraph.setText(encodeStyled(sliceStyled(runs, 0, position)));
        Paragraph rightParagraph = new Paragraph(
                encodeStyled(sliceStyled(runs, position, length)));

        // elias's tree helper updates the ids and locations after insertion
        candidate.insertParagraph(rightParagraph, sectionId, paragraphPosition + 1);
        String rightParagraphId = rightParagraph.getParagraphID();
        commit(candidate);
        return rightParagraphId;
    }

    // merges two neighboring paragraphs in reading order
    public void mergeParagraphs(String firstParagraphId, String secondParagraphId) {
        editDocument(tree -> {
            int firstHash = requireParagraphSeparator(firstParagraphId);
            int secondHash = requireParagraphSeparator(secondParagraphId);
            String firstSectionId = firstParagraphId.substring(0, firstHash);
            String secondSectionId = secondParagraphId.substring(0, secondHash);
            int firstPosition = parseParagraphPosition(firstParagraphId, firstHash);
            int secondPosition = parseParagraphPosition(secondParagraphId, secondHash);
            if (!firstSectionId.equals(secondSectionId)
                    || secondPosition != firstPosition + 1) {
                throw new IllegalArgumentException(
                        "Paragraphs must be neighbors in the same section.");
            }

            Paragraph first = requireParagraph(tree, firstParagraphId);
            Paragraph second = requireParagraph(tree, secondParagraphId);
            List<StyledText> combined = styledText(first.getText());
            combined.addAll(styledText(second.getText()));
            first.setText(encodeStyled(combined));

            // removal reuses elias's id and location updates
            tree.removeParagraph(secondParagraphId);
        });
    }

    // replaces every search result on one copied tree
    void replaceMatches(List<SearchMatch> matches, String replacement) {
        if (matches == null || replacement == null) {
            throw new IllegalArgumentException("Matches and replacement text are required.");
        }
        if (matches.isEmpty()) {
            return;
        }

        editDocument(tree -> {
            // working backward keeps earlier match positions from changing
            for (int i = matches.size() - 1; i >= 0; i--) {
                SearchMatch match = matches.get(i);

                // titles live on sections while normal text lives in paragraphs
                if (match.blockKind() == DocumentReadView.Kind.TITLE) {
                    Section section = tree.find(match.blockId()).orElseThrow(
                            () -> new IllegalArgumentException(
                                    "No section with label '" + match.blockId() + "'"));
                    section.setName(replaceVisible(
                            section.getName(),
                            match.startIndex(),
                            match.endIndex(),
                            replacement));
                } else {
                    Paragraph paragraph = requireParagraph(tree, match.blockId());
                    paragraph.setText(replaceVisible(
                            paragraph.getText(),
                            match.startIndex(),
                            match.endIndex(),
                            replacement));
                }
            }
        });
    }

    // future multi-paragraph operations use one copied tree and one history item
    void editDocument(Consumer<SectionTree> change) {
        if (change == null) {
            throw new IllegalArgumentException("Document change is required.");
        }
        // this keeps elias's copy then validate then save flow in one place
        SectionTree candidate = currentTreeCopy();
        // nothing reaches the real document if any part of this change fails
        change.accept(candidate);
        commit(candidate);
    }

    private static Paragraph requireParagraph(SectionTree tree, String paragraphId) {
        return tree.findParagraph(paragraphId).orElseThrow(
                () -> new IllegalArgumentException("No paragraph with ID '" + paragraphId + "'"));
    }

    private static int requireParagraphSeparator(String paragraphId) {
        if (paragraphId == null || paragraphId.lastIndexOf('#') < 1) {
            throw new IllegalArgumentException("Invalid paragraph ID '" + paragraphId + "'");
        }
        return paragraphId.lastIndexOf('#');
    }

    private static int parseParagraphPosition(String paragraphId, int hash) {
        try {
            return Integer.parseInt(paragraphId.substring(hash + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid paragraph ID '" + paragraphId + "'", e);
        }
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

    /** Starts a group whose successful edits will share one history entry. */
    public void beginEditGroup() {
        document.beginEditGroup(DocumentMarkupCodec.encode(document.getSectionTree()));
    }

    /** Completes the active group and records its net change once. */
    public void endEditGroup() {
        if (!document.isEditGroupActive()) {
            throw new IllegalStateException("No edit group is active.");
        }
        String before = document.getEditGroupStart();
        String after = DocumentMarkupCodec.encode(document.getSectionTree());
        document.clearEditGroup();
        if (before.equals(after)) {
            return;
        }
        try {
            document.getHistoryService().record(new StackHistoryService.Edit(
                    encodePatch(createPatch(before, after)),
                    encodePatch(createPatch(after, before))));
        } catch (RuntimeException historyFailure) {
            restoreSnapshot(before);
            throw historyFailure;
        }
    }

    /** Cancels the active group and restores its starting state. */
    public void abortEditGroup() {
        if (!document.isEditGroupActive()) {
            return;
        }
        String before = document.getEditGroupStart();
        document.clearEditGroup();
        restoreSnapshot(before);
    }

    private void editParagraph(String paragraphId, UnaryOperator<String> change) {
        SectionTree candidate = currentTreeCopy();
        Paragraph paragraph = candidate.findParagraph(paragraphId).orElseThrow(
                () -> new IllegalArgumentException("No paragraph with ID '" + paragraphId + "'"));
        paragraph.setText(change.apply(paragraph.getText()));
        commit(candidate);
    }

    private void editSectionTitle(String sectionId, UnaryOperator<String> change) {
        SectionTree candidate = currentTreeCopy();
        Section section = candidate.find(sectionId).orElseThrow(
                () -> new IllegalArgumentException("No section with label '" + sectionId + "'"));
        section.setName(change.apply(section.getName()));
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
        if (document.isEditGroupActive()) {
            document.getSectionTree().replaceWith(validated);
            document.touch();
            return;
        }
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

    private void restoreSnapshot(String raw) {
        SectionTree restored = DocumentMarkupCodec.decode(raw);
        document.getFileService().save(raw);
        document.getSectionTree().replaceWith(restored);
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

    private static List<StyledText> styledText(String raw) {
        // each list item stores visible text and the formatting around it
        List<StyledText> runs = new ArrayList<>();

        // active holds the formatting currently applied while reading tags
        Map<TagType, String> active = new EnumMap<>(TagType.class);

        // frames remembers the older value when formatting tags are nested
        Deque<StyleFrame> frames = new ArrayDeque<>();
        for (Token token : MarkupParser.parse(raw)) {
            if (token.kind() == Token.Kind.OPEN) {
                // an opening tag turns this style on for the following text
                frames.push(new StyleFrame(token.tag(), active.get(token.tag())));
                active.put(token.tag(), token.argument() == null ? "" : token.argument());
            } else if (token.kind() == Token.Kind.CLOSE) {
                // a closing tag restores the style from before it opened
                StyleFrame frame = frames.pop();
                if (frame.previous() == null) {
                    active.remove(frame.type());
                } else {
                    active.put(frame.type(), frame.previous());
                }
            } else if (!token.visible().isEmpty()) {
                // copyof keeps later tag changes from changing this text run
                runs.add(new StyledText(token.visible(), Map.copyOf(active)));
            }
        }
        return runs;
    }

    private static int styledLength(List<StyledText> runs) {
        int length = 0;
        for (StyledText run : runs) {
            length += run.text().length();
        }
        return length;
    }

    private static List<StyledText> sliceStyled(
            List<StyledText> runs, int start, int end) {
        List<StyledText> result = new ArrayList<>();
        int position = 0;

        // only copy the part of each run that overlaps the requested range
        for (StyledText run : runs) {
            int runEnd = position + run.text().length();
            int localStart = Math.max(0, start - position);
            int localEnd = Math.min(run.text().length(), end - position);
            if (localStart < localEnd) {
                result.add(new StyledText(
                        run.text().substring(localStart, localEnd), run.style()));
            }
            position = runEnd;
        }
        return result;
    }

    private static List<StyledText> removeStyled(
            List<StyledText> runs, int start, int end) {
        // keep everything before and after the removed range
        List<StyledText> result = sliceStyled(runs, 0, start);
        result.addAll(sliceStyled(runs, end, styledLength(runs)));
        return result;
    }

    private static List<StyledText> insertStyled(
            List<StyledText> runs, int position, List<StyledText> inserted) {
        // place the inserted runs between the two original sides
        List<StyledText> result = sliceStyled(runs, 0, position);
        result.addAll(inserted);
        result.addAll(sliceStyled(runs, position, styledLength(runs)));
        return result;
    }

    private static String encodeStyled(List<StyledText> runs) {
        // this turns the simple run list back into elias's stored tags
        StringBuilder raw = new StringBuilder();
        int index = 0;
        while (index < runs.size()) {
            StyledText run = runs.get(index);
            Map<TagType, String> style = new EnumMap<>(TagType.class);
            style.putAll(run.style());

            // neighboring pieces with the same style can share one set of tags
            StringBuilder visible = new StringBuilder(run.text());
            index++;
            while (index < runs.size() && runs.get(index).style().equals(run.style())) {
                visible.append(runs.get(index).text());
                index++;
            }

            for (Map.Entry<TagType, String> entry : style.entrySet()) {
                raw.append(entry.getKey().openText(entry.getValue()));
            }
            raw.append(MarkupParser.escape(visible.toString()));
            TagType[] types = style.keySet().toArray(new TagType[0]);
            for (int i = types.length - 1; i >= 0; i--) {
                raw.append(types[i].closeText());
            }
        }
        return raw.toString();
    }

    private static void checkRange(int start, int end, int length) {
        if (start < 0 || end > length || start >= end) {
            throw new IllegalArgumentException(
                    "Range [" + start + "," + end + ") is outside 0.." + length);
        }
    }

    private static IllegalArgumentException invalidPosition(int position, int length) {
        return new IllegalArgumentException(
                "Destination position " + position + " is outside 0.." + length);
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

    private static String replaceVisible(String raw, int start, int end, String text) {
        List<StyledText> runs = styledText(raw);
        checkRange(start, end, styledLength(runs));

        // the new text keeps the style at the start of the selected range
        List<StyledText> selectedStart = sliceStyled(runs, start, start + 1);
        List<StyledText> replacement = text.isEmpty()
                ? List.of()
                : List.of(new StyledText(text, selectedStart.get(0).style()));

        // remove and insert happen in memory before anything is saved
        List<StyledText> withoutRange = removeStyled(runs, start, end);
        return encodeStyled(insertStyled(withoutRange, start, replacement));
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
