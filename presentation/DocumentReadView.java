package presentation;

import java.util.List;

/** Supplies the current document's blocks in stored order, at all nesting levels. */
@FunctionalInterface
/** Exposes ordered document block to rendering and statistics */
public interface DocumentReadView {
    Iterable<Block> blocksInOrder();

    enum Kind { TITLE, PARAGRAPH }

    record Block(Kind kind, List<TextRun> runs) {
        public Block {
            runs = List.copyOf(runs);
        }
    }

    record TextRun(String text, boolean bold, boolean italic,
                    String font, Integer size, String color) {
        public TextRun(String text) {
            this(text, false, false, null, null, null);
        }
    }
}