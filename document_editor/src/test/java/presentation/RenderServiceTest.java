package presentation;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import presentation.DocumentReadView.*;

class RenderServiceTest {
    private final RenderService renderer = new RenderService();

    @Test
    void preservesBlockOrderAndUsesSelectedStrategy() {
        DocumentReadView document = () -> List.of(
                new Block(Kind.TITLE, List.of(new TextRun("Title"))),
                new Block(Kind.PARAGRAPH, List.of(new TextRun("aa bbbb c"))));
        assertEquals("Title\n\naa bbbb\nc", renderer.render(document,
                new RenderSettings(new PlainTextView(), new GreedyLineBreaker(), 8)));
        assertEquals("Title\n\naa\nbbbb c", renderer.render(document,
                new RenderSettings(new PlainTextView(), new BalancedLineBreaker(), 8)));
    }

    @Test
    void rendersMarkupWithBothStrategiesAndCountsMarkersTowardWidth() {
        DocumentReadView document = () -> List.of(new Block(Kind.PARAGRAPH,
                List.of(new TextRun("hello", true, false, null, null, null))));
        for (LineBreaker breaker : List.of(new GreedyLineBreaker(), new BalancedLineBreaker())) {
            assertEquals("<B*hello*B>", renderer.render(document,
                    new RenderSettings(new MarkedUpView(), breaker, 11)));
            assertThrows(IllegalArgumentException.class, () -> renderer.render(document,
                    new RenderSettings(new MarkedUpView(), breaker, 5)));
            assertEquals("hello", renderer.render(document,
                    new RenderSettings(new PlainTextView(), breaker, 5)));
        }
    }

    @Test
    void emptyDocumentRendersAsEmptyString() {
        assertEquals("", renderer.render(List::of,
                new RenderSettings(new PlainTextView(), new GreedyLineBreaker(), 80)));
    }
}

