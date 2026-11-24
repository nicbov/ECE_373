package text_editor;

import CommandPalette.CommandPalette;
import java.nio.file.Path;
import java.util.List;
import presentation.GreedyLineBreaker;
import presentation.PlainTextView;
import presentation.RenderService;
import presentation.RenderSettings;
import presentation.StatisticsView;

// connects commands to the document services and presentation layer
public final class EditorApplication {

    private final DocumentEngine engine;
    private final CommandPalette commandPalette;
    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final SearchReplaceService replacementService;
    private final RenderService renderService;
    private final StatisticsView statisticsView;
    private RenderSettings renderSettings;

    public EditorApplication() {
        this(DocumentEngine.getInstance());
    }

    EditorApplication(DocumentEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Document engine is required.");
        }

        // these objects are shared by every command handled by the application
        this.engine = engine;
        this.commandPalette = new CommandPalette();
        this.statisticsService = new StatisticsService();
        this.searchService = new SearchService();
        this.replacementService = new SearchReplaceService();
        this.renderService = new RenderService();
        this.statisticsView = new StatisticsView();

        // rendering starts with the simple choices from the design document
        this.renderSettings = new RenderSettings(
                new PlainTextView(),
                new GreedyLineBreaker(),
                80);
    }

    public void setDocumentDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Document directory is required.");
        }
        engine.setBaseDirectory(directory);
    }

    public Document createDocument(String name) {
        return engine.createDocument(name);
    }

    public Document openDocument(String name) {
        return engine.openDocument(name);
    }

    public void switchDocument(String name) {
        engine.switchDocument(name);
    }

    public Document currentDocument() {
        return engine.getDocument();
    }

    public TextEditService edits() {
        return engine.getTextEditService();
    }

    public CheckPointService checkpoints() {
        return new CheckPointService(currentDocument());
    }

    public void setRenderSettings(RenderSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Render settings are required.");
        }
        this.renderSettings = settings;
    }

    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    // calculates current values then lets the presentation layer format them
    public String showStatistics() {
        SectionTreeReadView view = currentReadView();
        DocumentStatistics statistics = statisticsService.calculate(view);
        String output = statisticsView.render(statistics);
        print(output);
        return output;
    }

    // rendering reads the same view used by statistics and search
    public String renderDocument() {
        String output = renderService.render(currentReadView(), renderSettings);
        print(output);
        return output;
    }

    public List<SearchMatch> findSubstring(String query) {
        List<SearchMatch> matches = searchService.findSubstring(currentReadView(), query);
        print(formatMatches(matches));
        return matches;
    }

    public List<SearchMatch> findWholeWord(String query) {
        List<SearchMatch> matches = searchService.findWholeWord(currentReadView(), query);
        print(formatMatches(matches));
        return matches;
    }

    public boolean replaceFirstSubstring(String query, String replacement) {
        boolean changed = replacementService.replaceFirstSubstring(
                currentDocument(), query, replacement);
        print(changed ? "replaced 1 match" : "no matches found");
        return changed;
    }

    public boolean replaceFirstWholeWord(String query, String replacement) {
        boolean changed = replacementService.replaceFirstWholeWord(
                currentDocument(), query, replacement);
        print(changed ? "replaced 1 match" : "no matches found");
        return changed;
    }

    public int replaceAllSubstring(String query, String replacement) {
        int changed = replacementService.replaceAllSubstring(
                currentDocument(), query, replacement);
        print("replaced " + changed + " matches");
        return changed;
    }

    public int replaceAllWholeWord(String query, String replacement) {
        int changed = replacementService.replaceAllWholeWord(
                currentDocument(), query, replacement);
        print("replaced " + changed + " matches");
        return changed;
    }

    public void print(String output) {
        // max's command palette owns console output
        commandPalette.Print(output);
    }

    private SectionTreeReadView currentReadView() {
        return new SectionTreeReadView(currentDocument().getSectionTree());
    }

    private String formatMatches(List<SearchMatch> matches) {
        if (matches.isEmpty()) {
            return "no matches found";
        }

        StringBuilder output = new StringBuilder();
        for (SearchMatch match : matches) {
            if (output.length() > 0) {
                output.append('\n');
            }
            output.append(match.blockId())
                    .append(" [")
                    .append(match.startIndex())
                    .append(',')
                    .append(match.endIndex())
                    .append(") ")
                    .append(match.text());
        }
        return output.toString();
    }
}
