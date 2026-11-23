package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadServicesIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    private final StatisticsService statistics = new StatisticsService();
    private final SearchService search = new SearchService();

    @Test
    void statisticsFollowRealEditsUndoRedoAndReopen() {
        Document document = Document.create("statistics", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Main");
        String paragraphId = edits.addParagraph("1", "one two");

        assertEquals(
                new DocumentStatistics(3, 11, 1),
                calculate(document));

        // insertion changes the current tree and the calculated result
        edits.addText(paragraphId, 7, " three");
        assertEquals(
                new DocumentStatistics(4, 17, 1),
                calculate(document));

        // deletion is also reflected without maintaining stored counters
        edits.removeText(paragraphId, 0, 4);
        assertEquals(
                new DocumentStatistics(3, 13, 1),
                calculate(document));

        assertTrue(edits.undo());
        assertEquals(
                new DocumentStatistics(4, 17, 1),
                calculate(document));

        assertTrue(edits.redo());
        assertEquals(
                new DocumentStatistics(3, 13, 1),
                calculate(document));

        // reopening proves statistics read the persisted current state
        Document reopened = Document.open("statistics", temporaryDirectory);
        assertEquals(
                new DocumentStatistics(3, 13, 1),
                calculate(reopened));
    }

    @Test
    void searchFollowsNestedFormattedEditsUndoRedoAndReopen() {
        Document document = Document.create("search", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Main");
        String nestedId = edits.addSubSection("1", "Nested");
        String paragraphId = edits.addParagraph(nestedId, "alpha beta");
        edits.addFormat(paragraphId, 6, 10, TagType.BOLD, null);

        List<SearchMatch> formattedMatches = search.findWholeWord(
                new SectionTreeReadView(document.getSectionTree()),
                "beta");
        assertEquals(1, formattedMatches.size());
        assertEquals("1/1#1", formattedMatches.get(0).blockId());

        edits.addText(paragraphId, 0, "find ");
        assertTrue(hasMatch(document, "find"));

        assertTrue(edits.undo());
        assertFalse(hasMatch(document, "find"));

        assertTrue(edits.redo());
        assertTrue(hasMatch(document, "find"));

        // search reads the same tagged text after it is loaded from disk
        Document reopened = Document.open("search", temporaryDirectory);
        assertTrue(hasMatch(reopened, "find"));
        assertTrue(hasMatch(reopened, "beta"));
    }

    private DocumentStatistics calculate(Document document) {
        return statistics.calculate(new SectionTreeReadView(document.getSectionTree()));
    }

    private boolean hasMatch(Document document, String query) {
        return !search.findWholeWord(
                new SectionTreeReadView(document.getSectionTree()),
                query).isEmpty();
    }
}
