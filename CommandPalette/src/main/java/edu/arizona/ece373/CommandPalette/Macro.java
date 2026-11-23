package edu.arizona.ece373.CommandPalette;
import java.util.regex.*;
import java.util.Map;

public class Macro extends Command{
    private Command[] commandList = {};
    private Pattern variableID = Pattern.compile("\\w+:\\w+");
    private Pattern variableDefine = Pattern.compile(":");
    private Map<String, String> variableTyping;
    private final static Map<String, String> typeMapping = Map.ofEntries(Map.entry("digit", "\\d+"), Map.entry("string", "\\S+"));
    private int variableCount = 0;

    public String Serialize(){
        throw new UnsupportedOperationException();
    }

    public String GetFormat(){
        throw new UnsupportedOperationException();
    }

    public String[] Execute(){
        throw new UnsupportedOperationException();
    }

    public void ParseRecordData(String input){
        throw new UnsupportedOperationException();
    }

    public Command[] ParseReplayData(String input){
        throw new UnsupportedOperationException();
    }
}
