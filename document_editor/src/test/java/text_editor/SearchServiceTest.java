package text_editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import presentation.DocumentReadView;

class SearchServiceTest {

    private final SearchService search = new SearchService();

    @Test
    void substringSearchFindsMatchesInDocumentOrder() {
        DocumentReadView document = document(
                block("1", DocumentReadView.Kind.TITLE, "Testing"),
                block("1#1", DocumentReadView.Kind.PARAGRAPH, "test testing test"),
                block("1#2", DocumentReadView.Kind.PARAGRAPH, "no match"));

        List<SearchMatch> matches = search.findSubstring(document, "test");

        assertEquals(3, matches.size());
        assertEquals(new SearchMatch(
                1,
                "1#1",
                DocumentReadView.Kind.PARAGRAPH,
                0,
                4,
                "test"), matches.get(0));
        assertEquals(5, matches.get(1).startIndex());
        assertEquals(13, matches.get(2).startIndex());
    }

    @Test
    void wholeWordSearchSkipsTextInsideLargerWords() {
        DocumentReadView document = document(
                block("1#1", DocumentReadView.Kind.PARAGRAPH, "test testing test_test test."));

        List<SearchMatch> matches = search.findWholeWord(document, "test");

        assertEquals(2, matches.size());
        assertEquals(0, matches.get(0).startIndex());
        assertEquals(23, matches.get(1).startIndex());
    }

    @Test
    void searchIncludesTitlesAndParagraphs() {
        DocumentReadView document = document(
                block("1", DocumentReadView.Kind.TITLE, "hello"),
                block("1#1", DocumentReadView.Kind.PARAGRAPH, "hello"));

        List<SearchMatch> matches = search.findWholeWord(document, "hello");

        assertEquals(2, matches.size());
        assertEquals(DocumentReadView.Kind.TITLE, matches.get(0).blockKind());
        assertEquals(DocumentReadView.Kind.PARAGRAPH, matches.get(1).blockKind());
        assertEquals("1#1", matches.get(1).blockId());
    }

    @Test
    void searchCanMatchAcrossFormattingRuns() {
        DocumentReadView.Block paragraph = new DocumentReadView.Block(
                "1#1",
                DocumentReadView.Kind.PARAGRAPH,
                List.of(
                        new DocumentReadView.TextRun("for"),
                        new DocumentReadView.TextRun(
                                "matted",
                                true,
                                false,
                                null,
                                null,
                                null)));

        List<SearchMatch> matches = search.findWholeWord(
                document(paragraph),
                "formatted");

        assertEquals(1, matches.size());
        assertEquals("1#1", matches.get(0).blockId());
    }

    @Test
    void emptySearchTextIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> search.findSubstring(document(), ""));
    }

    private DocumentReadView document(DocumentReadView.Block... blocks) {
        return () -> List.of(blocks);
    }

    private DocumentReadView.Block block(
            String id,
            DocumentReadView.Kind kind,
            String text) {
        return new DocumentReadView.Block(
                id,
                kind,
                List.of(new DocumentReadView.TextRun(text)));
    }
}
