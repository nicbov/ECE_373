package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemainingUseCasesTest {

    @TempDir
    Path directory;

    @Test
    void movesASectionSubtreeAndUndoesItOnce() {
        Document document = Document.create("move-sections", directory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("First");
        edits.addSection("Second");
        edits.addSubSection("1", "Child");

        edits.moveSection("1/1", "2", 1);

        assertTrue(document.getSectionTree().find("1/1").isEmpty());
        assertEquals("Child", document.getSectionTree().find("2/1").orElseThrow().getName());

        assertTrue(edits.undo());
        assertEquals("Child", document.getSectionTree().find("1/1").orElseThrow().getName());
    }

    @Test
    void formatsASectionTitleAndPersistsIt() {
        Document document = Document.create("title-format", directory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Main title");

        edits.addSectionTitleFormat("1", 0, 4, TagType.BOLD, null);

        Section title = document.getSectionTree().find("1").orElseThrow();
        assertEquals("Main title", MarkupParser.visibleText(title.getName()));
        assertTrue(title.getName().contains("<B*"));

        Document reopened = Document.open("title-format", directory);
        assertEquals(
                title.getName(),
                reopened.getSectionTree().find("1").orElseThrow().getName());
    }
}
