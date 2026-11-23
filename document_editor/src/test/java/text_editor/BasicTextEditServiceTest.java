package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BasicTextEditServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void insertsAtTheBeginningMiddleAndEnd() {
        Document document = createDocument("insert", "ac");
        TextEditService edits = new TextEditService(document);

        edits.addText("1#1", 1, "b");
        edits.addText("1#1", 0, "<");
        edits.addText("1#1", 4, ">");

        assertEquals("<abc>", visibleText(document, "1#1"));
    }

    @Test
    void insertionInsideFormattingKeepsThatFormatting() {
        Document document = createDocument("formatted-insert", "abcd");
        TextEditService edits = new TextEditService(document);
        edits.addFormat("1#1", 1, 3, TagType.BOLD, null);

        edits.addText("1#1", 2, "X");

        Paragraph paragraph = paragraph(document, "1#1");
        assertEquals("abXcd", MarkupParser.visibleText(paragraph.getText()));
        assertTrue(paragraph.getText().contains("<B*bXc*B>"));
    }

    @Test
    void deletesBeginningMiddleAndEndRanges() {
        Document document = createDocument("delete", "abcdef");
        TextEditService edits = new TextEditService(document);

        edits.removeText("1#1", 0, 1);
        edits.removeText("1#1", 1, 3);
        edits.removeText("1#1", 2, 3);

        assertEquals("be", visibleText(document, "1#1"));
    }

    @Test
    void replacementIsOneReversiblePersistentEdit() {
        Document document = createDocument("replace", "abcdef");
        TextEditService edits = new TextEditService(document);
        edits.addFormat("1#1", 1, 4, TagType.BOLD, null);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        edits.replaceText("1#1", 2, 3, "XY");

        assertEquals("abXYdef", visibleText(document, "1#1"));
        assertTrue(paragraph(document, "1#1").getText().contains("<B*bXYd*B>"));
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        assertTrue(edits.undo());
        assertEquals("abcdef", visibleText(document, "1#1"));
        assertTrue(paragraph(document, "1#1").getText().contains("<B*bcd*B>"));

        assertTrue(edits.redo());
        assertEquals("abXYdef", visibleText(document, "1#1"));

        Document reopened = Document.open("replace", temporaryDirectory);
        assertEquals("abXYdef", visibleText(reopened, "1#1"));
        assertTrue(paragraph(reopened, "1#1").getText().contains("<B*bXYd*B>"));
    }

    @Test
    void invalidReplacementLeavesDocumentAndHistoryUnchanged() {
        Document document = createDocument("invalid-replace", "safe");
        TextEditService edits = new TextEditService(document);
        String fileBefore = document.getFileService().load();
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertThrows(
                IllegalArgumentException.class,
                () -> edits.replaceText("1#1", 0, 10, "unsafe"));

        assertEquals(fileBefore, document.getFileService().load());
        assertEquals("safe", visibleText(document, "1#1"));
        assertEquals(versionBefore, document.getHistoryService().getCurrentVersion());
    }

    private Document createDocument(String name, String text) {
        Document document = Document.create(name, temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Section");
        edits.addParagraph("1", text);
        return document;
    }

    private Paragraph paragraph(Document document, String paragraphId) {
        return document.getSectionTree().findParagraph(paragraphId).orElseThrow();
    }

    private String visibleText(Document document, String paragraphId) {
        return MarkupParser.visibleText(paragraph(document, paragraphId).getText());
    }
}
