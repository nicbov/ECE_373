package presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/** Builds words across formatting run boundaries */
final class WordTokens {
    private static final Pattern PART = Pattern.compile("\\S+|\\s+");

    private WordTokens() {}

    static List<String> words(DocumentReadView.Block block,
                              BiFunction<DocumentReadView.TextRun, String, String> format) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();

        // Keep the current word open across adjacent style runs
        for (var run : block.runs()) {
            var matcher = PART.matcher(run.text());
            while (matcher.find()) {
                String part = matcher.group();
                if (Character.isWhitespace(part.charAt(0))) {
                    if (!word.isEmpty()) {
                        words.add(word.toString());
                        word.setLength(0);
                    }
                } else {
                    word.append(format.apply(run, part));
                }
            }
        }

        if (!word.isEmpty()) {
            words.add(word.toString());
        }
        return words;
    }
}