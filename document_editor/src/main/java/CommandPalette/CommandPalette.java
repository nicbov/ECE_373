package CommandPalette;

import java.util.ArrayList;
import java.util.List;
import text_editor.EditorApplication;

/**
 * Reads user input, parses commands, and sends them to the application.
 */
public class CommandPalette {

    private static EditorApplication application;
    private static String recordingHeader;
    private static final List<String> recordedLines = new ArrayList<>();

    /**
     * Connects the palette to the editor application.
     *
     * @param target application that executes parsed commands
     * @throws IllegalArgumentException if {@code target} is {@code null}
     */
    public static void Connect(EditorApplication target){
        if(target == null){
            throw new IllegalArgumentException("Application is required.");
        }
        application = target;
    }

    /**
     * Prints a palette message.
     *
     * @param s message to print
     */
    public static void Print(String s){
        System.out.println(s);
    }

    /**
     * Loads serialized macros into the macro dictionary.
     *
     * @param s serialized macro records
     * @throws IllegalArgumentException if a macro record is invalid
     */
    public static void LoadMacros(String s){
        MacroDictionary.LoadMacros(s);
    }

    /**
     * Sends parsed command data to the connected application.
     *
     * @param s parsed command data
     * @return the same command data
     * @throws IllegalStateException if no application is connected
     */
    public static String[][] SendData(String[][] s){
        if(application == null){
            throw new IllegalStateException("Command palette is not connected.");
        }
        application.executeCommand(s);
        return s;
    }

    /**
     * Parses and executes a registered system command.
     *
     * @param input complete command input
     * @param name registered command name
     * @throws IllegalArgumentException if the input does not match the command
     */
    public static void ExportCommand(String input, String name){
        Command c = SystemDictionary.Get(name);
        SendData(c.Parse(input));
    }

    /**
     * Interprets and executes one command-palette input line.
     *
     * @param input command, macro invocation, or recording instruction
     * @throws IllegalArgumentException if command or macro arguments are invalid
     * @throws IllegalStateException if execution or recording state is invalid
     */
    public static void ReadInput(String input){
        if(input == null || input.isBlank()){
            Print("invalid syntax");
            return;
        }
        if (input.startsWith("macro-start ")) {
            startRecording(input.substring("macro-start ".length()));
            return;
        }
        if (input.equals("macro-stop")) {
            stopRecording();
            return;
        }
        if (input.equals("macro-cancel")) {
            recordingHeader = null;
            recordedLines.clear();
            Print("macro recording cancelled");
            return;
        }

        Command parser = new Command();
        String name = parser.SeparateString(input)[0];
        if(SystemDictionary.Get(name) != null){
            ExportCommand(input, name);
            if (recordingHeader != null) {
                recordedLines.add(input);
            }
        }
        else if(MacroDictionary.Get(name) != null){
            Macro m = MacroDictionary.Get(name);
            application.beginEditGroup();
            try {
                for(String[][] i : m.ParseMacro(input)){
                    SendData(i);
                }
                application.endEditGroup();
            } catch (RuntimeException error) {
                application.abortEditGroup();
                throw error;
            }
        }
        else{
            Print("unknown command");
        }
    }

    private static void startRecording(String header) {
        if (recordingHeader != null) {
            throw new IllegalStateException("A macro is already being recorded.");
        }
        Macro candidate = new Macro();
        candidate.ParseRecordData(header + "\nplaceholder");
        if (candidate.GetName().isBlank()) {
            throw new IllegalArgumentException("Macro name is required.");
        }
        recordingHeader = header;
        recordedLines.clear();
        Print("macro recording started");
    }

    private static void stopRecording() {
        if (recordingHeader == null) {
            throw new IllegalStateException("No macro is being recorded.");
        }
        StringBuilder record = new StringBuilder(recordingHeader);
        for (String line : recordedLines) {
            record.append('\n').append(line);
        }
        Macro macro = new Macro();
        macro.ParseRecordData(record.toString());
        recordingHeader = null;
        recordedLines.clear();
        if (!MacroDictionary.RegisterMacro(macro)) {
            throw new IllegalArgumentException("A macro must contain at least one command.");
        }
        Print("macro saved");
    }
}