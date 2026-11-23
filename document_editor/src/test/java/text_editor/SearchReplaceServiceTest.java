package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearchReplaceServiceTest {

    @TempDir
    Path temporaryDirectory;

    private final SearchReplaceService replacements = new SearchReplaceService();

    @Test
    void replacesOnlyTheFirstNestedFormattedWholeWord() {
        Document document = Document.create("whole-word", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Main");
        String nestedId = edits.addSubSection("1", "Nested");
        String paragraphId = edits.addParagraph(nestedId, "test test");
        edits.addFormat(paragraphId, 0, 4, TagType.BOLD, null);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertTrue(replacements.replaceFirstWholeWord(
                document,
                "test",
                "<done>*"));

        assertEquals("<done>* test", visibleText(document, paragraphId));
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        assertTrue(edits.undo());
        assertEquals("test test", visibleText(document, paragraphId));

        assertTrue(edits.redo());
        assertEquals("<done>* test", visibleText(document, paragraphId));

        Document reopened = Document.open("whole-word", temporaryDirectory);
        assertEquals("<done>* test", visibleText(reopened, paragraphId));
    }

    @Test
    void replacesATitleWhenItIsTheFirstDocumentMatch() {
        Document document = Document.create("title", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Main title");
        String paragraphId = edits.addParagraph("1", "Main paragraph");

        assertTrue(replacements.replaceFirstWholeWord(document, "Main", "Changed"));

        assertEquals("Changed title", document.getSectionTree()
                .find("1").orElseThrow().getName());
        assertEquals("Main paragraph", visibleText(document, paragraphId));

        assertTrue(edits.undo());
        assertEquals("Main title", document.getSectionTree()
                .find("1").orElseThrow().getName());
    }

    @Test
    void substringModeCanReplaceInsideALargerWord() {
        Document document = createDocument("substring", "scatter cat");

        assertTrue(replacements.replaceFirstSubstring(document, "cat", "DOG"));

        assertEquals("sDOGter cat", visibleText(document, "1#1"));
    }

    @Test
    void noMatchLeavesDocumentAndHistoryUnchanged() {
        Document document = createDocument("no-match", "safe text");
        String fileBefore = document.getFileService().load();
        int versionBefore = document.getHistoryService().getCurrentVersion();

        assertFalse(replacements.replaceFirstWholeWord(document, "missing", "new"));

        assertEquals(fileBefore, document.getFileService().load());
        assertEquals(versionBefore, document.getHistoryService().getCurrentVersion());
    }

    private Document createDocument(String name, String text) {
        Document document = Document.create(name, temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("Section");
        edits.addParagraph("1", text);
        return document;
    }

    private String visibleText(Document document, String paragraphId) {
        Paragraph paragraph = document.getSectionTree()
                .findParagraph(paragraphId).orElseThrow();
        return MarkupParser.visibleText(paragraph.getText());
    }
}
