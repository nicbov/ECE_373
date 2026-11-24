package presentation;

import java.util.List;

/** Emits words without formatting markers. */
public final class PlainTextView implements RenderingView {
    /** Creates a plain-text rendering view. */
    public PlainTextView() {}

    /**
     * Extracts plain printable words from a block.
     *
     * @param block block to convert
     * @return words without formatting markers
     */
    @Override
    public List<String> words(DocumentReadView.Block block) {
        return WordTokens.words(block, (run, text) -> text);
    }
}