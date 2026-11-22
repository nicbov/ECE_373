package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
