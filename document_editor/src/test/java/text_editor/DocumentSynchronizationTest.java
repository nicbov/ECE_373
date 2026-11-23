package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentSynchronizationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void emptyDocumentsAndParagraphsSurviveReload() {
        Document document = Document.create("empty-case", temporaryDirectory);
        assertSynchronized(document);

        Document reopenedEmpty = Document.open("empty-case", temporaryDirectory);
        assertEquals(0, reopenedEmpty.getSectionTree().countSections());
        assertFalse(reopenedEmpty.getHistoryService().canUndo());
        assertSynchronized(reopenedEmpty);

        TextEditService edits = new TextEditService(reopenedEmpty);
        String sectionId = edits.addSection("");
        String paragraphId = edits.addParagraph(sectionId, "");
        assertEquals("", MarkupParser.visibleText(
                reopenedEmpty.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
        assertSynchronized(reopenedEmpty);

        Document reopenedWithEmptyParagraph = Document.open("empty-case", temporaryDirectory);
        assertEquals("", reopenedWithEmptyParagraph.getSectionTree()
                .findParagraph(paragraphId).orElseThrow().getText());
        assertSynchronized(reopenedWithEmptyParagraph);
    }

    @Test
    void escapedUnicodeAndMultilineTextRoundTripAndEditAtBothBoundaries() {
        Document document = Document.create("escaped-case", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        String sectionName = "Résumé * <chapter>";
        String initialText = "<tag> * stars\nsecond line\t🙂";
        edits.addSection(sectionName);
        String paragraphId = edits.addParagraph("1", initialText);
        assertSynchronized(document);

        edits.addText(paragraphId, 0, "<start>*");
        int end = MarkupParser.visibleText(document.getSectionTree()
                .findParagraph(paragraphId).orElseThrow().getText()).length();
        edits.addText(paragraphId, end, "<end>*");
        assertSynchronized(document);

        Document reopened = Document.open("escaped-case", temporaryDirectory);
        assertEquals(sectionName, reopened.getSectionTree().find("1").orElseThrow().getName());
        assertEquals("<start>*" + initialText + "<end>*", MarkupParser.visibleText(
                reopened.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
        assertSynchronized(reopened);

        TextEditService reopenedEdits = new TextEditService(reopened);
        assertTrue(reopenedEdits.undo());
        assertEquals("<start>*" + initialText, MarkupParser.visibleText(
                reopened.getSectionTree().findParagraph(paragraphId).orElseThrow().getText()));
        assertSynchronized(reopened);
        assertTrue(reopenedEdits.redo());
        assertSynchronized(reopened);
    }

    @Test
    void removingSectionsRelabelsNestedIdsAndUndoRedoRestoresThemAfterReload() {
        Document document = Document.create("relabel-case", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Remove me");
        edits.addSection("Keep me");
        String subsectionId = edits.addSubSection("2", "Nested");
        String paragraphId = edits.addParagraph(subsectionId, "nested text");
        assertEquals("2/1", subsectionId);
        assertEquals("2/1#1", paragraphId);

        edits.removeSection("1");
        assertEquals("Keep me", document.getSectionTree().find("1").orElseThrow().getName());
        assertEquals("nested text", MarkupParser.visibleText(
                document.getSectionTree().findParagraph("1/1#1").orElseThrow().getText()));
        assertSynchronized(document);

        Document reopened = Document.open("relabel-case", temporaryDirectory);
        assertEquals("1/1#1", reopened.getSectionTree()
                .findParagraph("1/1#1").orElseThrow().getParagraphID());
        TextEditService reopenedEdits = new TextEditService(reopened);
        assertTrue(reopenedEdits.undo());
        assertEquals("Remove me", reopened.getSectionTree().find("1").orElseThrow().getName());
        assertEquals("Keep me", reopened.getSectionTree().find("2").orElseThrow().getName());
        assertEquals("2/1#1", reopened.getSectionTree()
                .findParagraph("2/1#1").orElseThrow().getParagraphID());
        assertSynchronized(reopened);

        assertTrue(reopenedEdits.redo());
        assertFalse(reopened.getSectionTree().hasSection("2"));
        assertTrue(reopened.getSectionTree().hasSection("1/1"));
        assertSynchronized(reopened);
    }

    @Test
    void failedEditsLeaveTheFileTreeAndHistoryUnchanged() {
        Document document = Document.create("failure-case", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Section");
        String paragraphId = edits.addParagraph("1", "safe text");
        String rawBefore = document.getFileService().load();
        String treeBefore = DocumentMarkupCodec.encode(document.getSectionTree());
        int historyBefore = document.getHistoryService().getCurrentVersion();

        assertThrows(IllegalArgumentException.class,
                () -> edits.removeText(paragraphId, 0, 100));
        assertThrows(IllegalArgumentException.class,
                () -> edits.addSubSection("missing", "Invalid parent"));

        assertEquals(rawBefore, document.getFileService().load());
        assertEquals(treeBefore, DocumentMarkupCodec.encode(document.getSectionTree()));
        assertEquals(historyBefore, document.getHistoryService().getCurrentVersion());
        assertSynchronized(document);
    }

    @Test
    void openingMalformedStructureFailsWithoutSilentlyDroppingDocumentData() {
        Document document = Document.create("malformed-case", temporaryDirectory);
        String malformed = "<SEC:Chapter*text without paragraph*SEC>";
        document.getFileService().save(malformed);

        assertThrows(IllegalArgumentException.class,
                () -> Document.open("malformed-case", temporaryDirectory));
        assertEquals(malformed, document.getFileService().load());
    }

    private void assertSynchronized(Document document) {
        assertEquals(DocumentMarkupCodec.encode(document.getSectionTree()),
                document.getFileService().load());
    }
}
