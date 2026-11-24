package text_editor;

import java.util.ArrayList;
import java.util.List;

/**
 * Safely walks raw document text and splits it into tokens.
 *
 * Rules:
 *   {@code "<<"} is a literal less-than, {@code "**"} is a literal star
 *   {@code <code*} opens a tag, {@code *code>} closes a tag
 * Tags must nest properly. Anything malformed throws IllegalArgumentException.
 */
public final class MarkupParser {

    private MarkupParser() { }

    public static List<Token> parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Raw text is required.");
        }
        List<Token> tokens = new ArrayList<>();
        List<TagType> open = new ArrayList<>();
        StringBuilder visible = new StringBuilder();
        int textStart = -1;
        int i = 0;
        int n = raw.length();

        while (i < n) {
            char c = raw.charAt(i);
            if (c == '<' && i + 1 < n && raw.charAt(i + 1) == '<') {
                if (visible.length() == 0) textStart = i;
                visible.append('<');
                i += 2;
            } else if (c == '*' && i + 1 < n && raw.charAt(i + 1) == '*') {
                if (visible.length() == 0) textStart = i;
                visible.append('*');
                i += 2;
            } else if (c == '<') {
                flushText(tokens, visible, textStart, i);
                int end = raw.indexOf('*', i + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Unterminated opening tag at " + i);
                }
                String body = raw.substring(i + 1, end);
                int colon = body.indexOf(':');
                String code = colon < 0 ? body : body.substring(0, colon);
                String arg = colon < 0 ? null : body.substring(colon + 1);
                TagType type = TagType.fromCode(code);
                if (type.takesArgument() != (arg != null) || (arg != null && arg.isBlank())) {
                    throw new IllegalArgumentException("Bad argument for tag '" + code + "' at " + i);
                }
                tokens.add(new Token(Token.Kind.OPEN, type, arg, i, end + 1, ""));
                open.add(type);
                i = end + 1;
            } else if (c == '*') {
                flushText(tokens, visible, textStart, i);
                int end = raw.indexOf('>', i + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Unterminated closing tag at " + i);
                }
                TagType type = TagType.fromCode(raw.substring(i + 1, end));
                if (open.isEmpty() || open.get(open.size() - 1) != type) {
                    throw new IllegalArgumentException(
                            "Closing tag " + type.closeText() + " at " + i + " does not match an open tag");
                }
                open.remove(open.size() - 1);
                tokens.add(new Token(Token.Kind.CLOSE, type, null, i, end + 1, ""));
                i = end + 1;
            } else {
                if (visible.length() == 0) textStart = i;
                visible.append(c);
                i++;
            }
        }
        flushText(tokens, visible, textStart, n);
        if (!open.isEmpty()) {
            throw new IllegalArgumentException("Tag never closed: " + open.get(open.size() - 1));
        }
        return tokens;
    }

    private static void flushText(List<Token> tokens, StringBuilder visible, int start, int end) {
        if (visible.length() > 0) {
            tokens.add(new Token(Token.Kind.TEXT, null, null, start, end, visible.toString()));
            visible.setLength(0);
        }
    }

    /** All text a reader sees, with every tag removed and escapes resolved. */
    public static String visibleText(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            sb.append(t.visible());
        }
        return sb.toString();
    }

    public static String visibleText(String raw) {
        return visibleText(parse(raw));
    }

    /** Locates every opening or closing tag of one type, with its raw position. */
    public static List<Token> findTags(List<Token> tokens, TagType type) {
        List<Token> found = new ArrayList<>();
        for (Token t : tokens) {
            if (t.kind() != Token.Kind.TEXT && t.tag() == type) {
                found.add(t);
            }
        }
        return found;
    }

    /** Makes plain text safe to store: {@code "<"} becomes {@code "<<"} and {@code "*"} becomes {@code "**"}. */
    public static String escape(String visible) {
        return visible.replace("<", "<<").replace("*", "**");
    }
}
