package text_editor;

/** one search result in document order */
public record SearchMatch(
        int blockIndex,
        DocumentTraversal.BlockType blockType,
        int startIndex,
        int endIndex,
        String text) { }
