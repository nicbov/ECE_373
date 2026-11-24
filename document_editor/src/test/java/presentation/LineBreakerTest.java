package presentation;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies each strategy through the common interface. */
class LineBreakerTest {
    static Stream<LineBreaker> strategies() {
        return Stream.of(new GreedyLineBreaker(), new BalancedLineBreaker());
    }

    @Test
    void strategiesProduceDifferentLayouts() {
        var words = List.of("aa", "bbbb", "c");
        assertEquals(List.of("aa bbbb", "c"), new GreedyLineBreaker().wrap(words, 8));
        assertEquals(List.of("aa", "bbbb c"), new BalancedLineBreaker().wrap(words, 8));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void handlesExactWidthAndEmptyInput(LineBreaker breaker) {
        assertEquals(List.of("aa bbbb"), breaker.wrap(List.of("aa", "bbbb"), 7));
        assertEquals(List.of(), breaker.wrap(List.of(), 7));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void rejectsInvalidWidthsAndOversizedTokens(LineBreaker breaker) {
        assertThrows(IllegalArgumentException.class, () -> breaker.wrap(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> breaker.wrap(List.of("a"), -1));
        // Visible markers contribute to the emitted width.
        assertThrows(IllegalArgumentException.class,
                () -> breaker.wrap(List.of("<B*hello*B>"), 5));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void preservesWordsAndWidthForLargeInput(LineBreaker breaker) {
        var words = List.of("word ".repeat(20000).strip().split(" "));
        var lines = breaker.wrap(words, 80);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 80));
        assertEquals(String.join(" ", words), String.join(" ", lines));
        assertEquals(lines, breaker.wrap(words, 80));
    }
}

