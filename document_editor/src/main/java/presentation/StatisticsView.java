package presentation;

import text_editor.DocumentStatistics;

/** Formats calculated document statistics for console display. */
public final class StatisticsView {
    /** Creates a statistics view. */
    public StatisticsView() {}

    /**
     * Formats document statistics as labeled lines.
     *
     * @param statistics statistics to format
     * @return formatted word, character, and paragraph counts
     */
    public String render(DocumentStatistics statistics) {
        return "Words: " + statistics.wordCount()
                + "\nCharacters: " + statistics.characterCount()
                + "\nParagraphs: " + statistics.paragraphCount();
    }
}