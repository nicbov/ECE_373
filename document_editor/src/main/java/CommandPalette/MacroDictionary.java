package CommandPalette;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the macros available to the command palette.
 */
public final class MacroDictionary {
    private static final Map<String, Macro> registeredMacros = new LinkedHashMap<>();

    private MacroDictionary() { }

    /**
     * Reports whether a macro is registered.
     *
     * @param name macro name
     * @return true when the macro exists
     */
    public static boolean Contains(String name) {
        return registeredMacros.containsKey(name);
    }

    /**
     * Returns a registered macro.
     *
     * @param name macro name
     * @return matching macro or null
     */
    public static Macro Get(String name) {
        return registeredMacros.get(name);
    }

    /**
     * Returns the invocation format for one macro.
     *
     * @param name macro name
     * @return macro format
     */
    public static String ListCommandFormat(String name) {
        Macro macro = Get(name);
        return macro == null ? "" : macro.GetFormat();
    }

    /**
     * Lists every registered macro format.
     *
     * @return one format per line
     */
    public static String ListAll() {
        StringBuilder formats = new StringBuilder();
        for (Macro macro : registeredMacros.values()) {
            if (formats.length() > 0) {
                formats.append('\n');
            }
            formats.append(macro.GetFormat());
        }
        return formats.toString();
    }

    /**
     * Loads all valid {@code ~begin} macro records from text.
     *
     * @param input serialized macro records
     * @throws IllegalArgumentException if a macro record is invalid
     */
    public static void LoadMacros(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        for (String record : input.split("~begin")) {
            if (record.isBlank()) {
                continue;
            }
            Macro macro = new Macro();
            macro.ParseRecordData(record);
            if (!RegisterMacro(macro)) {
                throw new IllegalArgumentException("Invalid macro record.");
            }
        }
    }

    /**
     * Registers a valid macro by name.
     *
     * @param macro macro to register
     * @return true when registration succeeds
     */
    public static boolean RegisterMacro(Macro macro) {
        if (macro != null && macro.Validate()) {
            registeredMacros.put(macro.GetName(), macro);
            return true;
        }
        return false;
    }

    /**
     * Removes one macro.
     *
     * @param name macro name
     * @return true when a macro was removed
     */
    public static boolean ClearMacro(String name) {
        return registeredMacros.remove(name) != null;
    }

    /**
     * Removes all registered macros.
     */
    public static void ClearAll() {
        registeredMacros.clear();
    }
}
