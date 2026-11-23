package presentation;

import java.util.ArrayList;
import java.util.List;

/** Minimizes the total squared unused width across all lines. */
public final class BalancedLineBreaker implements LineBreaker {
    @Override
    public List<String> wrap(List<String> words, int width) {
        words = List.copyOf(words);
        LineBreaker.validate(words, width);
        String singleLine = String.join(" ", words);
        if (singleLine.length() <= width) {
            return words.isEmpty() ? List.of() : List.of(singleLine);
        }

        int count = words.size();

        // cost[start] is the cheapest layout for the remaining words, including the final line.
        double[] cost = new double[count + 1];
        int[] next = new int[count];
        for (int start = count - 1; start >= 0; start--) {
            cost[start] = Double.POSITIVE_INFINITY;
            long length = 0;
            // Try every fitting first line and reuse the optimal cost of its suffix.
            for (int end = start; end < count; end++) {
                length += words.get(end).length() + (end == start ? 0L : 1L);
                if (length > width) {
                    break;
                }
                double unused = width - length;
                double candidate = unused * unused + cost[end + 1];

                // On ties, prefer the line with more words.
                if (candidate <= cost[start]) {
                    cost[start] = candidate;
                    next[start] = end + 1;
                }
            }
        }

        // Reconstruct the output by following the chosen break positions.
        List<String> lines = new ArrayList<>();
        for (int start = 0; start < count; start = next[start]) {
            lines.add(String.join(" ", words.subList(start, next[start])));
        }
        return lines;
    }
}

