package presentation;

import java.util.List;

/** Converts a document block into printable words. */
public interface RenderingView {
    /**
     * Converts a document block into complete printable words.
     *
     * @param block block to convert
     * @return printable words, including any formatting markers
     */
    List<String> words(DocumentReadView.Block block);
}