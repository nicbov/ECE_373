package CommandPalette;
import java.util.regex.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Command{
    // RegEx patterns used to parse Strings into their separate components
    protected final Pattern splitSpaces = Pattern.compile(" ");
    protected final Pattern splitCommas = Pattern.compile(","); 
    protected final static Map<String, String> formatParser = Map.ofEntries(Map.entry("\\S+", "string"), Map.entry("\\d+", "number"));
    protected final static Map<String, String> typeMapping = Map.ofEntries(Map.entry("number", "\\d+"), Map.entry("string", "\\S+"));
    // Contains number of expected arguments for string parsing
    protected int argCount;
    // Contains the RegEx pattern used to discern proper command formatting
    protected Pattern contents;
    // Contains the String RegEx used to compile the contents Pattern
    protected String uncompiledContents;
    // Used to match a command to its editing action
    protected String name;
    // Holds arguments in <name, type> format
    protected LinkedHashMap<String, String> arguments;

    public Command(int argCount, String name, String uncompiledContents, String[][] arguments){
        this.argCount = argCount;
        this.name = name;
        this.uncompiledContents = uncompiledContents;
        LinkedHashMap<String,String> testing = new LinkedHashMap<>(); 
        for(String[] i : arguments){
            testing.put(i[0], i[1]);
        }
        this.arguments = new LinkedHashMap<>(testing);
    }

    public Command(){
        // this(-1, "", "", ));
    }

    public void CompileCommand(){
        this.contents = Pattern.compile(uncompiledContents);
    }

    public void SetArgCount(int argCount){
        this.argCount = argCount;
    }

    public void SetContents(String contents){
        this.uncompiledContents = contents;
    }

    public void SetName(String name){
        this.name = name;
    }

    public void SetArguments(Map<String, String> arguments){
        this.arguments = new LinkedHashMap<>(arguments);
    }

    public int GetArgCount(){
        return argCount;
    }

    public Pattern GetContents(){
        return contents;
    }

    public String GetUncompiledContents(){
        return uncompiledContents;
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

    protected String[] SortKeys(Object[] o, Set<String> k){
        int index = 0;
        String[] output = new String[argCount + 1];
        for(Object i : k){
            output[index] = (String)i;
            index++;
        }
        return output;
    }

    public String[][] Parse(String input){
        return CommandParse(input);
    }

    public String[][] CommandParse(String input){
        String[][] Output = new String[2][argCount + 1];
        // String[] keys = SortKeys(arguments.keySet().toArray(), arguments.keySet());
        Set<String> keys = arguments.keySet();
        Iterator<Entry<String,String>> keyIt = arguments.entrySet().iterator();
        if(CheckValid(input)){
            String[] splitString = SeparateString(input);
            Output[0][0] = splitString[0];
            for(int i = 1; i < splitString.length; i++){
                Output[0][i] = splitString[i];
                String next = keyIt.next().getValue();
                System.out.println(i + " " + next);
                System.out.println(next + " || ");
                Output[1][i] = typeMapping.get(next);
            }
            return Output;
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    public String[] SeparateString(String input){
        String[] Output = splitSpaces.split(input);
        for(int i = 0; i < Output.length; i++){
            Output[i] = splitCommas.split(Output[i])[0];
        }
        return Output;
    }

    /*
    public boolean Validate(){
        String ex = name;
        for(String i : arguments.keySet()){
            ex += " " + arguments.get(i) + ",";
        }
        ex = ex.substring(0, ex.length() - 1);
        if(contents == null){
            this.CompileCommand();
        }
        System.out.println(ex + "|");
        System.out.println(uncompiledContents + "|");
        if(contents != null){
            Matcher m = contents.matcher(ex);
            return m.matches();
        }
        else{
            return false;
        }
    }*/

    public String GetFormat(){
        String returnVal = "";
        String[] rawFormat = SeparateString(uncompiledContents);
        Object[] keys = SortKeys(arguments.keySet().toArray(), arguments.keySet());
        int index = 0;
        for(int i = 0; i < rawFormat.length; i++){
            if(i == 0){
                returnVal += rawFormat[i];
            }
            else{
                returnVal += " " + (String)keys[index] + '[' + formatParser.get(rawFormat[i]) + ']';
                index++;
            }
        }
        return returnVal;
    }

}