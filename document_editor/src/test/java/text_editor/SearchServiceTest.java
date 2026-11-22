package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchServiceTest {

    private final SearchService search = new SearchService();

    @Test
    void substringSearchFindsMatchesInDocumentOrder() {
        DocumentTraversal document = document(
                block(DocumentTraversal.BlockType.TITLE, "Testing"),
                block(DocumentTraversal.BlockType.PARAGRAPH, "test testing test"),
                block(DocumentTraversal.BlockType.PARAGRAPH, "no match"));

        List<SearchMatch> matches = search.findSubstring(document, "test");

        assertEquals(3, matches.size());
        assertEquals(new SearchMatch(
                1,
                DocumentTraversal.BlockType.PARAGRAPH,
                0,
                4,
                "test"), matches.get(0));
        assertEquals(5, matches.get(1).startIndex());
        assertEquals(13, matches.get(2).startIndex());
    }

    @Test
    void wholeWordSearchSkipsTextInsideLargerWords() {
        DocumentTraversal document = document(
                block(DocumentTraversal.BlockType.PARAGRAPH, "test testing test_test test."));

        List<SearchMatch> matches = search.findWholeWord(document, "test");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).startIndex());
        assertEquals(23, matches.get(1).startIndex());
    }

    @Test
    void searchIncludesTitlesAndParagraphs() {
        DocumentTraversal document = document(
                block(DocumentTraversal.BlockType.TITLE, "hello"),
                block(DocumentTraversal.BlockType.PARAGRAPH, "hello"));

        List<SearchMatch> matches = search.findWholeWord(document, "hello");

        assertEquals(2, matches.size());
        assertEquals(DocumentTraversal.BlockType.TITLE, matches.get(0).blockType());
        assertEquals(DocumentTraversal.BlockType.PARAGRAPH, matches.get(1).blockType());
    }

    @Test
    void emptySearchTextIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> search.findSubstring(document(), ""));
    }

    private DocumentTraversal document(DocumentTraversal.Block... blocks) {
        return () -> List.of(blocks);
    }

    private DocumentTraversal.Block block(
            DocumentTraversal.BlockType type,
            String text) {
        return new DocumentTraversal.Block(type, text);
    }
}
