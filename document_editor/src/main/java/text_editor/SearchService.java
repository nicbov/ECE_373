package text_editor;

import java.util.ArrayList;
import java.util.List;

/** finds visible text without changing the document */
public final class SearchService {

    public List<SearchMatch> findSubstring(DocumentTraversal document, String query) {
        // false means the query can be inside another word
        return find(document, query, false);
    }

    public List<SearchMatch> findWholeWord(DocumentTraversal document, String query) {
        // true means both sides of the query must be word boundaries
        return find(document, query, true);
    }

    // both public search methods use this shared search loop
    private List<SearchMatch> find(
            DocumentTraversal document,
            String query,
            boolean wholeWord) {
        // reject missing input before doing any work
        validate(document, query);

        // this list collects every match from every block
        List<SearchMatch> matches = new ArrayList<>();

        // the index tells us which title or paragraph contains the match
        int blockIndex = 0;

        // search each block in the order provided by the document
        for (DocumentTraversal.Block block : document.blocksInOrder()) {
            String text = block.visibleText();

            // each new block starts searching at its first character
            int searchFrom = 0;

            // stop once there is not enough text left for the query
            while (searchFrom <= text.length() - query.length()) {
                // indexof returns the next matching position or negative one
                int start = text.indexOf(query, searchFrom);
                if (start < 0) {
                    break;
                }

                // the end index is just after the final matched character
                int end = start + query.length();

                // substring mode accepts every match
                // whole word mode also checks the characters on both sides
                if (!wholeWord || hasWordBoundaries(text, start, end)) {
                    matches.add(new SearchMatch(
                            blockIndex,
                            block.type(),
                            start,
                            end,
                            text.substring(start, end)));
                }

                // move past this result so matches do not overlap
                searchFrom = end;
            }

            blockIndex++;
        }

        // copyof prevents callers from changing the result list
        return List.copyOf(matches);
    }

    private boolean hasWordBoundaries(String text, int start, int end) {
        // the start is valid at the beginning or after a nonword character
        boolean startsWord = start == 0 || !isWordCharacter(text.charAt(start - 1));

        // the end is valid at the end or before a nonword character
        boolean endsWord = end == text.length() || !isWordCharacter(text.charAt(end));
        return startsWord && endsWord;
    }

    private boolean isWordCharacter(char character) {
        // letters numbers and underscores count as part of a word
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private void validate(DocumentTraversal document, String query) {
        // searching requires a document and at least one search character
        if (document == null) {
            throw new IllegalArgumentException("Document traversal is required.");
        }
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Search text is required.");
        }
    }
}
