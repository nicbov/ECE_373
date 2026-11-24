package presentation;

import java.util.Objects;

/**
 * Carries the selected view, wrapping strategy, and output width.
 *
 * @param view view used to produce printable words
 * @param lineBreaker strategy used to wrap words
 * @param width maximum output line width
 */
public record RenderSettings(RenderingView view, LineBreaker lineBreaker, int width) {
    /**
     * Creates validated rendering settings.
     *
     * @param view view used to produce printable words
     * @param lineBreaker strategy used to wrap words
     * @param width maximum output line width
     * @throws NullPointerException if the view or line breaker is null
     * @throws IllegalArgumentException if the width is not positive
     */
    public RenderSettings {
        Objects.requireNonNull(view);
        Objects.requireNonNull(lineBreaker);
        if (width < 1) {
            throw new IllegalArgumentException("Output width must be positive.");
        }
    }
}