package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MoveTextServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void movesFormattedTextBetweenParagraphsAsOnePersistentEdit() {
        Document document = createDocument("move-between", "abcdef", "123");
        TextEditService edits = new TextEditService(document);
        edits.addFormat("1#1", 2, 4, TagType.BOLD, null);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        edits.moveText("1#1", 2, 4, "1#2", 1);

        assertEquals("abef", visibleText(document, "1#1"));
        assertEquals("1cd23", visibleText(document, "1#2"));
        assertTrue(paragraph(document, "1#2").getText().contains("<B*cd*B>"));
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        assertTrue(edits.undo());
        assertEquals("abcdef", visibleText(document, "1#1"));
        assertEquals("123", visibleText(document, "1#2"));

        assertTrue(edits.redo());
        assertEquals("abef", visibleText(document, "1#1"));
        assertEquals("1cd23", visibleText(document, "1#2"));

        Document reopened = Document.open("move-between", temporaryDirectory);
        assertEquals("abef", visibleText(reopened, "1#1"));
        assertEquals("1cd23", visibleText(reopened, "1#2"));
        assertTrue(paragraph(reopened, "1#2").getText().contains("<B*cd*B>"));
    }

    @Test
    void sameParagraphMoveUsesPositionsFromBeforeTheEdit() {
        Document document = createDocument("move-same", "abcdef");
        TextEditService edits = new TextEditService(document);

        // position six means the end of the original paragraph
        edits.moveText("1#1", 1, 3, "1#1", 6);

        assertEquals("adefbc", visibleText(document, "1#1"));
        assertTrue(edits.undo());
        assertEquals("abcdef", visibleText(document, "1#1"));
    }

    @Test
    void invalidDestinationLeavesFileTreeAndHistoryUnchanged() {
        Document document = createDocument("invalid-move", "safe", "here");
        TextEditService edits = new TextEditService(document);
        String fileBefore = document.getFileService().load();
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertThrows(
                IllegalArgumentException.class,
                () -> edits.moveText("1#1", 0, 2, "1#2", 20));

        assertEquals(fileBefore, document.getFileService().load());
        assertEquals("safe", visibleText(document, "1#1"));
        assertEquals("here", visibleText(document, "1#2"));
        assertEquals(versionBefore, document.getHistoryService().getCurrentVersion());
    }

    @Test
    void destinationInsideMovedRangeIsRejectedWithoutChanges() {
        Document document = createDocument("inside-range", "abcdef");
        TextEditService edits = new TextEditService(document);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertThrows(
                IllegalArgumentException.class,
                () -> edits.moveText("1#1", 1, 4, "1#1", 2));

        assertEquals("abcdef", visibleText(document, "1#1"));
        assertEquals(versionBefore, document.getHistoryService().getCurrentVersion());
    }

    private Document createDocument(String name, String... paragraphs) {
        Document document = Document.create(name, temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Section");
        for (String paragraph : paragraphs) {
            edits.addParagraph("1", paragraph);
        }
        return document;
    }

    private Paragraph paragraph(Document document, String paragraphId) {
        return document.getSectionTree().findParagraph(paragraphId).orElseThrow();
    }

    private String visibleText(Document document, String paragraphId) {
        return MarkupParser.visibleText(paragraph(document, paragraphId).getText());
    }
}
