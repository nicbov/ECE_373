package text_editor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import presentation.DocumentReadView;

/** adapts elias section tree to the shared read only document view */
public final class SectionTreeReadView implements DocumentReadView {

    private record StyleFrame(TagType type, String previous) { }

    private final SectionTree sectionTree;

    public SectionTreeReadView(SectionTree sectionTree) {
        if (sectionTree == null) {
            throw new IllegalArgumentException("Section tree is required.");
        }
        this.sectionTree = sectionTree;
    }

    @Override
    public Iterable<Block> blocksInOrder() {
        List<Block> blocks = new ArrayList<>();

        for (Section section : sectionTree.getMainSectionArray()) {
            addSectionBlocks(section, blocks);
        }

        return List.copyOf(blocks);
    }

    private void addSectionBlocks(Section section, List<Block> blocks) {
        blocks.add(new Block(
                section.getSectionID(),
                Kind.TITLE,
                textRuns(section.getName())));

        for (Paragraph paragraph : section.getParagraphs()) {
            blocks.add(new Block(
                    paragraph.getParagraphID(),
                    Kind.PARAGRAPH,
                    textRuns(paragraph.getText())));
        }

        for (Section child : section.getSubSections()) {
            addSectionBlocks(child, blocks);
        }
    }

    private List<TextRun> textRuns(String rawText) {
        List<TextRun> runs = new ArrayList<>();
        Map<TagType, String> active = new EnumMap<>(TagType.class);
        Deque<StyleFrame> frames = new ArrayDeque<>();

        // elias parser removes tags and resolves escaped characters
        for (Token token : MarkupParser.parse(rawText)) {
            if (token.kind() == Token.Kind.OPEN && token.tag().isFormatting()) {
                frames.push(new StyleFrame(token.tag(), active.get(token.tag())));
                active.put(token.tag(), token.argument() == null ? "" : token.argument());
            } else if (token.kind() == Token.Kind.CLOSE && token.tag().isFormatting()) {
                StyleFrame frame = frames.pop();
                if (frame.previous() == null) {
                    active.remove(frame.type());
                } else {
                    active.put(frame.type(), frame.previous());
                }
            } else if (token.kind() == Token.Kind.TEXT && !token.visible().isEmpty()) {
                runs.add(new TextRun(
                        token.visible(),
                        active.containsKey(TagType.BOLD),
                        active.containsKey(TagType.ITALIC),
                        active.get(TagType.FONT),
                        fontSize(active),
                        active.get(TagType.COLOR)));
            }
        }

        return List.copyOf(runs);
    }

    private Integer fontSize(Map<TagType, String> active) {
        String size = active.get(TagType.SIZE);
        return size == null ? null : Integer.valueOf(size);
    }
}
