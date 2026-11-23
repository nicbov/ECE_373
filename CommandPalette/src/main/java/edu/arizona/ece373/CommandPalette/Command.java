package edu.arizona.ece373.CommandPalette;
import java.util.regex.*;
import java.util.Map;
import java.util.Set;

public class Command{
    // RegEx patterns used to parse Strings into their separate components
    protected final Pattern splitSpaces = Pattern.compile(" ");
    protected final Pattern splitCommas = Pattern.compile(","); 
    protected final static Map<String, String> formatParser = Map.ofEntries(Map.entry("\\S+", "string"), Map.entry("\\d+", "digit"));
    // Possibly depricated
    protected int index;
    // Contains the RegEx pattern used to discern proper command formatting
    protected Pattern contents;
    // Contains the String RegEx used to compile the contents Pattern
    protected String uncompiledContents;
    // Used to match a command to its editing action
    protected String name;
    // Holds arguments
    protected Map<String, String> arguments;

    public Command(int index, String name, String uncompiledContents, Map<String, String> arguments){
        this.index = index;
        this.name = name;
        this.uncompiledContents = uncompiledContents;
        this.arguments = arguments;
    }

    public Command(){
        this(-1, "", "", Map.of("", ""));
    }

    public void CompileCommand(){
        this.contents = Pattern.compile(uncompiledContents);
    }

    public void SetIndex(int index){
        this.index = index;
    }

    public void SetContents(String contents){
        this.uncompiledContents = contents;
    }

    public void SetName(String name){
        this.name = name;
    }

    public void SetArguments(String arguments){
        this.arguments = arguments;
    }

    public int GetIndex(){
        return index;
    }

    public Pattern GetContents(){
        return contents;
    }

    public String GetName(){
        return name;
    }

    public String GetArguments(){
        return arguments;
    }

    public String GetFormat(){
        throw new UnsupportedOperationException();
    }

}