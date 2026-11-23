package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextEditServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void editsTextAndFormattingAndKeepsTreeAndHistorySynchronized() {
        Document document = Document.create("notes", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        SectionTree tree = document.getSectionTree();

        String sectionId = edits.addSection("Chapter *one");
        String paragraphId = edits.addParagraph(sectionId, "hello <world>*");

        assertEquals("1", sectionId);
        assertEquals("1#1", paragraphId);
        assertEquals("hello <world>*",
                MarkupParser.visibleText(document.getSectionTree().findParagraph(paragraphId)
                        .orElseThrow().getText()));

        edits.addText(paragraphId, 5, "!");
        edits.addFormat(paragraphId, 0, 5, TagType.BOLD, null);

        Paragraph paragraph = document.getSectionTree().findParagraph(paragraphId).orElseThrow();
        assertEquals("hello! <world>*", MarkupParser.visibleText(paragraph.getText()));
        assertTrue(paragraph.getText().contains("<B*hello*B>"));
        assertEquals(4, document.getHistoryService().getCurrentVersion());
        assertSame(tree, document.getSectionTree());

        assertTrue(edits.undo());
        assertFalse(document.getSectionTree().findParagraph(paragraphId).orElseThrow()
                .getText().contains("<B*"));
        assertTrue(edits.redo());
        assertTrue(document.getSectionTree().findParagraph(paragraphId).orElseThrow()
                .getText().contains("<B*hello*B>"));

        Document reopened = Document.open("notes", temporaryDirectory);
        Paragraph reopenedParagraph = reopened.getSectionTree().findParagraph(paragraphId).orElseThrow();
        assertEquals("hello! <world>*", MarkupParser.visibleText(reopenedParagraph.getText()));
        assertTrue(reopenedParagraph.getText().contains("<B*hello*B>"));
        assertEquals("Chapter *one", reopened.getSectionTree().find("1").orElseThrow().getName());
        assertEquals(document.getHistoryService().getCurrentVersion(),
                reopened.getHistoryService().getCurrentVersion());
        TextEditService reopenedEdits = new TextEditService(reopened);
        assertTrue(reopenedEdits.undo());
        assertFalse(reopened.getSectionTree().findParagraph(paragraphId).orElseThrow()
                .getText().contains("<B*"));
        assertTrue(reopenedEdits.redo());
        assertTrue(reopened.getSectionTree().findParagraph(paragraphId).orElseThrow()
                .getText().contains("<B*hello*B>"));
    }

    @Test
    void supportsNestedStructureAndReversibleStructuralEdits() {
        Document document = Document.create("structure", temporaryDirectory);
        TextEditService edits = new TextEditService(document);

        edits.addSection("First");
        String subsectionId = edits.addSubSection("1", "Nested");
        String paragraphId = edits.addParagraph(subsectionId, "nested text");
        assertEquals("1/1", subsectionId);
        assertEquals("1/1#1", paragraphId);

        edits.removeParagraph(paragraphId);
        assertTrue(document.getSectionTree().findParagraph(paragraphId).isEmpty());
        assertTrue(edits.undo());
        assertEquals("nested text", MarkupParser.visibleText(
                document.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));

        edits.removeSection(subsectionId);
        assertFalse(document.getSectionTree().hasSection(subsectionId));
        assertTrue(edits.undo());
        assertTrue(document.getSectionTree().hasSection(subsectionId));

        Document reopened = Document.open("structure", temporaryDirectory);
        assertEquals("nested text", MarkupParser.visibleText(
                reopened.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
    }

    @Test
    void removalAcrossFormattingRunsPreservesValidMarkupAndCanBeUndone() {
        Document document = Document.create("formatting", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Section");
        String paragraphId = edits.addParagraph("1", "abcd");
        edits.addFormat(paragraphId, 1, 3, TagType.ITALIC, null);

        edits.removeText(paragraphId, 0, 4);

        Paragraph paragraph = document.getSectionTree().findParagraph(paragraphId).orElseThrow();
        assertEquals("", MarkupParser.visibleText(paragraph.getText()));
        assertTrue(edits.undo());
        assertEquals("abcd", MarkupParser.visibleText(
                document.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
        assertTrue(edits.redo());
        assertEquals("", MarkupParser.visibleText(
                document.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
    }

    @Test
    void invalidEditsDoNotChangeTheDocumentOrRecordHistory() {
        Document document = Document.create("invalid", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Section");
        String paragraphId = edits.addParagraph("1", "text");
        String before = document.getFileService().load();
        int historySize = document.getHistoryService().getCurrentVersion();

        assertThrows(IllegalArgumentException.class,
                () -> edits.addFormat(paragraphId, 0, 5, TagType.BOLD, null));

        assertEquals(before, document.getFileService().load());
        assertEquals(historySize, document.getHistoryService().getCurrentVersion());
        assertEquals("text", MarkupParser.visibleText(
                document.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
    }
}
