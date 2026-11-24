package presentation;

import java.util.List;

/** Wraps printable words to a maximum width. */
public interface LineBreaker {
    /**
     * Wraps printable words into lines.
     *
     * @param words printable words to wrap
     * @param width maximum line width
     * @return wrapped lines
     * @throws IllegalArgumentException if the width is invalid or a word is too long
     */
    List<String> wrap(List<String> words, int width);

    /**
     * Validates line-wrapping input.
     *
     * @param words printable words to validate
     * @param width maximum line width
     * @throws IllegalArgumentException if the width is invalid or a word is too long
     */
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
