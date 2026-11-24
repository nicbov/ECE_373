package CommandPalette;
import java.util.regex.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Macro extends Command{
    private Command[] commandList = {};
    private Pattern variableID = Pattern.compile("\\w+:[number|string]");
    private Pattern variableDefine = Pattern.compile(":");
    private Pattern repeatedCommandParsing = Pattern.compile("_\\d+");
    private Pattern newline = Pattern.compile("\n");
    private Pattern number = Pattern.compile("\\d+");
    private Pattern string = Pattern.compile("\\w+");
    private LinkedHashMap<String, Pattern> variableUse;

    public String Serialize(){
        throw new UnsupportedOperationException();
    }

    public String GetFormat(){
        throw new UnsupportedOperationException();
    }

    public void ParseRecordData(String input){
        String[] lines = newline.split(input);
        String[] macroDefinition = this.SeparateString(lines[0]);
        name = macroDefinition[0];
        for(String i : macroDefinition){
            Matcher m = variableDefine.matcher(i);
            if(m.matches()){
                String[] argumentPotential = variableDefine.split(i);
                arguments.put(argumentPotential[0], argumentPotential[1]);
            }
        }
        commandList = new Command[lines.length];
        for(int j = 1; j < lines.length; j++){
            String[] command = SeparateString(lines[j]);
            String commandName = command[0];
            String commandContents = commandName;
            Map<String, String> commandArgs = Map.of();
            int argCount = 0;
            for(int k = 1; k < command.length; k++){
                argCount = k;
                commandContents += " " + command[k] + ",";
                if(arguments.containsKey(command[k])){
                    commandArgs.put(command[k], arguments.get(command[k]));
                }
                else{
                    Matcher m1 = number.matcher(command[k]);
                    Matcher m2 = string.matcher(command[k]);
                    if(m1.matches()){
                        commandArgs.put(command[k], "number");
                    }
                    else{
                        commandArgs.put(command[k], "string");
                    }
                }
            }
            Command c = new Command(argCount, commandName, commandContents, commandArgs);
            commandList[j - 1] = c;
        }
    }

    public String[][][] ParseMacro(String input){
        throw new UnsupportedOperationException();
    }

    /*
    @Override 
    public boolean Validate(){
        String ex = name;
        Set<String> keys = arguments.keySet();
        boolean selfValid;
        for(String i : keys){
            ex += " " + arguments.get(i) + ",";
        }
        ex = ex.substring(0, ex.length() - 1);
        if(contents == null){
            this.CompileCommand();
        }
        if(contents != null){
            Matcher m = contents.matcher(ex);
            selfValid = m.matches();
        }
        else{
            selfValid = false;
        }
        if(selfValid){
            for(String i : commandList){
                String index = repeatedCommandParsing.split(i)[0];
                String template = SystemDictionary.Get(index).GetUncompiledContents();
                Pattern p = variableUse.get(i);
                Matcher m = p.matcher(template);
                if(!m.matches()){
                    return false;
                }
        }
        return true;
        }
        else{
            return false;
        }
    }
        */

    public boolean RegisterVariable(String input){
        String[] parsed = variableDefine.split(input);
        if(arguments.get(parsed[0]) != null){
            return false;
        }
        arguments.put(parsed[0], parsed[1]);
        return true;
    }

    public void reset(){
        variableUse.clear();
        commandList = new Command[0];
    }
}
