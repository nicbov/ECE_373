package CommandPalette;

public class CommandPalette {

    public static void Print(String s){
        System.out.println(s);
    }

    public static void LoadMacros(String s){
        MacroDictionary.LoadMacros(s);
    }

    public static String[][] SendData(String[][] s){
        throw new UnsupportedOperationException();
    }

    public static void ExportCommand(String input, String name){
        Command c = SystemDictionary.Get(name);
        SendData(c.Parse(input));
    }

    public static void ReadInput(String input){
        Command parser = new Command();
        String name = parser.SeparateString(input)[0];
        if(SystemDictionary.Get(name) != null){
            ExportCommand(input, name);
        }
        else if(MacroDictionary.Get(name) != null){
            Macro m = MacroDictionary.Get(name);
            for(String[][] i : m.ParseMacro(input)){
                SendData(i);
            }
        }
    }
}