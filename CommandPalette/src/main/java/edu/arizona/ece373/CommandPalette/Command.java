package CommandPalette.src.main.java.edu.arizona.ece373.CommandPalette;
import java.util.regex.*;

public class Command{
    private int index;
    private Pattern contents;
    private String uncompiledContents;
    private String name;
    private String arguments;

    public Command(int index, String name, String uncompiledContents, String arguments){
        this.index = index;
        this.name = name;
        this.uncompiledContents = uncompiledContents;
        this.arguments = arguments;
    }

    public Command(){
        this(-1, "", "", "");
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

    public String GetContents(){
        return uncompiledContents;
    }

    public String GetName(){
        return name;
    }

    public String GetArguments(){
        return arguments;
    }

}