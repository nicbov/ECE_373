package presentation;

import java.util.List;

/** Converts a document block into printable words. */
public interface RenderingView {
    /** Each item is one complete printable word, including any markers. */
    List<String> words(DocumentReadView.Block block);
}