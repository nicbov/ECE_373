package edu.arizona.ece373.CommandPalette;

import java.util.Map;

public class Macro extends Command{
    private Command[] CommandList;
    private String[][] VariableUse;
    private Map<String, String> VariableList;

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
