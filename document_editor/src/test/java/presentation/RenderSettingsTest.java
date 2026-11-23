package presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderSettingsTest {
    @Test
    void retainsSelectedComponentsAndWidth() {
        var view = new MarkedUpView();
        var breaker = new BalancedLineBreaker();
        var settings = new RenderSettings(view, breaker, 40);
        assertSame(view, settings.view());
        assertSame(breaker, settings.lineBreaker());
        assertEquals(40, settings.width());
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(NullPointerException.class,
                () -> new RenderSettings(null, new GreedyLineBreaker(), 80));
        assertThrows(NullPointerException.class,
                () -> new RenderSettings(new PlainTextView(), null, 80));
        assertThrows(IllegalArgumentException.class,
                () -> new RenderSettings(new PlainTextView(), new GreedyLineBreaker(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RenderSettings(new PlainTextView(), new GreedyLineBreaker(), -1));
    }
}

