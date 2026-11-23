package presentation;

import java.util.ArrayList;
import java.util.List;

/** Packs as many complete words as possible onto each line. */
public final class GreedyLineBreaker implements LineBreaker {
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
