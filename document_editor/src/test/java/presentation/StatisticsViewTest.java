package presentation;

import org.junit.jupiter.api.Test;
import text_editor.Paragraph;
import text_editor.Section;
import text_editor.SectionTree;
import text_editor.SectionTreeReadView;
import text_editor.StatisticsService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsViewTest {

    @Test
    void displaysStatisticsFromFormattedDocument() {
        // Create a title and a paragraph with bold formatting.
        SectionTree tree = new SectionTree();
        Section section = new Section("Title");
        section.appendParagraph(new Paragraph("<B*hello*B> world"));
        tree.addSection(section);

        // Calculate counts using the real document adapter and service.
        var statistics = new StatisticsService().calculate(
                new SectionTreeReadView(tree)
        );

        // Count visible text, including spaces and the title.
        assertEquals(
                "Words: 3\nCharacters: 16\nParagraphs: 1",
                new StatisticsView().render(statistics)
        );
    }

    @Test
    void displaysZeroCountsForEmptyDocument() {
        var statistics = new StatisticsService().calculate(
                new SectionTreeReadView(new SectionTree())
        );

        assertEquals(
                "Words: 0\nCharacters: 0\nParagraphs: 0",
                new StatisticsView().render(statistics)
        );
    }
}