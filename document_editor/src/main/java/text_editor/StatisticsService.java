package text_editor;

import java.util.regex.Pattern;
import presentation.DocumentReadView;

/** Calculates statistics on demand without maintaining editing-side counters. */
public final class StatisticsService {

    private static final Pattern WORD = Pattern.compile("\\S+");

    public DocumentStatistics calculate(DocumentReadView document) {
        if (document == null) {
            throw new IllegalArgumentException("Document view is required.");
        }

        int words = 0;
        int characters = 0;
        int paragraphs = 0;

        for (DocumentReadView.Block block : document.blocksInOrder()) {
            StringBuilder visibleText = new StringBuilder();

            // joining runs first keeps split formatted words as one word
            for (DocumentReadView.TextRun run : block.runs()) {
                visibleText.append(run.text());
            }

            words += (int) WORD.matcher(visibleText).results().count();
            characters += visibleText.length();
            if (block.kind() == DocumentReadView.Kind.PARAGRAPH) {
                paragraphs++;
            }
        }

        return new DocumentStatistics(words, characters, paragraphs);
    }
}
