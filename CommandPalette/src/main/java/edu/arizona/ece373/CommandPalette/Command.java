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

    public void SetArguments(Map<String, String> arguments){
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

    public Set<String> GetArguments(){
        return arguments.keySet();
    }

    public boolean CheckValid(String input){
        if(contents == null){
            return false;
        }
        Matcher m = contents.matcher(input);
        return m.matches();
    }

    public String[] SeparateString(String input){
        String [] Output = splitSpaces.split(input);
        for(String i : Output){
            i = splitCommas.split(i)[0];
        }
        return Output;
    }

    public String GetFormat(){
        String returnVal = "";
        String[] rawFormat = SeparateString(uncompiledContents);
        Set<String> varNames = arguments.keySet();
        int index = 0;
        for(int i = 0; i < rawFormat.length; i++){
            if(i == 0){
                returnVal += rawFormat[i];
            }
            else{
                returnVal += " " + [index] + '[' + formatParser.get(rawFormat[i]) + ']';
                index++;
            }
        }
        return returnVal;
    }

}