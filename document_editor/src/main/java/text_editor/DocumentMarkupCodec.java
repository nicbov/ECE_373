package text_editor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;

/** Reads and writes the tagged structure stored in a document file. */
final class DocumentMarkupCodec {

    private DocumentMarkupCodec() { }

    static String encode(SectionTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("Section tree is required.");
        }
        StringBuilder raw = new StringBuilder();
        for (Section section : tree.getMainSectionArray()) {
            appendSection(raw, section);
        }
        return raw.toString();
    }

    static SectionTree decode(String raw) {
        List<Token> tokens = MarkupParser.parse(raw);
        SectionTree tree = new SectionTree();
        Deque<Section> sections = new ArrayDeque<>();
        Paragraph paragraph = null;
        int paragraphContentStart = -1;

        for (Token token : tokens) {
            if (token.kind() == Token.Kind.TEXT) {
                if (paragraph == null) {
                    throw new IllegalArgumentException("Document text must be inside a paragraph tag.");
                }
                continue;
            }

            if (token.kind() == Token.Kind.OPEN) {
                if (token.tag() == TagType.SECTION) {
                    if (paragraph != null) {
                        throw new IllegalArgumentException("A section cannot start inside a paragraph.");
                    }
                    Section section = new Section(decodeSectionName(token.argument()));
                    if (sections.isEmpty()) {
                        tree.addSection(section);
                    } else {
                        sections.peek().appendSection(section);
                    }
                    sections.push(section);
                } else if (token.tag() == TagType.PARAGRAPH) {
                    if (sections.isEmpty() || paragraph != null) {
                        throw new IllegalArgumentException(
                                "A paragraph must be directly inside one section.");
                    }
                    paragraph = new Paragraph("");
                    sections.peek().appendParagraph(paragraph);
                    paragraphContentStart = token.rawEnd();
                } else if (paragraph == null) {
                    throw new IllegalArgumentException("Formatting tags must be inside a paragraph.");
                }
                continue;
            }

            if (token.tag() == TagType.PARAGRAPH) {
                if (paragraph == null) {
                    throw new IllegalArgumentException("Paragraph closing tag has no open paragraph.");
                }
                paragraph.setText(raw.substring(paragraphContentStart, token.rawStart()));
                paragraph = null;
                paragraphContentStart = -1;
            } else if (token.tag() == TagType.SECTION) {
                if (paragraph != null || sections.isEmpty()) {
                    throw new IllegalArgumentException("Section closing tag is out of place.");
                }
                sections.pop();
            } else if (paragraph == null) {
                throw new IllegalArgumentException("Formatting tags must be inside a paragraph.");
            }
        }

        if (paragraph != null || !sections.isEmpty()) {
            throw new IllegalArgumentException("Document structure is incomplete.");
        }
        tree.recalculateLocations();
        return tree;
    }

    static SectionTree copy(SectionTree tree) {
        return decode(encode(tree));
    }

    private static void appendSection(StringBuilder raw, Section section) {
        raw.append(TagType.SECTION.openText(encodeSectionName(section.getName())));
        for (Paragraph paragraph : section.getParagraphs()) {
            raw.append(TagType.PARAGRAPH.openText(paragraph.getParagraphID()));
            raw.append(paragraph.getText());
            raw.append(TagType.PARAGRAPH.closeText());
        }
        for (Section child : section.getSubSections()) {
            appendSection(raw, child);
        }
        raw.append(TagType.SECTION.closeText());
    }

    private static String encodeSectionName(String name) {
        return "~" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(name.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeSectionName(String argument) {
        if (argument.startsWith("~")) {
            try {
                byte[] bytes = Base64.getUrlDecoder().decode(argument.substring(1));
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid encoded section name.", e);
            }
        }
        return argument;
    }
}
