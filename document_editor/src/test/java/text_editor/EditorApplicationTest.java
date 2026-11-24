package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
