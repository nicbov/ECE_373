package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearchReplaceAllServiceTest {

    @TempDir
    Path temporaryDirectory;

    private final SearchReplaceService replacements = new SearchReplaceService();

    @Test
    void replacesEveryMatchAsOnePersistentEdit() {
        Document document = Document.create("replace-all", temporaryDirectory);
        TextEditService edits = new TextEditService(document);
        edits.addSection("cat title");
        String firstId = edits.addParagraph("1", "cat cat");
        String nestedId = edits.addSubSection("1", "nested cat");
        String nestedParagraphId = edits.addParagraph(nestedId, "cat");
        edits.addFormat(firstId, 0, 3, TagType.BOLD, null);
        int versionBefore = document.getHistoryService().getCurrentVersion();

        int changed = replacements.replaceAllWholeWord(document, "cat", "kitten");

        assertEquals(5, changed);
        assertEquals("kitten title", document.getSectionTree().find("1").orElseThrow().getName());
        assertEquals("kitten kitten", visibleText(document, firstId));
        assertEquals("nested kitten", document.getSectionTree().find(nestedId).orElseThrow().getName());
        assertEquals("kitten", visibleText(document, nestedParagraphId));
        assertTrue(paragraph(document, firstId).getText().contains("<B*kitten*B>"));
        assertEquals(versionBefore + 1, document.getHistoryService().getCurrentVersion());

        // one undo restores every title and paragraph together
        assertTrue(edits.undo());
        assertEquals("cat title", document.getSectionTree().find("1").orElseThrow().getName());
        assertEquals("cat cat", visibleText(document, firstId));
        assertEquals("nested cat", document.getSectionTree().find(nestedId).orElseThrow().getName());
        assertEquals("cat", visibleText(document, nestedParagraphId));

        assertTrue(edits.redo());
        Document reopened = Document.open("replace-all", temporaryDirectory);
        assertEquals("kitten kitten", visibleText(reopened, firstId));
        assertEquals("kitten", visibleText(reopened, nestedParagraphId));
    }

    @Test
    void substringModeReplacesMatchesInsideLargerWords() {
        Document document = createDocument("replace-substrings", "scatter cat");

        int changed = replacements.replaceAllSubstring(document, "cat", "DOG");

        assertEquals(2, changed);
        assertEquals("sDOGter DOG", visibleText(document, "1#1"));
    }

    @Test
    void noMatchesLeaveTheDocumentAndHistoryUnchanged() {
        Document document = createDocument("replace-none", "safe text");
        String fileBefore = document.getFileService().load();
        int versionBefore = document.getHistoryService().getCurrentVersion();

        int changed = replacements.replaceAllWholeWord(document, "missing", "new");

        assertEquals(0, changed);
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

    private Paragraph paragraph(Document document, String paragraphId) {
        return document.getSectionTree().findParagraph(paragraphId).orElseThrow();
    }

    private String visibleText(Document document, String paragraphId) {
        return MarkupParser.visibleText(paragraph(document, paragraphId).getText());
    }
}
