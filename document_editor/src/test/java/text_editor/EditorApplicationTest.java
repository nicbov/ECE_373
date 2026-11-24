package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import CommandPalette.CommandPalette;
import CommandPalette.MacroDictionary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import presentation.GreedyLineBreaker;
import presentation.PlainTextView;
import presentation.RenderSettings;

class EditorApplicationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void connectsDocumentStatisticsSearchReplacementAndRendering() {
        EditorApplication application = createApplication("connected");
        TextEditService edits = application.edits();
        edits.addSection("Main");
        edits.addParagraph("1", "hello world");

        assertEquals(
                "Words: 3\nCharacters: 15\nParagraphs: 1",
                application.showStatistics());
        assertEquals("Main\n\nhello world", application.renderDocument());
        assertEquals(1, application.findWholeWord("world").size());

        assertTrue(application.replaceFirstWholeWord("world", "team"));
        assertEquals("Main\n\nhello team", application.renderDocument());
    }

    @Test
    void statisticsAndSearchReadTheRestoredCheckpointState() {
        EditorApplication application = createApplication("checkpoint-connected");
        TextEditService edits = application.edits();
        edits.addSection("Main");
        String paragraphId = edits.addParagraph("1", "before");
        application.checkpoints().createAtCurrentVersion("saved");

        edits.replaceText(paragraphId, 0, 6, "after words");
        assertTrue(application.findWholeWord("after").size() == 1);

        application.checkpoints().restore("saved");

        assertEquals(
                "Words: 2\nCharacters: 10\nParagraphs: 1",
                application.showStatistics());
        assertEquals(1, application.findWholeWord("before").size());
        assertTrue(application.findWholeWord("after").isEmpty());
    }

    @Test
    void parsedCommandsReachTheDocumentServices() {
        EditorApplication application = new EditorApplication();
        application.setDocumentDirectory(temporaryDirectory);

        application.executeLine("create command-connected");
        application.executeLine("add-section Main");
        application.executeLine("add-paragraph 1, hello");
        application.executeLine("insert 1#1, 5, -team");
        application.executeLine("checkpoint-create saved");
        application.executeLine("replace 1#1, 0, 5, goodbye");

        assertEquals("goodbye-team", visibleText(application, "1#1"));

        application.executeLine("checkpoint-restore saved");

        assertEquals("hello-team", visibleText(application, "1#1"));
        assertEquals(
                "Words: 2\nCharacters: 14\nParagraphs: 1",
                application.showStatistics());
    }

    @Test
    void scriptAndDirectCommandsProduceTheSameDocument() throws IOException {
        EditorApplication application = new EditorApplication();
        application.setDocumentDirectory(temporaryDirectory);

        application.executeLine("create direct");
        application.executeLine("add-section Main");
        application.executeLine("add-paragraph 1, hello");
        application.executeLine("insert 1#1, 5, -team");
        String direct = DocumentMarkupCodec.encode(
                application.currentDocument().getSectionTree());

        Path script = temporaryDirectory.resolve("commands.txt");
        Files.writeString(script, String.join("\n",
                "create scripted",
                "add-section Main",
                "add-paragraph 1, hello",
                "insert 1#1, 5, -team"));
        application.executeScript(script);
        String scripted = DocumentMarkupCodec.encode(
                application.currentDocument().getSectionTree());

        assertEquals(direct, scripted);
    }

    @Test
    void recordedMacroReplaysAsOneUndoableEdit() {
        MacroDictionary.ClearAll();
        EditorApplication application = createApplication("macro");
        application.executeLine("add-section Main");

        application.executeLine("macro-start greeting");
        application.executeLine("add-paragraph 1, hello");
        application.executeLine("insert 1#1, 5, -team");
        application.executeLine("macro-stop");
        application.executeLine("undo");
        application.executeLine("undo");
        int versionBefore = application.currentDocument()
                .getHistoryService()
                .getCurrentVersion();

        application.executeLine("greeting");

        assertEquals("hello-team", visibleText(application, "1#1"));
        assertEquals(
                versionBefore + 1,
                application.currentDocument().getHistoryService().getCurrentVersion());

        application.executeLine("undo");
        assertTrue(application.currentDocument()
                .getSectionTree()
                .findParagraph("1#1")
                .isEmpty());
    }

    @Test
    void failedMacroRestoresTheWholeDocument() {
        MacroDictionary.ClearAll();
        EditorApplication application = createApplication("macro-rollback");
        application.executeLine("add-section Main");
        String before = DocumentMarkupCodec.encode(
                application.currentDocument().getSectionTree());
        int versionBefore = application.currentDocument()
                .getHistoryService()
                .getCurrentVersion();
        CommandPalette.LoadMacros(String.join("\n",
                "~begin",
                "broken",
                "add-paragraph 1, hello",
                "insert 9#1, 0, failure"));

        application.executeLine("broken");

        assertEquals(
                before,
                DocumentMarkupCodec.encode(application.currentDocument().getSectionTree()));
        assertEquals(
                versionBefore,
                application.currentDocument().getHistoryService().getCurrentVersion());
    }

    private EditorApplication createApplication(String name) {
        EditorApplication application = new EditorApplication();
        application.setDocumentDirectory(temporaryDirectory);
        application.createDocument(name);
        application.setRenderSettings(new RenderSettings(
                new PlainTextView(),
                new GreedyLineBreaker(),
                80));
        return application;
    }

    private String visibleText(EditorApplication application, String paragraphId) {
        Paragraph paragraph = application.currentDocument()
                .getSectionTree()
                .findParagraph(paragraphId)
                .orElseThrow();
        return MarkupParser.visibleText(paragraph.getText());
    }
}
