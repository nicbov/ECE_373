package text_editor;

/**
 * Read-only view of document text in its stored order.
 *
 * The document layer can implement this without exposing its mutable section,
 * paragraph, or formatting representation.
 */
@FunctionalInterface
public interface DocumentTraversal {

    Iterable<Block> blocksInOrder();

    enum BlockType {
        TITLE,
        PARAGRAPH
    }

    record Block(BlockType type, String visibleText) {
        public Block {
            if (type == null || visibleText == null) {
                throw new IllegalArgumentException("Block type and visible text are required.");
            }
        }
    }
}
