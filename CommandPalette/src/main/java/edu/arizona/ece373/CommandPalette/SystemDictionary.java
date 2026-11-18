package CommandPalette.src.main.java.edu.arizona.ece373.CommandPalette;
import java.util.Map;

public final class SystemDictionary {
    private Map<String, Command> supportedCommands = Map.ofEntries(Map.entry("", new Command()));
    private int commandCount;

    public Command GetCommand(String name){
        return supportedCommands.get(name);
    }
}
