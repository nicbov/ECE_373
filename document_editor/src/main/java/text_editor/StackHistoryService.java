package text_editor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Tracks every edit made to a document (the "queue file") and provides
 * undo/redo through two stacks. The stacks are persisted to the history
 * file after every change, so history survives restarts.
 *
 * File format, one line per edit, oldest first:
 *   U<TAB>command<TAB>inverse   (edit that can be undone)
 *   R<TAB>command<TAB>inverse   (edit that can be redone, most recent undo last)
 *   C<TAB>n                     (checkpoint at undo-stack depth n)
 */
public class StackHistoryService {

    /** One edit: the command that was applied and the command that reverses it. */
    public record Edit(String command, String inverse) { }

    private final Path historyPath;
    private final Deque<Edit> undoStack = new ArrayDeque<>();
    private final Deque<Edit> redoStack = new ArrayDeque<>();
    private final List<Integer> checkPoints = new ArrayList<>();

    public StackHistoryService(Path historyPath) {
        this.historyPath = historyPath;
    }

    public Path getHistoryPath() {
        return historyPath;
    }

    /** Creates an empty history file (and parent folders) if it is missing. */
    public void create() {
        try {
            if (historyPath.getParent() != null) {
                Files.createDirectories(historyPath.getParent());
            }
            if (!Files.exists(historyPath)) {
                Files.createFile(historyPath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create history file: " + historyPath, e);
        }
    }

    /** Records a newly applied edit. A new edit invalidates the redo stack. */
    public void record(Edit edit) {
        undoStack.push(edit);
        redoStack.clear();
        persist();
    }

    /** Pops the latest edit; the caller applies its inverse to the document. */
    public Optional<Edit> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        Edit edit = undoStack.pop();
        redoStack.push(edit);
        checkPoints.removeIf(c -> c > undoStack.size());
        persist();
        return Optional.of(edit);
    }

    /** Re-pops the latest undone edit; the caller re-applies its command. */
    public Optional<Edit> redo() {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        Edit edit = redoStack.pop();
        undoStack.push(edit);
        persist();
        return Optional.of(edit);
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    public boolean canRedo() { return !redoStack.isEmpty(); }

    /** Number of edits currently applied (the current version number). */
    public int getCurrentVersion() { return undoStack.size(); }

    /** Marks the current version as a checkpoint. */
    public void addCheckPoint() {
        int version = getCurrentVersion();
        if (!checkPoints.contains(version)) {
            checkPoints.add(version);
            persist();
        }
    }

    public List<Integer> getCheckPoints() { return List.copyOf(checkPoints); }

    /** All applied edits, oldest first. */
    public List<Edit> getVersionHistory() {
        List<Edit> list = new ArrayList<>(undoStack);
        java.util.Collections.reverse(list);
        return list;
    }

    /** Loads stacks and checkpoints from the history file. */
    public void load() {
        undoStack.clear();
        redoStack.clear();
        checkPoints.clear();
        List<String> lines;
        try {
            lines = Files.readAllLines(historyPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read history file: " + historyPath, e);
        }
        List<Edit> redoOldestFirst = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split("\t", -1);
            if (parts.length < 2) continue;
            switch (parts[0]) {
                case "U" -> undoStack.push(new Edit(unescape(parts[1]), unescape(parts[2])));
                case "R" -> redoOldestFirst.add(new Edit(unescape(parts[1]), unescape(parts[2])));
                case "C" -> checkPoints.add(Integer.parseInt(parts[1]));
                default -> { }
            }
        }
        // Redo lines are stored with the next-to-redo edit last.
        for (Edit e : redoOldestFirst) {
            redoStack.push(e);
        }
    }

    private void persist() {
        List<String> lines = new ArrayList<>();
        for (Edit e : getVersionHistory()) {
            lines.add("U\t" + escape(e.command()) + "\t" + escape(e.inverse()));
        }
        List<Edit> redoList = new ArrayList<>(redoStack);
        java.util.Collections.reverse(redoList);
        for (Edit e : redoList) {
            lines.add("R\t" + escape(e.command()) + "\t" + escape(e.inverse()));
        }
        for (int c : checkPoints) {
            lines.add("C\t" + c);
        }
        try {
            Files.write(historyPath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write history file: " + historyPath, e);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 't' -> sb.append('\t');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}