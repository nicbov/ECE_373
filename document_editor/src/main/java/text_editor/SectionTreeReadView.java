package text_editor;

import java.util.ArrayList;
import java.util.List;
import presentation.DocumentReadView;

/** adapts elias section tree to the shared read only document view */
public final class SectionTreeReadView implements DocumentReadView {

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
        // formatting can add more runs later without changing this interface
        blocks.add(new Block(
                section.getSectionID(),
                Kind.TITLE,
                List.of(new TextRun(section.getName()))));

        for (Paragraph paragraph : section.getParagraphs()) {
            blocks.add(new Block(
                    paragraph.getParagraphID(),
                    Kind.PARAGRAPH,
                    List.of(new TextRun(paragraph.getText()))));
        }

        for (Section child : section.getSubSections()) {
            addSectionBlocks(child, blocks);
        }
    }
}
