package text_editor;

/**
 * One piece of a raw document string: a run of visible text, an opening tag,
 * or a closing tag. rawStart is inclusive and rawEnd is exclusive, so the
 * token's exact characters are raw.substring(rawStart, rawEnd).
 *
 * For TEXT tokens, {@code visible} is the unescaped text ("<<" is "<",
 * "**" is "*"). Tag tokens have an empty visible string.
 */
public record Token(Kind kind, TagType tag, String argument, int rawStart, int rawEnd, String visible) {

    public enum Kind { TEXT, OPEN, CLOSE }
}