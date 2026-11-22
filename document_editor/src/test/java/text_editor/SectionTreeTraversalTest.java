package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectionTreeTraversalTest {

    @Test
    void traversesRealSectionsAndParagraphsInOrder() {
        DocumentTraversal traversal = new SectionTreeTraversal(createTree());
        List<DocumentTraversal.Block> blocks = new ArrayList<>();

        traversal.blocksInOrder().forEach(blocks::add);

        assertEquals(6, blocks.size());
        assertEquals(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.TITLE,
                "Main Title"), blocks.get(0));
        assertEquals(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.PARAGRAPH,
                "hello world"), blocks.get(1));
        assertEquals(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.TITLE,
                "Nested Title"), blocks.get(2));
        assertEquals(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.PARAGRAPH,
                "find me"), blocks.get(3));
        assertEquals(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.TITLE,
                "Last"), blocks.get(4));
        assertEquals(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.PARAGRAPH,
                "find me too"), blocks.get(5));
    }

    @Test
    void statisticsUseTheRealSectionTree() {
        DocumentTraversal traversal = new SectionTreeTraversal(createTree());
        StatisticsService statistics = new StatisticsService();

        DocumentStatistics result = statistics.calculate(traversal);

        assertEquals(new DocumentStatistics(12, 55, 3), result);
    }

    @Test
    void searchUsesNestedParagraphsFromTheRealSectionTree() {
        DocumentTraversal traversal = new SectionTreeTraversal(createTree());
        SearchService search = new SearchService();

        List<SearchMatch> matches = search.findWholeWord(traversal, "find");

        assertEquals(2, matches.size());
        assertEquals(3, matches.get(0).blockIndex());
        assertEquals(5, matches.get(1).blockIndex());
    }

    private SectionTree createTree() {
        SectionTree tree = new SectionTree();

        Section main = new Section("Main Title");
        main.appendParagraph(new Paragraph("hello world"));

        Section nested = new Section("Nested Title");
        nested.appendParagraph(new Paragraph("find me"));
        main.appendSection(nested);

        Section last = new Section("Last");
        last.appendParagraph(new Paragraph("find me too"));

        tree.addSection(main);
        tree.addSection(last);
        return tree;
    }
}
