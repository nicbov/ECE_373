package presentation;

import java.util.List;

/** Provides ordered document blocks without exposing mutation operations. */
@FunctionalInterface
public interface DocumentReadView {

    /**
     * Returns the document blocks in display order.
     *
     * @return ordered document blocks
     */
    Iterable<Block> blocksInOrder();

    /** Identifies the semantic kind of a document block. */
    enum Kind {
        TITLE,
        PARAGRAPH
    }

    /**
     * Describes one document block and its styled text runs.
     *
     * @param id block identifier
     * @param kind semantic block kind
     * @param runs styled text runs in order
     */
    record Block(String id, Kind kind, List<TextRun> runs) {
        /**
         * Creates an immutable block description.
         *
         * @param id block identifier
         * @param kind semantic block kind
         * @param runs styled text runs in order
         * @throws IllegalArgumentException if any argument is null
         */
        public Block {
            if (id == null || kind == null || runs == null) {
                throw new IllegalArgumentException("Block values are required.");
            }
            runs = List.copyOf(runs);
        }

        /**
         * Creates a block with an empty identifier.
         *
         * @param kind semantic block kind
         * @param runs styled text runs in order
         * @throws IllegalArgumentException if either argument is null
         */
        public Block(Kind kind, List<TextRun> runs) {
            this("", kind, runs);
        }
    }

    /**
     * Describes a text fragment and its optional formatting.
     *
     * @param text fragment text
     * @param bold whether bold styling is enabled
     * @param italic whether italic styling is enabled
     * @param font optional font name
     * @param size optional font size
     * @param color optional color name
     */
    record TextRun(
            String text,
            boolean bold,
            boolean italic,
            String font,
            Integer size,
            String color) {
        /**
         * Creates a text run.
         *
         * @param text fragment text
         * @param bold whether bold styling is enabled
         * @param italic whether italic styling is enabled
         * @param font optional font name
         * @param size optional font size
         * @param color optional color name
         * @throws IllegalArgumentException if text is null
         */
        public TextRun {
            if (text == null) {
                throw new IllegalArgumentException("Text is required.");
            }
        }

        /**
         * Creates an unformatted text run.
         *
         * @param text fragment text
         * @throws IllegalArgumentException if text is null
         */
        public TextRun(String text) {
            this(text, false, false, null, null, null);
        }
    }
}
