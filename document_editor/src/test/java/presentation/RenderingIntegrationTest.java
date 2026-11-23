package presentation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import text_editor.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises rendering through the same adapter used by the application. */
class RenderingIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void reflectsNestedFormattedEditsUndoRedoAndReopen() {
        Document document = Document.create("render-test", directory);
        var edits = new TextEditService(document);
        edits.addSection("Main");
        String nested = edits.addSubSection("1", "Nested");
        String paragraph = edits.addParagraph(nested, "hello world");
        DocumentReadView view = new SectionTreeReadView(document.getSectionTree());
        var renderer = new RenderService();
        var settings = new RenderSettings(new PlainTextView(), new GreedyLineBreaker(), 80);

        String original = "Main\n\nNested\n\nhello world";
        assertEquals(original, renderer.render(view, settings));
        edits.addFormat(paragraph, 0, 5, TagType.BOLD, null);

        // Rendering must preserve the content and style information exposed by the adapter.
        List<DocumentReadView.Block> before = new ArrayList<>();
        view.blocksInOrder().forEach(before::add);
        assertEquals("Main\n\nNested\n\n<B*hello*B> world", renderer.render(view,
                new RenderSettings(new MarkedUpView(), new BalancedLineBreaker(), 80)));
        List<DocumentReadView.Block> after = new ArrayList<>();
        view.blocksInOrder().forEach(after::add);
        assertEquals(before, after);

        // Reuse the same view to verify it reads current state after edits.
        edits.addText(paragraph, 11, " again");
        assertEquals(original + " again", renderer.render(view, settings));
        assertTrue(edits.undo());
        assertEquals(original, renderer.render(view, settings));
        assertTrue(edits.redo());
        assertEquals(original + " again", renderer.render(view, settings));

        Document reopened = Document.open("render-test", directory);
        assertEquals(original + " again", renderer.render(
                new SectionTreeReadView(reopened.getSectionTree()), settings));
    }
}

