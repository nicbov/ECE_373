package text_editor;

/**
 * Every tag the document format knows about.
 *
 * Written as an opening tag {@code <code*} or {@code <code:argument*}
 * and closed with {@code *code>}.
 * Formatting tags apply to text; structure tags mark sections and paragraphs.
 */
public enum TagType {
    // declaration order is the canonical nesting order for formatting tags
    BOLD("B", false, true),
    ITALIC("I", false, true),
    FONT("F", true, true),
    SIZE("S", true, true),
    COLOR("C", true, true),
    // NOTE: "S" is already taken by SIZE, so SECTION uses "SEC" here.
    // Change this one string if you pick a different code.
    SECTION("SEC", true, false),
    PARAGRAPH("P", true, false);

    private final String code;
    private final boolean takesArgument;
    private final boolean formatting;

    TagType(String code, boolean takesArgument, boolean formatting) {
        this.code = code;
        this.takesArgument = takesArgument;
        this.formatting = formatting;
    }

    public String code() { return code; }

    public boolean takesArgument() { return takesArgument; }

    public boolean isFormatting() { return formatting; }

    public static TagType fromCode(String code) {
        for (TagType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown tag code: '" + code + "'");
    }

    /** Text of the opening tag, e.g. {@code <B*} or {@code <F:Arial*}. */
    public String openText(String argument) {
        return "<" + code + (takesArgument ? ":" + argument : "") + "*";
    }

    /** Text of the closing tag, e.g. "*B>". */
    public String closeText() {
        return "*" + code + ">";
    }
}