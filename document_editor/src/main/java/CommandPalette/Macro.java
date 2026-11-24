package CommandPalette;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores a named sequence of command lines that can accept ordered arguments.
 */
public class Macro extends Command {
    private final List<String> commandLines = new ArrayList<>();

    /**
     * Creates an empty macro that can be populated from stored record data.
     */
    public Macro() {
        super();
        arguments.clear();
    }

    /**
     * Creates a macro from its name, ordered arguments, and command lines.
     *
     * @param name macro name
     * @param macroArguments ordered argument names and types
     * @param lines command templates to replay
     */
    public Macro(String name, Map<String, String> macroArguments, List<String> lines) {
        this();
        this.name = name;
        if (macroArguments != null) {
            this.arguments.putAll(macroArguments);
        }
        if (lines != null) {
            this.commandLines.addAll(lines);
        }
    }

    /**
     * Converts this macro to the text format accepted by the macro dictionary.
     *
     * @return serialized macro record
     */
    public String Serialize() {
        StringBuilder output = new StringBuilder("~begin\n").append(GetFormat());
        for (String line : commandLines) {
            output.append('\n').append(line);
        }
        return output.toString();
    }

    /**
     * Returns the macro name and ordered argument declarations.
     *
     * @return macro header
     */
    @Override
    public String GetFormat() {
        StringBuilder format = new StringBuilder(name == null ? "" : name);
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            format.append(' ')
                    .append(entry.getKey())
                    .append(':')
                    .append(entry.getValue())
                    .append(',');
        }
        if (!arguments.isEmpty()) {
            format.setLength(format.length() - 1);
        }
        return format.toString();
    }

    /**
     * Loads one macro record without its {@code ~begin} marker.
     *
     * @param input macro header followed by command lines
     */
    public void ParseRecordData(String input) {
        reset();
        if (input == null || input.isBlank()) {
            return;
        }

        String[] lines = input.strip().split("\\R");
        String[] header = SeparateString(lines[0].trim());
        name = header.length == 0 ? "" : header[0];
        for (int index = 1; index < header.length; index++) {
            RegisterVariable(header[index]);
        }
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index].trim();
            if (!line.isBlank()) {
                commandLines.add(line);
            }
        }
    }

    /**
     * Expands a macro invocation into normal parsed system commands.
     *
     * @param input invocation containing the macro name and values
     * @return parsed commands in replay order
     * @throws IllegalArgumentException if arguments or referenced commands are invalid
     */
    public String[][][] ParseMacro(String input) {
        String[] values = SeparateString(input);
        if (values.length != arguments.size() + 1 || !values[0].equals(name)) {
            throw new IllegalArgumentException("Invalid macro arguments.");
        }

        Map<String, String> replacements = new LinkedHashMap<>();
        int valueIndex = 1;
        for (Map.Entry<String, String> argument : arguments.entrySet()) {
            String value = values[valueIndex++];
            if (argument.getValue().equals("number") && !value.matches("\\d+")) {
                throw new IllegalArgumentException("Expected a number for " + argument.getKey());
            }
            replacements.put(argument.getKey(), value);
        }

        String[][][] parsed = new String[commandLines.size()][][];
        for (int index = 0; index < commandLines.size(); index++) {
            String expanded = expand(commandLines.get(index), replacements);
            String commandName = SeparateString(expanded)[0];
            Command command = SystemDictionary.Get(commandName);
            if (command == null) {
                throw new IllegalArgumentException("Unknown macro command " + commandName);
            }
            parsed[index] = command.Parse(expanded);
        }
        return parsed;
    }

    /**
     * Checks whether this macro has a usable name and command body.
     *
     * @return true when the macro can be registered
     */
    @Override
    public boolean Validate() {
        if (name == null || name.isBlank() || commandLines.isEmpty()) {
            return false;
        }
        for (String line : commandLines) {
            String[] parts = SeparateString(line);
            if (parts.length == 0 || parts[0].isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Registers one argument declaration such as {@code location:number}.
     *
     * @param input argument declaration
     * @return true when the declaration was added
     * @throws IllegalArgumentException if the declaration is invalid
     */
    public boolean RegisterVariable(String input) {
        if (input == null || !input.matches("\\w+:(number|string)")) {
            throw new IllegalArgumentException("Invalid macro argument " + input);
        }
        String[] parsed = input.split(":", 2);
        if (arguments.containsKey(parsed[0])) {
            return false;
        }
        arguments.put(parsed[0], parsed[1]);
        return true;
    }

    /**
     * Clears all macro data.
     */
    public void reset() {
        name = "";
        arguments.clear();
        commandLines.clear();
    }

    private String expand(String line, Map<String, String> replacements) {
        String[] parts = SeparateString(line);
        StringBuilder expanded = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            expanded.append(' ')
                    .append(replacements.getOrDefault(parts[index], parts[index]));
            if (index < parts.length - 1) {
                expanded.append(',');
            }
        }
        return expanded.toString();
    }
}
