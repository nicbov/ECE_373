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

    public boolean validate(){
        for(Command i : commandList){
            
        }
    }

    public boolean RegisterVariable(String input){
        String[] parsed = variableDefine.split(input);
        if(variableTyping.get(parsed[0]) != null){
            return false;
        }
        variableTyping.put(parsed[0], parsed[1]);
        argumentNames[variableCount] = parsed[0];
        variableCount++;
        return true;
    }

    public void reset(){
        variableTyping.clear();
        commandList = new Command[0];
        argumentNames = new String[0];
        variableCount = 0;
    }
}
