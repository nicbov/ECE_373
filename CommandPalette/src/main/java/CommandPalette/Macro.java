package CommandPalette;
import java.util.regex.*;
import java.util.Map;
import java.util.Set;

public class Macro extends Command{
    private String[] commandList = {};
    private Pattern variableID = Pattern.compile("\\w+:\\w+");
    private Pattern variableDefine = Pattern.compile(":");
    private Pattern repeatedCommandParsing = Pattern.compile("_\\d+");
    private Map<String, Pattern> variableUse;

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

    public String[][][] ParseMacro(String input){
        throw new UnsupportedOperationException();
    }

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
        commandList = new String[0];
    }
}
