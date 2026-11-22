package presentation;

import java.util.List;

/** exposes ordered document blocks without allowing changes */
@FunctionalInterface
public interface DocumentReadView {

    Iterable<Block> blocksInOrder();

    enum Kind {
        TITLE,
        PARAGRAPH
    }

    record Block(String id, Kind kind, List<TextRun> runs) {
        public Block {
            if (id == null || kind == null || runs == null) {
                throw new IllegalArgumentException("Block values are required.");
            }
            runs = List.copyOf(runs);
        }

        // keeps amoghs original constructor working
        public Block(Kind kind, List<TextRun> runs) {
            this("", kind, runs);
        }
    }

    record TextRun(
            String text,
            boolean bold,
            boolean italic,
            String font,
            Integer size,
            String color) {
        public TextRun {
            if (text == null) {
                throw new IllegalArgumentException("Text is required.");
            }
        }

        // makes a plain text run with no formatting
        public TextRun(String text) {
            this(text, false, false, null, null, null);
        }
    }
}
