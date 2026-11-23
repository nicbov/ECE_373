package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParagraphEditingFoundationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void insertsAndRemovesAtOneBasedPositions() {
        Document document = createDocument("positions");
        TextEditService edits = new TextEditService(document);
        edits.addParagraph("1", "first");
        edits.addParagraph("1", "third");

        String insertedId = edits.insertParagraph("1", 2, "second");

        assertEquals("1#2", insertedId);
        assertParagraphs(document, "first", "second", "third");
        assertEquals("1#3", paragraph(document, 3).getParagraphID());
        assertEquals(0, paragraph(document, 1).getStartLocation());
        assertEquals(6, paragraph(document, 2).getStartLocation());
        assertEquals(13, paragraph(document, 3).getStartLocation());

        edits.removeParagraph("1#2");

        assertParagraphs(document, "first", "third");
        assertEquals("1#2", paragraph(document, 2).getParagraphID());
        assertEquals(6, paragraph(document, 2).getStartLocation());
        assertTrue(edits.undo());
        assertParagraphs(document, "first", "second", "third");
    }

    @Test
    void multiParagraphChangeUsesOneUndoRedoAndSurvivesReopen() {
        Document document = createDocument("multi");
        TextEditService edits = new TextEditService(document);
        edits.addParagraph("1", "first");
        edits.addParagraph("1", "last");
        int versionBefore = document.getHistoryService().getCurrentVersion();

        // both changes happen on the copy before it is saved
        edits.editDocument(tree -> {
            tree.insertParagraph(new Paragraph("second"), "1", 2);
            tree.insertParagraph(new Paragraph("third"), "1", 3);
        });

        assertParagraphs(document, "first", "second", "third", "last");
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        assertTrue(edits.undo());
        assertParagraphs(document, "first", "last");

        assertTrue(edits.redo());
        assertParagraphs(document, "first", "second", "third", "last");

        Document reopened = Document.open("multi", temporaryDirectory);
        assertParagraphs(reopened, "first", "second", "third", "last");
    }

    @Test
    void failureMidwayLeavesFileTreeAndHistoryUnchanged() {
        Document document = createDocument("failure");
        TextEditService edits = new TextEditService(document);
        edits.addParagraph("1", "safe");
        String fileBefore = document.getFileService().load();
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertThrows(IllegalStateException.class, () -> edits.editDocument(tree -> {
            tree.insertParagraph(new Paragraph("temporary"), "1", 2);
            throw new IllegalStateException("second part failed");
        }));

        assertEquals(fileBefore, document.getFileService().load());
        assertParagraphs(document, "safe");
        assertEquals(versionBefore, document.getHistoryService().getCurrentVersion());
    }

    private Document createDocument(String name) {
        Document document = Document.create(name, temporaryDirectory);
        new TextEditService(document).addSection("Section");
        return document;
    }

    private Paragraph paragraph(Document document, int position) {
        return document.getSectionTree().findParagraph("1#" + position).orElseThrow();
    }

    private void assertParagraphs(Document document, String... expected) {
        assertEquals(expected.length, document.getSectionTree().find("1").orElseThrow()
                .countParagraphs());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], MarkupParser.visibleText(paragraph(document, i + 1).getText()));
        }
    }
}
