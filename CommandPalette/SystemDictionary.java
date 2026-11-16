package CommandPalette;
import java.util.Map;

public final class SystemDictionary {
    private Map<String, Command> supportedCommands = Map.ofEntries(entry("", Command()));
    private int commandCount;

    public Command GetCommand(String name){
        return supportedCommands.get(name);
    }
}
