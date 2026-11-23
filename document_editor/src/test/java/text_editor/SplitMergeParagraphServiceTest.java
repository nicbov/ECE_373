package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SplitMergeParagraphServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void splitsFormattedParagraphAsOnePersistentEdit() {
        Document document = createDocument("split", "abcdef");
        TextEditService edits = new TextEditService(document);
        edits.addFormat("1#1", 1, 5, TagType.BOLD, null);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        String newParagraphId = edits.splitParagraph("1#1", 3);

        assertEquals("1#2", newParagraphId);
        assertEquals("abc", visibleText(document, "1#1"));
        assertEquals("def", visibleText(document, "1#2"));
        assertTrue(paragraph(document, "1#1").getText().contains("<B*bc*B>"));
        assertTrue(paragraph(document, "1#2").getText().contains("<B*de*B>"));
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        assertTrue(edits.undo());
        assertEquals("abcdef", visibleText(document, "1#1"));
        assertTrue(edits.redo());
        assertEquals("abc", visibleText(document, "1#1"));
        assertEquals("def", visibleText(document, "1#2"));

        Document reopened = Document.open("split", temporaryDirectory);
        assertEquals("abc", visibleText(reopened, "1#1"));
        assertEquals("def", visibleText(reopened, "1#2"));
    }

    @Test
    void mergesNeighboringParagraphsAndPreservesFormatting() {
        Document document = createDocument("merge", "first ", "second", "third");
        TextEditService edits = new TextEditService(document);
        edits.addFormat("1#2", 0, 6, TagType.ITALIC, null);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        edits.mergeParagraphs("1#1", "1#2");

        assertEquals("first second", visibleText(document, "1#1"));
        assertTrue(paragraph(document, "1#1").getText().contains("<I*second*I>"));
        assertEquals("third", visibleText(document, "1#2"));
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        assertTrue(edits.undo());
        assertEquals("first ", visibleText(document, "1#1"));
        assertEquals("second", visibleText(document, "1#2"));
        assertEquals("third", visibleText(document, "1#3"));

        assertTrue(edits.redo());
        Document reopened = Document.open("merge", temporaryDirectory);
        assertEquals("first second", visibleText(reopened, "1#1"));
        assertEquals("third", visibleText(reopened, "1#2"));
    }

    @Test
    void rejectsNonNeighboringParagraphsWithoutChanges() {
        Document document = createDocument("invalid-merge", "one", "two", "three");
        TextEditService edits = new TextEditService(document);
        String fileBefore = document.getFileService().load();
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertThrows(
                IllegalArgumentException.class,
                () -> edits.mergeParagraphs("1#1", "1#3"));

        assertEquals(fileBefore, document.getFileService().load());
        assertEquals("one", visibleText(document, "1#1"));
        assertEquals("two", visibleText(document, "1#2"));
        assertEquals("three", visibleText(document, "1#3"));
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
