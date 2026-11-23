package text_editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Adds and removes formatting tags over a range of visible text.
 *
 * Ranges use visible-text positions (start inclusive, end exclusive), the same
 * kind of positions SearchService reports. The editor never touches section or
 * paragraph tags. It rebuilds formatting runs, so the result is always validly
 * nested, and formatting tags in the stored text may be rewritten in canonical
 * order (B, I, F, S, C) with equal neighbouring runs merged.
 */
public final class FormatEditor {

    private record Frame(TagType type, String previous) { }

    /** Wraps [start, end) in the tag. Font, size and color replace any existing value. */
    public String addFormat(String raw, int start, int end, TagType type, String argument) {
        requireFormatting(type);
        if (type.takesArgument() && (argument == null || argument.isBlank() || argument.contains("*"))) {
            throw new IllegalArgumentException("Tag " + type + " needs an argument without '*'.");
        }
        String arg = type.takesArgument() ? argument : "";
        return restyle(raw, start, end, style -> style.put(type, arg));
    }

    /** Removes the tag's effect from [start, end), splitting wider tags as needed. */
    public String removeFormat(String raw, int start, int end, TagType type) {
        requireFormatting(type);
        return restyle(raw, start, end, style -> style.remove(type));
    }

    private String restyle(String raw, int start, int end, Consumer<Map<TagType, String>> change) {
        java.util.List<Token> tokens = MarkupParser.parse(raw);
        int visibleLength = MarkupParser.visibleText(tokens).length();
        if (start < 0 || end > visibleLength || start >= end) {
            throw new IllegalArgumentException("Range [" + start + "," + end + ") is outside 0.."
                    + visibleLength);
        }

        StringBuilder out = new StringBuilder();
        Map<TagType, String> active = new EnumMap<>(TagType.class);
        Deque<Frame> frames = new ArrayDeque<>();
        Run run = new Run(out);
        int visPos = 0;

        for (Token t : tokens) {
            switch (t.kind()) {
                case OPEN -> {
                    if (t.tag().isFormatting()) {
                        frames.push(new Frame(t.tag(), active.get(t.tag())));
                        active.put(t.tag(), t.argument() == null ? "" : t.argument());
                    } else {
                        run.flush();
                        out.append(rawOf(raw, t));
                    }
                }
                case CLOSE -> {
                    if (t.tag().isFormatting()) {
                        Frame f = frames.pop();
                        if (f.previous() == null) active.remove(f.type());
                        else active.put(f.type(), f.previous());
                    } else {
                        run.flush();
                        out.append(rawOf(raw, t));
                    }
                }
                case TEXT -> {
                    String text = t.visible();
                    int from = 0;
                    // split the token into outside / inside / outside pieces
                    int a = Math.max(0, Math.min(text.length(), start - visPos));
                    int b = Math.max(0, Math.min(text.length(), end - visPos));
                    from = emit(run, text, from, a, active, null);
                    from = emit(run, text, from, b, active, change);
                    emit(run, text, from, text.length(), active, null);
                    visPos += text.length();
                }
            }
        }
        run.flush();
        return out.toString();
    }

    private static int emit(Run run, String text, int from, int to,
                            Map<TagType, String> active, Consumer<Map<TagType, String>> change) {
        if (to > from) {
            Map<TagType, String> style = new EnumMap<>(TagType.class);
            style.putAll(active);
            if (change != null) change.accept(style);
            run.add(text.substring(from, to), style);
        }
        return Math.max(from, to);
    }

    private static String rawOf(String raw, Token t) {
        return raw.substring(t.rawStart(), t.rawEnd());
    }

    private static void requireFormatting(TagType type) {
        if (type == null || !type.isFormatting()) {
            throw new IllegalArgumentException("Only formatting tags can be added or removed: " + type);
        }
    }

    /** Buffers consecutive text that shares one style, then writes it wrapped in tags. */
    private static final class Run {
        private final StringBuilder out;
        private Map<TagType, String> style;
        private final StringBuilder text = new StringBuilder();

        Run(StringBuilder out) { this.out = out; }

        void add(String piece, Map<TagType, String> pieceStyle) {
            if (style != null && !style.equals(pieceStyle)) {
                flush();
            }
            style = pieceStyle;
            text.append(piece);
        }

        void flush() {
            if (style == null || text.length() == 0) {
                style = null;
                text.setLength(0);
                return;
            }
            for (Map.Entry<TagType, String> e : style.entrySet()) {
                out.append(e.getKey().openText(e.getValue()));
            }
            out.append(MarkupParser.escape(text.toString()));
            TagType[] types = style.keySet().toArray(new TagType[0]);
            for (int i = types.length - 1; i >= 0; i--) {
                out.append(types[i].closeText());
            }
            style = null;
            text.setLength(0);
        }
    }
}