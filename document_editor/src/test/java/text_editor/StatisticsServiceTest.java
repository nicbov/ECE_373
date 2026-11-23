package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import presentation.DocumentReadView;

class StatisticsServiceTest {

    private final StatisticsService statistics = new StatisticsService();

    @Test
    void emptyDocumentHasZeroCounts() {
        DocumentReadView document = List::<DocumentReadView.Block>of;

        assertEquals(new DocumentStatistics(0, 0, 0), statistics.calculate(document));
    }

    @Test
    void countsVisibleTitlesAndParagraphsInTraversalOrder() {
        DocumentReadView document = () -> List.of(
                block(DocumentReadView.Kind.TITLE, "First Section"),
                block(DocumentReadView.Kind.PARAGRAPH, "Hello world."),
                block(DocumentReadView.Kind.TITLE, "Nested"),
                block(DocumentReadView.Kind.PARAGRAPH, "More text here"));

        assertEquals(new DocumentStatistics(8, 45, 2), statistics.calculate(document));
    }

    @Test
    void countsWordsSeparatedByDifferentWhitespace() {
        String text = "one  two\tthree\nfour";
        DocumentReadView document = () -> List.of(
                block(DocumentReadView.Kind.PARAGRAPH, text));

        DocumentStatistics result = statistics.calculate(document);

        assertEquals(4, result.wordCount());
        assertEquals(text.length(), result.characterCount());
        assertEquals(1, result.paragraphCount());
    }

    @Test
    void emptyParagraphsStillCountAsParagraphs() {
        DocumentReadView document = () -> List.of(
                block(DocumentReadView.Kind.PARAGRAPH, ""),
                block(DocumentReadView.Kind.PARAGRAPH, ""));

        assertEquals(new DocumentStatistics(0, 0, 2), statistics.calculate(document));
    }

    @Test
    void calculatesAgainFromTheCurrentDocumentState() {
        List<DocumentReadView.Block> blocks = new ArrayList<>();
        blocks.add(block(DocumentReadView.Kind.PARAGRAPH, "first version"));
        DocumentReadView document = () -> blocks;

        assertEquals(new DocumentStatistics(2, 13, 1), statistics.calculate(document));

        // this acts like the document changed after an edit or restore
        blocks.set(0, block(DocumentReadView.Kind.PARAGRAPH, "new current version"));

        assertEquals(new DocumentStatistics(3, 19, 1), statistics.calculate(document));
    }

    @Test
    void formattingRunsDoNotSplitOneVisibleWord() {
        DocumentReadView.Block paragraph = new DocumentReadView.Block(
                DocumentReadView.Kind.PARAGRAPH,
                List.of(
                        new DocumentReadView.TextRun("for"),
                        new DocumentReadView.TextRun(
                                "matted",
                                true,
                                false,
                                null,
                                null,
                                null)));
        DocumentReadView document = () -> List.of(paragraph);

        assertEquals(new DocumentStatistics(1, 9, 1), statistics.calculate(document));
    }

    @Test
    void handlesTheRequiredDocumentSize() {
        String text = "a".repeat(100_000);
        DocumentReadView document = () -> List.of(
                block(DocumentReadView.Kind.PARAGRAPH, text));

        assertEquals(
                new DocumentStatistics(1, 100_000, 1),
                statistics.calculate(document));
    }

    @Test
    void missingDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> statistics.calculate(null));
    }

    private DocumentReadView.Block block(DocumentReadView.Kind kind, String text) {
        return new DocumentReadView.Block(
                kind,
                List.of(new DocumentReadView.TextRun(text)));
    }
}
