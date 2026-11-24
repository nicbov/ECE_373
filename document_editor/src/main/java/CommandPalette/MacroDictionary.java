package CommandPalette;

import java.util.Map;
import java.util.regex.*;

public class MacroDictionary{
    private static Map<String, Macro> registeredMacros;
    private static Pattern macroStart = Pattern.compile("~begin");
    private static Pattern macroName = Pattern.compile("Name:");
    private static Pattern macroCommand = Pattern.compile(":");

    public static boolean Contains(String name){
        return registeredMacros.containsKey(name);
    }

    public static Macro Get(String name){
        return registeredMacros.get(name);
    }

    public static String ListCommandFormat(String name){
        String returnVal = "";
        returnVal += Get(name).GetFormat();
        return returnVal;
    }

    public static String ListAll(){
        String returnVal = "";
        for(Macro i : registeredMacros.values()){
            returnVal += i.GetFormat() + '\n';
        }
        return returnVal;
    }

    public static void LoadMacros(String s){
        String[] toLoad = macroStart.split(s);
        for(String i : toLoad){
            Macro m = new Macro();
            m.ParseRecordData(i);
        }
    }

    public static boolean RegisterMacro(Macro m){
        if(m.Validate()){
            registeredMacros.put(m.name, m);
            return true;
        }
        else{
            return false;
        }
    }

    public static boolean ClearMacro(String name){
        registeredMacros.remove(name);
        if(registeredMacros.get(name) != null){
            return false;
        }
        else{
            return true;
        }
    }

    public static void ClearAll(){
        registeredMacros.clear();
    }
}
