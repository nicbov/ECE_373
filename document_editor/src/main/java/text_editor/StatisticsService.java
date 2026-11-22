package text_editor;

import java.util.regex.Pattern;

/** Calculates statistics on demand without maintaining editing-side counters. */
public final class StatisticsService {

    private static final Pattern WORD = Pattern.compile("\\S+");

    public DocumentStatistics calculate(DocumentTraversal document) {
        if (document == null) {
            throw new IllegalArgumentException("Document traversal is required.");
        }

        int words = 0;
        int characters = 0;
        int paragraphs = 0;

        for (DocumentTraversal.Block block : document.blocksInOrder()) {
            words += (int) WORD.matcher(block.visibleText()).results().count();
            characters += block.visibleText().length();
            if (block.type() == DocumentTraversal.BlockType.PARAGRAPH) {
                paragraphs++;
            }
        }

        return new DocumentStatistics(words, characters, paragraphs);
    }
}
