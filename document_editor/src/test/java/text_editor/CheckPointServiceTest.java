package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Document used by every test, after each queued edit (version):
 *   0: empty
 *   1: section "A"
 *   2: paragraph 1#1 = "hello"
 *   3: paragraph 1#1 = "hello world"
 *   4: paragraph 1#1 = "hello world!"
 */
class CheckPointServiceTest {

    private static final String PARAGRAPH = "1#1";

    @TempDir
    Path folder;

    private Document document;
    private TextEditService editor;
    private CheckPointService checkPoints;

    @BeforeEach
    void setUp() {
        document = Document.create("doc", folder);
        editor = new TextEditService(document);
        checkPoints = new CheckPointService(document);

        editor.addSection("A");
        editor.addParagraph("1", "hello");
        editor.addText(PARAGRAPH, 5, " world");
        editor.addText(PARAGRAPH, 11, "!");
    }

    // ---- creating ---------------------------------------------------------------

    @Test
    void createsCheckpointAtCurrentVersion() {
        StackHistoryService.CheckPoint created = checkPoints.createAtCurrentVersion("  tip  ");

        assertEquals("tip", created.name());
        assertEquals(4, created.version());
        assertEquals(created, checkPoints.get("tip").orElseThrow());
    }

    @Test
    void createsCheckpointAtSelectedQueuedEdit() {
        StackHistoryService.CheckPoint created = checkPoints.createAtEdit("first words", 2);

        assertEquals(2, created.version());
        assertEquals("hello", textAfterRestoring("first words"));
    }

    @Test
    void canSelectAnUndoneEditThatIsStillQueued() {
        editor.undo();
        editor.undo();

        StackHistoryService.CheckPoint created = checkPoints.createAtEdit("tip", 4);

        assertEquals(4, created.version());
    }

    @Test
    void rejectsBadEditNumbersNamesAndEmptyHistory() {
        assertThrows(IllegalArgumentException.class, () -> checkPoints.createAtEdit("x", 0));
        assertThrows(IllegalArgumentException.class, () -> checkPoints.createAtEdit("x", 5));
        assertThrows(IllegalArgumentException.class, () -> checkPoints.createAtCurrentVersion(" "));
        assertThrows(IllegalArgumentException.class, () -> checkPoints.createAtCurrentVersion(null));

        checkPoints.createAtCurrentVersion("tip");
        assertThrows(IllegalStateException.class, () -> checkPoints.createAtEdit("tip", 1));

        Document empty = Document.create("empty", folder);
        assertThrows(IllegalStateException.class,
                () -> new CheckPointService(empty).createAtCurrentVersion("none"));
    }

    // ---- rename / delete / list ------------------------------------------------

    @Test
    void renamesCheckpointAndKeepsItsVersion() {
        checkPoints.createAtEdit("draft", 3);

        StackHistoryService.CheckPoint renamed = checkPoints.rename("draft", "final draft");

        assertEquals(new StackHistoryService.CheckPoint("final draft", 3), renamed);
        assertTrue(checkPoints.get("draft").isEmpty());
        assertEquals(3, checkPoints.get("final draft").orElseThrow().version());
    }

    @Test
    void renameRejectsMissingAndDuplicateNames() {
        checkPoints.createAtEdit("one", 1);
        checkPoints.createAtEdit("two", 2);

        assertThrows(IllegalArgumentException.class, () -> checkPoints.rename("missing", "x"));
        assertThrows(IllegalStateException.class, () -> checkPoints.rename("one", "two"));
        assertEquals("one", checkPoints.rename("one", "one").name());
    }

    @Test
    void deletesOnlyTheCheckpoint() {
        checkPoints.createAtEdit("one", 1);
        String before = textNow();

        assertTrue(checkPoints.delete("one"));
        assertFalse(checkPoints.delete("one"));

        assertTrue(checkPoints.list().isEmpty());
        assertEquals(before, textNow());
        assertEquals(4, document.getHistoryService().getCurrentVersion());
    }

    @Test
    void listsCheckpointsOrderedByVersion() {
        checkPoints.createAtEdit("late", 4);
        checkPoints.createAtEdit("early", 1);
        checkPoints.createAtEdit("middle", 3);

        List<String> names = checkPoints.list().stream()
                .map(StackHistoryService.CheckPoint::name).toList();

        assertEquals(List.of("early", "middle", "late"), names);
    }

    @Test
    void checkpointsSurviveReopeningTheDocument() {
        checkPoints.createAtEdit("keeps\ttabs", 2);

        Document reopened = Document.open("doc", folder);
        CheckPointService reloaded = new CheckPointService(reopened);

        assertEquals(List.of(new StackHistoryService.CheckPoint("keeps\ttabs", 2)), reloaded.list());
        reloaded.restore("keeps\ttabs");
        assertEquals("hello", paragraphText(reopened));
    }

    // ---- restore ---------------------------------------------------------------

    @Test
    void restoresBackToAnEarlierCheckpoint() {
        checkPoints.createAtEdit("first words", 2);

        checkPoints.restore("first words");

        assertEquals("hello", textNow());
        assertEquals(2, document.getHistoryService().getCurrentVersion());
        assertEquals("hello", paragraphText(Document.open("doc", folder)));
    }

    @Test
    void restoresForwardAgainAndKeepsEveryCheckpoint() {
        checkPoints.createAtEdit("first words", 2);
        checkPoints.createAtEdit("tip", 4);

        checkPoints.restore("first words");
        assertEquals(2, checkPoints.list().size());

        checkPoints.restore("tip");
        assertEquals("hello world!", textNow());
    }

