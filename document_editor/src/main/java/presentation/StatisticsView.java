package presentation;

import text_editor.DocumentStatistics;

/** Formats calculated document statistics for console display. */
public final class StatisticsView {

    public String render(DocumentStatistics statistics) {
        return "Words: " + statistics.wordCount()
                + "\nCharacters: " + statistics.characterCount()
                + "\nParagraphs: " + statistics.paragraphCount();
    }
}