package CommandPalette;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the system commands available to the command palette.
 */
public final class SystemDictionary {
    private static final Map<String, Command> supportedCommands = new LinkedHashMap<>();

    private SystemDictionary() { }

    /**
     * Registers or replaces a command by name.
     *
     * @param command command to register
     * @throws IllegalArgumentException if the command or its name is invalid
     */
    public static void Register(Command command){
        if(command == null || command.GetName() == null || command.GetName().isBlank()){
            throw new IllegalArgumentException("Command name is required.");
        }
        supportedCommands.put(command.GetName(), command);
    }

    /**
     * Removes all registered system commands.
     */
    public static void Clear(){
        supportedCommands.clear();
    }

    /**
     * Returns a registered command.
     *
     * @param name command name
     * @return matching command, or {@code null} if none exists
     */
    public static Command Get(String name){
        return supportedCommands.get(name);
    }

    /**
     * Reports whether a command is registered.
     *
     * @param name command name
     * @return {@code true} if the command exists
     */
    public static boolean Contains(String name){
        return supportedCommands.containsKey(name);
    }

    /**
     * Returns the display format for a command.
     *
     * @param name command name
     * @return command format, or an empty string if not found
     */
    public static String ListCommandFormat(String name){
        Command c = supportedCommands.get(name);
        return c == null ? "" : c.GetFormat();
    }

    /**
     * Lists all registered command formats.
     *
     * @return one command format per line
     */
    public static String ListAll(){
        StringBuilder returnString = new StringBuilder();
        for(Command c : supportedCommands.values()){
            if (returnString.length() > 0) {
                returnString.append('\n');
            }
            returnString.append(c.GetFormat());
        }
        return returnString.toString();
    }
}
