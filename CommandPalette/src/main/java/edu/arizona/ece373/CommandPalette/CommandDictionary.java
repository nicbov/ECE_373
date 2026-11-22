package edu.arizona.ece373.CommandPalette;

public abstract class CommandDictionary {
    public abstract String ListCommandFormat(String name);
    public abstract String ListAll();
    public abstract boolean Contains(String name);
    public abstract Command Get(String name);
}
