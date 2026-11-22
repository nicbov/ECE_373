package text_editor;

import java.util.ArrayList;
import java.util.List;

/** turns a section tree into ordered visible document blocks */
public final class SectionTreeTraversal implements DocumentTraversal {

    private final SectionTree sectionTree;

    public SectionTreeTraversal(SectionTree sectionTree) {
        if (sectionTree == null) {
            throw new IllegalArgumentException("Section tree is required.");
        }
        this.sectionTree = sectionTree;
    }

    @Override
    public Iterable<Block> blocksInOrder() {
        List<Block> blocks = new ArrayList<>();

        // each top level section includes all of its nested content
        for (Section section : sectionTree.getMainSectionArray()) {
            addSectionBlocks(section, blocks);
        }

        return List.copyOf(blocks);
    }

    private void addSectionBlocks(Section section, List<Block> blocks) {
        // the section name is the visible title
        blocks.add(new Block(BlockType.TITLE, section.getName()));

        // paragraphs appear before this sections nested sections
        for (Paragraph paragraph : section.getParagraphs()) {
            blocks.add(new Block(BlockType.PARAGRAPH, paragraph.getText()));
        }

        // recursion repeats the same order for every nesting level
        for (Section child : section.getSubSections()) {
            addSectionBlocks(child, blocks);
        }
    }
}
