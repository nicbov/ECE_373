package text_editor;

import java.util.List;
import presentation.DocumentReadView;

// coordinates search results with shared text editing operations
public final class SearchReplaceService {

    // this service only finds matches and never edits by itself
    private final SearchService search = new SearchService();

    // false tells the shared method to allow matches inside larger words
    public boolean replaceFirstSubstring(
            Document document, String query, String replacement) {
        return replaceFirst(document, query, replacement, false);
    }

    // true tells the shared method to require word boundaries
    public boolean replaceFirstWholeWord(
            Document document, String query, String replacement) {
        return replaceFirst(document, query, replacement, true);
    }

    // returns how many substring matches were replaced
    public int replaceAllSubstring(
            Document document, String query, String replacement) {
        return replaceAll(document, query, replacement, false);
    }

    // returns how many complete word matches were replaced
    public int replaceAllWholeWord(
            Document document, String query, String replacement) {
        return replaceAll(document, query, replacement, true);
    }

    private boolean replaceFirst(
            Document document,
            String query,
            String replacement,
            boolean wholeWord) {
        if (document == null || replacement == null) {
            throw new IllegalArgumentException("Document and replacement text are required.");
        }

        // the read view gives search ordered titles and paragraphs with stable ids
        SectionTreeReadView view = new SectionTreeReadView(document.getSectionTree());

        // the conditional chooses one search mode without duplicating the rest
        List<SearchMatch> matches = wholeWord
                ? search.findWholeWord(view, query)
                : search.findSubstring(view, query);

        // no match is a normal result and does not create history
        if (matches.isEmpty()) {
            return false;
        }

        // index zero is the first match in document order
        SearchMatch match = matches.get(0);
        TextEditService edits = new TextEditService(document);

        // titles and paragraphs use different structure lookup methods
        if (match.blockKind() == DocumentReadView.Kind.TITLE) {
            edits.replaceSectionTitle(
                    match.blockId(),
                    match.startIndex(),
                    match.endIndex(),
                    replacement);
        } else {
            edits.replaceText(
                    match.blockId(),
                    match.startIndex(),
                    match.endIndex(),
                    replacement);
        }

        // true tells the caller one replacement was saved
        return true;
    }

    private int replaceAll(
            Document document,
            String query,
            String replacement,
            boolean wholeWord) {
        if (document == null || replacement == null) {
            throw new IllegalArgumentException("Document and replacement text are required.");
        }

        // search once so every position refers to the same document version
        SectionTreeReadView view = new SectionTreeReadView(document.getSectionTree());
        List<SearchMatch> matches = wholeWord
                ? search.findWholeWord(view, query)
                : search.findSubstring(view, query);

        // no matches means there is nothing to save or add to history
        if (matches.isEmpty()) {
            return 0;
        }

        // the edit service applies the full list as one saved change
        TextEditService edits = new TextEditService(document);
        edits.replaceMatches(matches, replacement);

        // callers can show the number of changed matches
        return matches.size();
    }
}
