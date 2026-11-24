package text_editor;

import java.nio.file.Path;
import java.util.Scanner;

/** Starts the interactive editor or runs a command script. */
public class Main {
    /** Runs the editor. */
    public static void main(String[] args) {
        EditorApplication application = new EditorApplication();

        // a file argument runs every line through the normal command path
        if (args.length > 0) {
            application.executeScript(Path.of(args[0]));
            return;
        }

        // without a file the editor reads commands until input is closed
        System.out.println("document editor ready type help to list commands");
        try (Scanner input = new Scanner(System.in)) {
            while (input.hasNextLine()) {
                application.executeLine(input.nextLine());
            }
        }
    }
}