    @Test
    void restoreKeepsLaterEditsRedoable() {
        checkPoints.createAtEdit("first words", 2);

        checkPoints.restore("first words");

        assertTrue(document.getHistoryService().canRedo());
        assertTrue(editor.redo());
        assertEquals("hello world", textNow());
    }

    @Test
    void restoreToUnknownCheckpointFailsWithoutChangingAnything() {
        String before = textNow();

        assertThrows(IllegalArgumentException.class, () -> checkPoints.restore("nope"));

        assertEquals(before, textNow());
    }

    @Test
    void newEditAfterRestoreDropsCheckpointsOnTheAbandonedBranch() {
        checkPoints.createAtEdit("first words", 2);
        checkPoints.createAtEdit("tip", 4);
        checkPoints.restore("first words");

        editor.addText(PARAGRAPH, 5, "?");

        List<String> names = checkPoints.list().stream()
                .map(StackHistoryService.CheckPoint::name).toList();
        assertEquals(List.of("first words"), names);
    }

    // ---- present version ----------------------------------------------------------

    @Test
    void reportsWhichCheckpointsAreOnThePresentVersion() {
        checkPoints.createAtEdit("first words", 2);
        checkPoints.createAtEdit("tip", 4);
        checkPoints.createAtEdit("also tip", 4);

        assertEquals(List.of("tip", "also tip"), names(checkPoints.getPresentCheckPoints()));
        assertTrue(checkPoints.isPresent("tip"));
        assertFalse(checkPoints.isPresent("first words"));

        checkPoints.restore("first words");

        assertEquals(List.of("first words"), names(checkPoints.getPresentCheckPoints()));
    }

    @Test
    void setAsPresentRestoresAndDiscardsLaterEdits() {
        checkPoints.createAtEdit("first words", 2);
        checkPoints.createAtEdit("tip", 4);

        checkPoints.setAsPresent("first words");

        assertEquals("hello", textNow());
        assertFalse(document.getHistoryService().canRedo());
        assertFalse(editor.redo());
        assertEquals(List.of("first words"), names(checkPoints.list()));
        assertTrue(checkPoints.isPresent("first words"));
    }

    // ---- preview ----------------------------------------------------------------

    @Test
    void previewsAnEarlierVersionWithoutChangingTheDocument() throws IOException {
        checkPoints.createAtEdit("first words", 2);
        Snapshot before = snapshot();

        try (CheckPointService.Preview preview = checkPoints.preview("first words")) {
            assertEquals("hello", paragraphText(preview.getSectionTree()));
            assertEquals("first words", preview.getCheckPoint().name());
            assertTrue(Files.isDirectory(preview.getTemporaryDirectory()));
            assertFalse(preview.getTemporaryDirectory().startsWith(folder));
        }

        assertEquals(before, snapshot());
        assertEquals("hello world!", textNow());
    }

    @Test
    void previewsALaterVersionThatIsOnlyInTheRedoQueue() throws IOException {
        checkPoints.createAtEdit("tip", 4);
        editor.undo();
        editor.undo();
        editor.undo();
        Snapshot before = snapshot();

        try (CheckPointService.Preview preview = checkPoints.preview("tip")) {
            assertEquals("hello world!", paragraphText(preview.getSectionTree()));
        }

        assertEquals(before, snapshot());
        assertEquals(1, document.getHistoryService().getCurrentVersion());
        assertTrue(document.getSectionTree().findParagraph(PARAGRAPH).isEmpty());
    }

    @Test
    void previewOfTheCurrentVersionMatchesTheDocument() {
        checkPoints.createAtEdit("tip", 4);

        try (CheckPointService.Preview preview = checkPoints.preview("tip")) {
            assertEquals(DocumentMarkupCodec.encode(document.getSectionTree()), preview.getRawText());
        }
    }

    @Test
    void previewExposesAReadViewOfTheVersion() {
        checkPoints.createAtEdit("first words", 2);

        try (CheckPointService.Preview preview = checkPoints.preview("first words")) {
            StringBuilder text = new StringBuilder();
            for (var block : preview.readView().blocksInOrder()) {
                for (var run : block.runs()) {
                    text.append(run.text());
                }
            }
            assertEquals("Ahello", text.toString());
        }
    }

    @Test
    void closingAPreviewDeletesItsTemporaryFilesAndIsRepeatable() {
        checkPoints.createAtEdit("first words", 2);
        CheckPointService.Preview preview = checkPoints.preview("first words");
        Path temp = preview.getTemporaryDirectory();
        assertTrue(Files.exists(temp));

        preview.close();
        preview.close();

        assertFalse(Files.exists(temp));
    }

    @Test
    void previewOfUnknownCheckpointFails() {
        assertThrows(IllegalArgumentException.class, () -> checkPoints.preview("nope"));
    }

    // ---- helpers ----------------------------------------------------------------

    private record Snapshot(String documentFile, String historyFile) { }

    private Snapshot snapshot() throws IOException {
        return new Snapshot(
                Files.readString(Path.of(document.getPathDocument())),
                Files.readString(Path.of(document.getPathHistory())));
    }

    private String textAfterRestoring(String checkPointName) {
        checkPoints.restore(checkPointName);
        return textNow();
    }

    private String textNow() {
        return paragraphText(document);
    }

    private static String paragraphText(Document doc) {
        return paragraphText(doc.getSectionTree());
    }

    private static String paragraphText(SectionTree tree) {
        return tree.findParagraph(PARAGRAPH)
                .map(p -> MarkupParser.visibleText(p.getText()))
                .orElse("");
    }

    private static List<String> names(List<StackHistoryService.CheckPoint> list) {
        return list.stream().map(StackHistoryService.CheckPoint::name).toList();
    }
}