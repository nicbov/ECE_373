package text_editor;

import presentation.DocumentReadView;

/** one search result in document order */
public record SearchMatch(
        int blockIndex,
        String blockId,
        DocumentReadView.Kind blockKind,
        int startIndex,
        int endIndex,
        String text) { }
