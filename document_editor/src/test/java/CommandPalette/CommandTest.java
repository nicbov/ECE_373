package CommandPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import java.util.Set;

class CommandTest {
    
    @Test
    void DefaultCommandConstruction(){
        Command test = new Command();
        Set<String> expected = Set.of("");
        assertEquals(-1, test.GetArgCount());
        assertEquals("", test.GetName());
        assertEquals("", test.GetUncompiledContents());
        assertEquals(expected, test.GetArguments());
    }

    @Test
    void CommandConstruction(){
        Command test = new Command(1, "Add", "Add \\d+, \\S+",
                new String[][] {{"location", "number"}, {"text", "string"}});
        Set<String> expected = Set.of("location", "text");
        assertEquals(1, test.GetArgCount());
        assertEquals("Add", test.GetName());
        assertEquals("Add \\d+, \\S+", test.GetUncompiledContents());
        assertEquals(expected, test.GetArguments());
    }

    @Test
    void CommandValidates(){
        Command test = new Command(1, "Add", "Add \\d+, \\S+",
                new String[][] {{"location", "\\d+"}, {"text", "\\S+"}});
        test.CompileCommand();
        assertEquals(true, test.Validate());
    }

    @Test
    void CommandCheckValidTrue(){
        Command test = new Command(1, "Add", "Add \\d+, \\S+",
                new String[][] {{"location", "number"}, {"text", "string"}});
        test.CompileCommand();
        assertEquals(true, test.CheckValid("Add 5, hello"));
    }

    @Test
    void CommandCheckValidFalse(){
        Command test = new Command(1, "Add", "Add \\d+, \\S+",
                new String[][] {{"location", "number"}, {"text", "string"}});
        test.CompileCommand();
        assertEquals(false, test.CheckValid("Add apple, hello"));
    }

    @Test
    void CommandSeparateStringTrue(){
        Command test = new Command(1, "Add", "Add \\d+, \\S+",
                new String[][] {{"location", "number"}, {"text", "string"}});
        test.CompileCommand();
        String[] expected = {"Add", "5", "hello"};
        String[] actual = test.SeparateString("Add 5, hello");
        assertEquals(true, expected[0].equals(actual[0]));
        assertEquals(true, expected[1].equals(actual[1]));
        assertEquals(true, expected[2].equals(actual[2]));
    }

    @Test
    void CommandParseStringTrue(){
        Command test = new Command(2, "Add", "Add \\d+, \\S+",
                new String[][] {{"location", "number"}, {"text", "string"}});
        test.CompileCommand();
        String[][] expected = new String[2][3];
        expected[0][0] = "Add";
        expected[0][1] = "5";
        expected[0][2] = "hello";
        expected[1][1] = "\\d+";
        expected[1][2] = "\\S+";
        String[][] actual = test.Parse("Add 5, hello");
        System.out.println(expected[0][0] + " " + actual[0][0]);        
        assertEquals(true, expected[0][0].equals(actual[0][0]));
        System.out.println(expected[0][1] + " " + actual[0][1]);
        assertEquals(true, expected[0][1].equals(actual[0][1]));
        System.out.println(expected[0][2] + " " + actual[0][2]);
        assertEquals(true, expected[0][2].equals(actual[0][2]));
        System.out.println(expected[1][1] + " " + actual[1][1]);
        assertEquals(true, expected[1][1].equals(actual[1][1]));
        System.out.println(expected[1][2] + " " + actual[1][2]);
        assertEquals(true, expected[1][2].equals(actual[1][2]));
    }

    @Test
    void CommandParseStringTwo(){
        Command test = new Command(4, "Test", "Test \\d+, \\S+, \\S+, \\S+",
                new String[][] {
                    {"location", "number"},
                    {"text", "string"},
                    {"test2", "string"},
                    {"test3", "string"}
                });
        test.CompileCommand();
        String[][] expected = new String[2][5];
        expected[0][0] = "Test";
        expected[0][1] = "5";
        expected[0][2] = "hello";
        expected[0][3] = "HI";
        expected[0][4] = "hi";
        expected[1][1] = "\\d+";
        expected[1][2] = "\\S+";
        expected[1][3] = "\\S+";
        expected[1][4] = "\\S+";
        String[][] actual = test.Parse("Test 5, hello, HI, hi");
        System.out.println(expected[0][0] + " " + actual[0][0]);        
        assertEquals(true, expected[0][0].equals(actual[0][0]));
        System.out.println(expected[0][1] + " " + actual[0][1]);
        assertEquals(true, expected[0][1].equals(actual[0][1]));
        System.out.println(expected[0][2] + " " + actual[0][2]);
        assertEquals(true, expected[0][2].equals(actual[0][2]));
        System.out.println(expected[0][3] + " " + actual[0][3]);
        assertEquals(true, expected[0][3].equals(actual[0][3]));
        System.out.println(expected[0][4] + " " + actual[0][4]);
        assertEquals(true, expected[0][4].equals(actual[0][4]));
        System.out.println(expected[1][1] + " " + actual[1][1]);
        assertEquals(true, expected[1][1].equals(actual[1][1]));
        System.out.println(expected[1][2] + " " + actual[1][2]);
        assertEquals(true, expected[1][2].equals(actual[1][2]));
        System.out.println(expected[1][2] + " " + actual[1][3]);
        assertEquals(true, expected[1][2].equals(actual[1][3]));
        System.out.println(expected[1][2] + " " + actual[1][4]);
        assertEquals(true, expected[1][2].equals(actual[1][4]));

    }
}
