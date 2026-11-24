package presentation;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import presentation.DocumentReadView.*;

/** Checks both view implementations and the shared word tokenizer. */
class RenderingViewTest {
    @Test
    void plainViewJoinsWordsAcrossStyleBoundaries() {
        var block = new Block(Kind.PARAGRAPH, List.of(
                new TextRun("  hel"),
                new TextRun("lo", true, false, null, null, null),
                new TextRun("\tworld\n")));
        assertEquals(List.of("hello", "world"), new PlainTextView().words(block));
    }

    @Test
    void markupNestsEverySupportedStyle() {
        var block = new Block(Kind.TITLE, List.of(
                new TextRun("Hello", true, true, "Arial", 12, "blue")));
        assertEquals(List.of("<B*<I*<F:Arial*<S:12*<C:blue*Hello*C>*S>*F>*I>*B>"),
                new MarkedUpView().words(block));
    }

    @Test
    void markupKeepsAWordTogetherAcrossRuns() {
        var block = new Block(Kind.PARAGRAPH, List.of(
                new TextRun("hel", true, false, null, null, null),
                new TextRun("lo world")));
        assertEquals(List.of("<B*hel*B>lo", "world"),
                new MarkedUpView().words(block));
    }

    @Test
    void bothViewsHandleEmptyAndWhitespaceOnlyText() {
        for (RenderingView view : List.of(new PlainTextView(), new MarkedUpView())) {
            assertEquals(List.of(), view.words(new Block(Kind.PARAGRAPH, List.of())));
            assertEquals(List.of(), view.words(new Block(Kind.PARAGRAPH,
                    List.of(new TextRun(" \t\n")))));
        }
    }
}

