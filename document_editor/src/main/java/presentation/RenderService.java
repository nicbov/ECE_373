package presentation;

import java.util.ArrayList;
import java.util.List;

/** Applies the selected view and wrapping strategy to each document block. */
public final class RenderService {
    public String render(DocumentReadView document, RenderSettings settings) {
        List<String> blocks = new ArrayList<>();

        // Wrap each title or paragraph independently to preserve block boundaries.
        for (var block : document.blocksInOrder()) {
            var words = settings.view().words(block);
            blocks.add(String.join("\n", settings.lineBreaker().wrap(words, settings.width())));
        }
        return String.join("\n\n", blocks);
    }
}