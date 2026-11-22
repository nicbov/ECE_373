package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {

    private final StatisticsService statistics = new StatisticsService();

    @Test
    void emptyDocumentHasZeroCounts() {
        DocumentTraversal document = List::<DocumentTraversal.Block>of;

        assertEquals(new DocumentStatistics(0, 0, 0), statistics.calculate(document));
    }

    @Test
    void countsVisibleTitlesAndParagraphsInTraversalOrder() {
        DocumentTraversal document = () -> List.of(
                new DocumentTraversal.Block(DocumentTraversal.BlockType.TITLE, "First Section"),
                new DocumentTraversal.Block(DocumentTraversal.BlockType.PARAGRAPH, "Hello world."),
                new DocumentTraversal.Block(DocumentTraversal.BlockType.TITLE, "Nested"),
                new DocumentTraversal.Block(DocumentTraversal.BlockType.PARAGRAPH, "More text here"));

        assertEquals(new DocumentStatistics(8, 45, 2), statistics.calculate(document));
    }

    @Test
    void countsWordsSeparatedByDifferentWhitespace() {
        String text = "one  two\tthree\nfour";
        DocumentTraversal document = () -> List.of(
                new DocumentTraversal.Block(DocumentTraversal.BlockType.PARAGRAPH, text));

        DocumentStatistics result = statistics.calculate(document);

        assertEquals(4, result.wordCount());
        assertEquals(text.length(), result.characterCount());
        assertEquals(1, result.paragraphCount());
    }

    @Test
    void emptyParagraphsStillCountAsParagraphs() {
        DocumentTraversal document = () -> List.of(
                new DocumentTraversal.Block(DocumentTraversal.BlockType.PARAGRAPH, ""),
                new DocumentTraversal.Block(DocumentTraversal.BlockType.PARAGRAPH, ""));

        assertEquals(new DocumentStatistics(0, 0, 2), statistics.calculate(document));
    }

    @Test
    void calculatesAgainFromTheCurrentDocumentState() {
        List<DocumentTraversal.Block> blocks = new ArrayList<>();
        blocks.add(new DocumentTraversal.Block(
                DocumentTraversal.BlockType.PARAGRAPH,
                "first version"));
        DocumentTraversal document = () -> blocks;

        assertEquals(new DocumentStatistics(2, 13, 1), statistics.calculate(document));

        // this acts like the document changed after an edit or restore
        blocks.set(0, new DocumentTraversal.Block(
                DocumentTraversal.BlockType.PARAGRAPH,
                "new current version"));

        assertEquals(new DocumentStatistics(3, 19, 1), statistics.calculate(document));
    }

    @Test
    void handlesTheRequiredDocumentSize() {
        String text = "a".repeat(100_000);
        DocumentTraversal document = () -> List.of(
                new DocumentTraversal.Block(DocumentTraversal.BlockType.PARAGRAPH, text));

        assertEquals(
                new DocumentStatistics(1, 100_000, 1),
                statistics.calculate(document));
    }

    @Test
    void missingDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> statistics.calculate(null));
    }
}
