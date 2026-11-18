package edu.arizona.ece373.CommandPalette;
import java.util.Map;

//import edu.arizona.ece373.CommandPalette.Command;

public final class SystemDictionary extends CommandDictionary{
    private Map<String, Command> supportedCommands = Map.ofEntries(Map.entry("", new Command()));
    private int commandCount;

    public Command GetCommand(String name){
        return supportedCommands.get(name);
    }

    public String ListCommandFormat(String name){
        Command c = supportedCommands.get(name);
        return c.GetName() + ": " + c.GetContents() + ", " + c.GetArguments();
    }

    public String ListAll(){
        String returnString = "";
        for(Command c : supportedCommands.values()){
            returnString += c.GetName() + ": " + c.GetContents() + ", " + c.GetArguments() + '\n';
        }
        return returnString;
    }
}
