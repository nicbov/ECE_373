package presentation;

import java.util.ArrayList;
import java.util.List;

/** Packs as many complete words as possible onto each line. */
public final class GreedyLineBreaker implements LineBreaker {
    /** Creates a greedy line breaker. */
    public GreedyLineBreaker() {}

    /**
     * Wraps words by filling each line in order.
     *
     * @param words printable words to wrap
     * @param width maximum line width
     * @return wrapped lines
     * @throws IllegalArgumentException if the width is invalid or a word is too long
     */
    @Override
    public List<String> wrap(List<String> words, int width) {
        LineBreaker.validate(words, width);
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            // An existing line needs one extra column for the space before the word.
            if (!line.isEmpty() && word.length() >= width - line.length()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
