package text_editor;

import CommandPalette.CommandPalette;
import CommandPalette.Command;
import CommandPalette.SystemDictionary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import presentation.BalancedLineBreaker;
import presentation.GreedyLineBreaker;
import presentation.MarkedUpView;
import presentation.PlainTextView;
import presentation.RenderService;
import presentation.RenderSettings;
import presentation.StatisticsView;

/**
 * Facade that connects commands to document and presentation services.
 */
public final class EditorApplication {

    private final DocumentEngine engine;
    private final CommandPalette commandPalette;
    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final SearchReplaceService replacementService;
    private final RenderService renderService;
    private final StatisticsView statisticsView;
    private RenderSettings renderSettings;

    /** Creates an application backed by the shared document engine. */
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

        // the palette sends parsed commands back to this application
        CommandPalette.Connect(this);
        registerCommands();
    }

    /** Sets the directory used to create and open documents. */
    public void setDocumentDirectory(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Document directory is required.");
        }
        engine.setBaseDirectory(directory);
    }

    /** Creates and selects a document. */
    public Document createDocument(String name) {
        return engine.createDocument(name);
    }

    /** Opens and selects an existing document. */
    public Document openDocument(String name) {
        return engine.openDocument(name);
    }

    /** Selects a document already open in the engine. */
    public void switchDocument(String name) {
        engine.switchDocument(name);
    }

    /** Returns the active document. */
    public Document currentDocument() {
        return engine.getDocument();
    }

    /** Returns an editing service for the active document. */
    public TextEditService edits() {
        return engine.getTextEditService();
    }

    /** Starts one atomic group on the active document. */
    public void beginEditGroup() {
        edits().beginEditGroup();
    }

    /** Saves the active group as one undoable edit. */
    public void endEditGroup() {
        edits().endEditGroup();
    }

    /** Rolls back every edit in the active group. */
    public void abortEditGroup() {
        edits().abortEditGroup();
    }

    /** Returns a checkpoint service for the active document. */
    public CheckPointService checkpoints() {
        return new CheckPointService(currentDocument());
    }

    /** Replaces the current rendering choices. */
    public void setRenderSettings(RenderSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Render settings are required.");
        }
        this.renderSettings = settings;
    }

    /** Returns the current rendering choices. */
    public RenderSettings getRenderSettings() {
        return renderSettings;
    }

    /** Calculates, prints, and returns statistics for the active document. */
    public String showStatistics() {
        SectionTreeReadView view = currentReadView();
        DocumentStatistics statistics = statisticsService.calculate(view);
        String output = statisticsView.render(statistics);
        print(output);
        return output;
    }

    /** Renders, prints, and returns the active document. */
    public String renderDocument() {
        String output = renderService.render(currentReadView(), renderSettings);
        print(output);
        return output;
    }

    /** Finds and prints every substring match. */
    public List<SearchMatch> findSubstring(String query) {
        List<SearchMatch> matches = searchService.findSubstring(currentReadView(), query);
        print(formatMatches(matches));
        return matches;
    }

    /** Finds and prints every whole-word match. */
    public List<SearchMatch> findWholeWord(String query) {
        List<SearchMatch> matches = searchService.findWholeWord(currentReadView(), query);
        print(formatMatches(matches));
        return matches;
    }

    /** Replaces and reports the first substring match. */
    public boolean replaceFirstSubstring(String query, String replacement) {
        boolean changed = replacementService.replaceFirstSubstring(
                currentDocument(), query, replacement);
        print(changed ? "replaced 1 match" : "no matches found");
        return changed;
    }

    /** Replaces and reports the first whole-word match. */
    public boolean replaceFirstWholeWord(String query, String replacement) {
        boolean changed = replacementService.replaceFirstWholeWord(
                currentDocument(), query, replacement);
        print(changed ? "replaced 1 match" : "no matches found");
        return changed;
    }

    /** Replaces and reports all substring matches as one edit. */
    public int replaceAllSubstring(String query, String replacement) {
        int changed = replacementService.replaceAllSubstring(
                currentDocument(), query, replacement);
        print("replaced " + changed + " matches");
        return changed;
    }

    /** Replaces and reports all whole-word matches as one edit. */
    public int replaceAllWholeWord(String query, String replacement) {
        int changed = replacementService.replaceAllWholeWord(
                currentDocument(), query, replacement);
        print("replaced " + changed + " matches");
        return changed;
    }

    /** Executes one interactive or scripted command line. */
    public void executeLine(String input) {
        try {
            CommandPalette.ReadInput(input);
        } catch (IllegalArgumentException error) {
            print("invalid syntax: " + error.getMessage());
        } catch (IllegalStateException error) {
            print("operation failed: " + error.getMessage());
        }
    }

    /** Executes every line in a script through the normal command path. */
    public void executeScript(Path script) {
        if (script == null) {
            throw new IllegalArgumentException("Script path is required.");
        }
        try {
            for (String line : Files.readAllLines(script)) {
                executeLine(line);
            }
        } catch (IOException error) {
            throw new UncheckedIOException("Could not read script " + script, error);
        }
    }

    /** Dispatches parsed command data to the matching service. */
    public void executeCommand(String[][] data) {
        if (data == null || data.length == 0 || data[0].length == 0) {
            throw new IllegalArgumentException("Command data is required.");
        }
        String[] values = data[0];
        String name = values[0];

        switch (name) {
            case "create" -> { createDocument(values[1]); print("document created"); }
            case "open" -> { openDocument(values[1]); print("document opened"); }
            case "switch" -> { switchDocument(values[1]); print("document switched"); }
            case "add-section" -> print(edits().addSection(values[1]));
            case "add-subsection" -> print(edits().addSubSection(values[1], values[2]));
            case "remove-section" -> { edits().removeSection(values[1]); print("section removed"); }
            case "move-section" -> {
                edits().moveSection(values[1], values[2], number(values[3]));
                print("section moved");
            }
            case "add-paragraph" -> print(edits().addParagraph(values[1], values[2]));
            case "insert" -> {
                edits().addText(values[1], number(values[2]), values[3]);
                print("text inserted");
            }
            case "delete" -> {
                edits().removeText(values[1], number(values[2]), number(values[3]));
                print("text deleted");
            }
            case "replace" -> {
                edits().replaceText(values[1], number(values[2]), number(values[3]), values[4]);
                print("text replaced");
            }
            case "move" -> {
                edits().moveText(values[1], number(values[2]), number(values[3]),
                        values[4], number(values[5]));
                print("text moved");
            }
            case "split" -> print(edits().splitParagraph(values[1], number(values[2])));
            case "merge" -> {
                edits().mergeParagraphs(values[1], values[2]);
                print("paragraphs merged");
            }
            case "format" -> {
                String argument = values[5].equals("-") ? null : values[5];
                edits().addFormat(values[1], number(values[2]), number(values[3]),
                        TagType.fromCode(values[4]), argument);
                print("format added");
            }
            case "remove-format" -> {
                edits().removeFormat(values[1], number(values[2]), number(values[3]),
                        TagType.fromCode(values[4]));
                print("format removed");
            }
            case "format-title" -> {
                String argument = values[5].equals("-") ? null : values[5];
                edits().addSectionTitleFormat(values[1], number(values[2]), number(values[3]),
                        TagType.fromCode(values[4]), argument);
                print("title format added");
            }
            case "remove-title-format" -> {
                edits().removeSectionTitleFormat(values[1], number(values[2]), number(values[3]),
                        TagType.fromCode(values[4]));
                print("title format removed");
            }
            case "undo" -> print(edits().undo() ? "edit undone" : "nothing to undo");
            case "redo" -> print(edits().redo() ? "edit redone" : "nothing to redo");
            case "statistics" -> showStatistics();
            case "render" -> renderDocument();
            case "find-substring" -> findSubstring(values[1]);
            case "find-word" -> findWholeWord(values[1]);
            case "replace-first-substring" -> replaceFirstSubstring(values[1], values[2]);
            case "replace-first-word" -> replaceFirstWholeWord(values[1], values[2]);
            case "replace-all-substring" -> replaceAllSubstring(values[1], values[2]);
            case "replace-all-word" -> replaceAllWholeWord(values[1], values[2]);
            case "checkpoint-create" -> {
                checkpoints().createAtCurrentVersion(values[1]);
                print("checkpoint created");
            }
            case "checkpoint-restore" -> {
                checkpoints().restore(values[1]);
                print("checkpoint restored");
            }
            case "checkpoint-preview" -> previewCheckpoint(values[1]);
            case "render-view" -> selectRenderView(values[1]);
            case "render-breaker" -> selectLineBreaker(values[1]);
            case "render-width" -> {
                setRenderSettings(new RenderSettings(renderSettings.view(),
                        renderSettings.lineBreaker(), number(values[1])));
                print("render width updated");
            }
            case "help" -> print(help());
            default -> throw new IllegalArgumentException("Unknown command '" + name + "'");
        }
    }

    /** Sends user-facing output through the command palette. */
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

    private int number(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Expected a number but got '" + value + "'.");
        }
    }

    private void registerCommands() {
        SystemDictionary.Clear();
        register("create", "string");
        register("open", "string");
        register("switch", "string");
        register("add-section", "string");
        register("add-subsection", "string", "string");
        register("remove-section", "string");
        register("move-section", "string", "string", "number");
        register("add-paragraph", "string", "string");
        register("insert", "string", "number", "string");
        register("delete", "string", "number", "number");
        register("replace", "string", "number", "number", "string");
        register("move", "string", "number", "number", "string", "number");
        register("split", "string", "number");
        register("merge", "string", "string");
        register("format", "string", "number", "number", "string", "string");
        register("remove-format", "string", "number", "number", "string");
        register("format-title", "string", "number", "number", "string", "string");
        register("remove-title-format", "string", "number", "number", "string");
        register("undo");
        register("redo");
        register("statistics");
        register("render");
        register("find-substring", "string");
        register("find-word", "string");
        register("replace-first-substring", "string", "string");
        register("replace-first-word", "string", "string");
        register("replace-all-substring", "string", "string");
        register("replace-all-word", "string", "string");
        register("checkpoint-create", "string");
        register("checkpoint-restore", "string");
        register("checkpoint-preview", "string");
        register("render-view", "string");
        register("render-breaker", "string");
        register("render-width", "number");
        register("help");
    }

    private void register(String name, String... types) {
        String[][] arguments = new String[types.length][2];
        StringBuilder pattern = new StringBuilder(name);
        for (int i = 0; i < types.length; i++) {
            arguments[i][0] = "argument" + (i + 1);
            arguments[i][1] = types[i];
            pattern.append(' ').append(types[i].equals("number") ? "\\d+" : "\\S+");
            if (i < types.length - 1) {
                pattern.append(',');
            }
        }
        Command command = new Command(types.length, name, pattern.toString(), arguments);
        command.CompileCommand();
        SystemDictionary.Register(command);
    }

    private void previewCheckpoint(String name) {
        try (CheckPointService.Preview preview = checkpoints().preview(name)) {
            print(renderService.render(preview.readView(), renderSettings));
        }
    }

    private void selectRenderView(String name) {
        setRenderSettings(new RenderSettings(
                switch (name) {
                    case "plain" -> new PlainTextView();
                    case "marked" -> new MarkedUpView();
                    default -> throw new IllegalArgumentException(
                            "Render view must be plain or marked.");
                },
                renderSettings.lineBreaker(),
                renderSettings.width()));
        print("render view updated");
    }

    private void selectLineBreaker(String name) {
        setRenderSettings(new RenderSettings(
                renderSettings.view(),
                switch (name) {
                    case "greedy" -> new GreedyLineBreaker();
                    case "balanced" -> new BalancedLineBreaker();
                    default -> throw new IllegalArgumentException(
                            "Line breaker must be greedy or balanced.");
                },
                renderSettings.width()));
        print("line breaker updated");
    }

    private String help() {
        return SystemDictionary.ListAll()
                + "\nmacro-start name begins recording and macro-stop saves it";
    }
}
