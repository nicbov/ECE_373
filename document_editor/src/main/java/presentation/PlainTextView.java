package presentation;

import java.util.List;

/** Emits words without formatting markers */
public final class PlainTextView implements RenderingView {
    @Override
    public List<String> words(DocumentReadView.Block block) {
        return WordTokens.words(block, (run, text) -> text);
    }
}