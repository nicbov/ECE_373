package presentation;

import java.util.List;

/** Wraps printable words to a maximum width. */
public interface LineBreaker {
    List<String> wrap(List<String> words, int width);

    static void validate(List<String> words, int width) {
        if (width < 1) {
            throw new IllegalArgumentException("Output width must be positive.");
        }
        for (String word : words) {
            if (word.length() > width) {
                throw new IllegalArgumentException(
                        "A word including formatting markers exceeds the output width. "
                                + "Choose a width of at least " + word.length() + ".");
            }
        }
    }
}
