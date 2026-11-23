package presentation;

import java.util.Objects;

/** Carries the selected view, wrapping strategy, and width. */
public record RenderSettings(RenderingView view, LineBreaker lineBreaker, int width) {
    public RenderSettings {
        Objects.requireNonNull(view);
        Objects.requireNonNull(lineBreaker);
        if (width < 1) {
            throw new IllegalArgumentException("Output width must be positive.");
        }
    }
}