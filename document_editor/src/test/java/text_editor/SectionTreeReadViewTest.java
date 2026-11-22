package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import presentation.DocumentReadView;

class SectionTreeReadViewTest {

    @Test
    void adaptsRealSectionsWithStableIdsAndPlainRuns() {
        DocumentReadView document = new SectionTreeReadView(createTree());
        List<DocumentReadView.Block> blocks = new ArrayList<>();

        document.blocksInOrder().forEach(blocks::add);

        assertEquals(6, blocks.size());
        assertBlock(blocks.get(0), "1", DocumentReadView.Kind.TITLE, "Main Title");
        assertBlock(blocks.get(1), "1#1", DocumentReadView.Kind.PARAGRAPH, "hello world");
        assertBlock(blocks.get(2), "1/1", DocumentReadView.Kind.TITLE, "Nested Title");
        assertBlock(blocks.get(3), "1/1#1", DocumentReadView.Kind.PARAGRAPH, "find me");
        assertBlock(blocks.get(4), "2", DocumentReadView.Kind.TITLE, "Last");
        assertBlock(blocks.get(5), "2#1", DocumentReadView.Kind.PARAGRAPH, "find me too");
    }

    @Test
    void statisticsUseTheSharedReadView() {
        DocumentReadView document = new SectionTreeReadView(createTree());
        StatisticsService statistics = new StatisticsService();

        assertEquals(
                new DocumentStatistics(12, 55, 3),
                statistics.calculate(document));
    }

    @Test
    void searchReturnsRealNestedParagraphIds() {
        DocumentReadView document = new SectionTreeReadView(createTree());
        SearchService search = new SearchService();

        List<SearchMatch> matches = search.findWholeWord(document, "find");

        assertEquals(2, matches.size());
        assertEquals("1/1#1", matches.get(0).blockId());
        assertEquals("2#1", matches.get(1).blockId());
    }

    @Test
    void convertsEliasFormattingTagsIntoSharedTextRuns() {
        SectionTree tree = new SectionTree();
        Section section = new Section("Title");
        section.appendParagraph(new Paragraph("<B*for*B><C:blue*matted*C> text"));
        tree.addSection(section);

        DocumentReadView document = new SectionTreeReadView(tree);
        List<DocumentReadView.Block> blocks = new ArrayList<>();
        document.blocksInOrder().forEach(blocks::add);

        List<DocumentReadView.TextRun> runs = blocks.get(1).runs();
        assertEquals(3, runs.size());
        assertEquals("for", runs.get(0).text());
        assertEquals(true, runs.get(0).bold());
        assertEquals("matted", runs.get(1).text());
        assertEquals("blue", runs.get(1).color());
        assertEquals(" text", runs.get(2).text());

        SearchService search = new SearchService();
        assertEquals(1, search.findWholeWord(document, "formatted").size());

        StatisticsService statistics = new StatisticsService();
        assertEquals(
                new DocumentStatistics(3, 19, 1),
                statistics.calculate(document));
    }

    private void assertBlock(
            DocumentReadView.Block block,
            String id,
            DocumentReadView.Kind kind,
            String text) {
        assertEquals(id, block.id());
        assertEquals(kind, block.kind());
        assertEquals(1, block.runs().size());
        assertEquals(text, block.runs().get(0).text());
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
