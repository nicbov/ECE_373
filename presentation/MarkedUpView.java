package presentation;

import java.util.List;

/** Adds visible markers to styled word fragments. */
public final class MarkedUpView implements RenderingView {
    @Override
    public List<String> words(DocumentReadView.Block block) {
        return WordTokens.words(block, MarkedUpView::mark);
    }

    private static String mark(DocumentReadView.TextRun run, String text) {
        // Build inner tags first so opening and closing markers nest correctly.
        if (run.color() != null) {
            text = tag("C", run.color(), text);
        }
        if (run.size() != null) {
            text = tag("S", run.size().toString(), text);
        }
        if (run.font() != null) {
            text = tag("F", run.font(), text);
        }
        if (run.italic()) {
            text = tag("I", null, text);
        }
        if (run.bold()) {
            text = tag("B", null, text);
        }
        return text;
    }

    private static String tag(String code, String value, String text) {
        String opening = value == null ? code : code + ":" + value;
        return "<" + opening + "*" + text + "*" + code + ">";
    }
}
