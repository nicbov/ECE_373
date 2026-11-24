package CommandPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import text_editor.EditorApplication;

class MacroTest {

    @BeforeEach
    void resetDictionaries() {
        MacroDictionary.ClearAll();
        new EditorApplication();
    }

    @Test
    void loadsSerializesAndExpandsOrderedArguments() {
        MacroDictionary.LoadMacros(String.join("\n",
                "~begin",
                "append target:string, position:number, text:string",
                "insert target, position, text"));

        Macro macro = MacroDictionary.Get("append");

        assertTrue(macro.Validate());
        assertEquals(
                "append target:string, position:number, text:string",
                macro.GetFormat());
        assertEquals(
                "insert",
                macro.ParseMacro("append 1#1, 5, hello")[0][0][0]);
        assertEquals(
                "1#1",
                macro.ParseMacro("append 1#1, 5, hello")[0][0][1]);
        assertTrue(macro.Serialize().startsWith("~begin\nappend"));
    }

    @Test
    void rejectsWrongMacroArgumentTypes() {
        MacroDictionary.LoadMacros(String.join("\n",
                "~begin",
                "append position:number",
                "insert 1#1, position, hello"));

        assertThrows(
                IllegalArgumentException.class,
                () -> MacroDictionary.Get("append").ParseMacro("append wrong"));
    }
}
