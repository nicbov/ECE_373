package CommandPalette;
import java.util.regex.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Defines a named command and parses input according to its argument format.
 */
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

    /**
     * Creates a command definition.
     *
     * @param argCount expected argument count
     * @param name command name
     * @param uncompiledContents regular expression used to validate input
     * @param arguments ordered argument names and types
     */
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

    /**
     * Creates an empty command definition.
     */
    public Command(){
        this(-1, "", "", new String[][] {{"", ""}});
    }

    /**
     * Compiles the configured input-validation expression.
     *
     * @throws java.util.regex.PatternSyntaxException if the expression is invalid
     */
    public void CompileCommand(){
        this.contents = Pattern.compile(uncompiledContents);
    }

    /**
     * Sets the expected argument count.
     *
     * @param argCount expected argument count
     */
    public void SetArgCount(int argCount){
        this.argCount = argCount;
    }

    /**
     * Sets the uncompiled input-validation expression.
     *
     * @param contents regular expression for valid input
     */
    public void SetContents(String contents){
        this.uncompiledContents = contents;
    }

    /**
     * Sets the command name.
     *
     * @param name command name
     */
    public void SetName(String name){
        this.name = name;
    }

    /**
     * Replaces the ordered argument definitions.
     *
     * @param arguments argument names mapped to types
     */
    public void SetArguments(Map<String, String> arguments){
        this.arguments = new LinkedHashMap<>(arguments);
    }

    /**
     * Returns the expected argument count.
     *
     * @return expected argument count
     */
    public int GetArgCount(){
        return argCount;
    }

    /**
     * Returns the compiled input-validation pattern.
     *
     * @return compiled pattern, or {@code null} if not compiled
     */
    public Pattern GetContents(){
        return contents;
    }

    /**
     * Returns the input-validation expression.
     *
     * @return uncompiled regular expression
     */
    public String GetUncompiledContents(){
        return uncompiledContents;
    }

    /**
     * Returns the command name.
     *
     * @return command name
     */
    public String GetName(){
        return name;
    }

    /**
     * Returns the ordered argument names.
     *
     * @return argument-name view
     */
    public Set<String> GetArguments(){
        return arguments.keySet();
    }

    /**
     * Tests input against the compiled command pattern.
     *
     * @param input complete command input
     * @return {@code true} if the input matches
     */
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

    /**
     * Parses valid command input into values and type patterns.
     *
     * @param input complete command input
     * @return parsed values and corresponding type patterns
     * @throws IllegalArgumentException if the input is invalid
     */
    public String[][] Parse(String input){
        return CommandParse(input);
    }

    /**
     * Parses valid command input into values and type patterns.
     *
     * @param input complete command input
     * @return parsed values and corresponding type patterns
     * @throws IllegalArgumentException if the input is invalid
     */
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
                Output[1][i] = typeMapping.get(next);
            }
            return Output;
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    /**
     * Splits command input into comma-trimmed space-delimited tokens.
     *
     * @param input command input
     * @return parsed tokens
     */
    public String[] SeparateString(String input){
        String[] Output = splitSpaces.split(input);
        for(int i = 0; i < Output.length; i++){
            Output[i] = splitCommas.split(Output[i])[0];
        }
        return Output;
    }

    /**
     * Checks whether the configured expression matches the command definition.
     *
     * @return {@code true} if the definition is internally consistent
     */
    public boolean Validate(){
        String ex = name;
        for(String i : arguments.keySet()){
            String argumentType = arguments.get(i);
            ex += " " + typeMapping.getOrDefault(argumentType, argumentType) + ",";
        }
        if (!arguments.isEmpty()) {
            ex = ex.substring(0, ex.length() - 1);
        }
        return uncompiledContents.equals(ex);
    }

    /**
     * Returns a human-readable command invocation format.
     *
     * @return command format with named, typed arguments
     */
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