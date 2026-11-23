package text_editor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    public synchronized void record(Edit edit) {
        if (edit == null || edit.command() == null || edit.inverse() == null) {
            throw new IllegalArgumentException("An edit and its command snapshots are required.");
        }
        undoStack.push(edit);
        List<Edit> redoBefore = new ArrayList<>(redoStack);
        redoStack.clear();
        try {
            persist();
        } catch (RuntimeException e) {
            undoStack.pop();
            redoStack.addAll(redoBefore);
            throw e;
        }
    }

    /** Pops the latest edit; the caller applies its inverse to the document. */
    public synchronized Optional<Edit> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        Edit edit = undoStack.pop();
        redoStack.push(edit);
        List<Integer> checkpointsBefore = new ArrayList<>(checkPoints);
        checkPoints.removeIf(c -> c > undoStack.size());
        try {
            persist();
        } catch (RuntimeException e) {
            redoStack.pop();
            undoStack.push(edit);
            checkPoints.clear();
            checkPoints.addAll(checkpointsBefore);
            throw e;
        }
        return Optional.of(edit);
    }

    public synchronized Optional<Edit> peekUndo() {
        return Optional.ofNullable(undoStack.peek());
    }

    /** Re-pops the latest undone edit; the caller re-applies its command. */
    public synchronized Optional<Edit> redo() {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        Edit edit = redoStack.pop();
        undoStack.push(edit);
        try {
            persist();
        } catch (RuntimeException e) {
            undoStack.pop();
            redoStack.push(edit);
            throw e;
        }
        return Optional.of(edit);
    }

    public synchronized Optional<Edit> peekRedo() {
        return Optional.ofNullable(redoStack.peek());
    }

    public synchronized boolean canUndo() { return !undoStack.isEmpty(); }

    public synchronized boolean canRedo() { return !redoStack.isEmpty(); }

    /** Number of edits currently applied (the current version number). */
    public synchronized int getCurrentVersion() { return undoStack.size(); }

    /** Marks the current version as a checkpoint. */
    public synchronized void addCheckPoint() {
        int version = getCurrentVersion();
        if (!checkPoints.contains(version)) {
            checkPoints.add(version);
            try {
                persist();
            } catch (RuntimeException e) {
                checkPoints.remove(Integer.valueOf(version));
                throw e;
            }
        }
    }

    public synchronized List<Integer> getCheckPoints() { return List.copyOf(checkPoints); }

    /** All applied edits, oldest first. */
    public synchronized List<Edit> getVersionHistory() {
        List<Edit> list = new ArrayList<>(undoStack);
        java.util.Collections.reverse(list);
        return list;
    }

    /** Loads stacks and checkpoints from the history file. */
    public synchronized void load() {
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
        Path absolutePath = historyPath.toAbsolutePath();
        Path parent = absolutePath.getParent();
        Path temporaryPath = null;
        try {
            String prefix = temporaryPrefix(historyPath);
            if (parent == null) {
                temporaryPath = Files.createTempFile(prefix, ".tmp");
            } else {
                Files.createDirectories(parent);
                temporaryPath = Files.createTempFile(parent, prefix, ".tmp");
            }
            Files.write(temporaryPath, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryPath, absolutePath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryPath, absolutePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (temporaryPath != null) {
                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw new UncheckedIOException("Could not write history file: " + historyPath, e);
        }
    }

    private static String temporaryPrefix(Path path) {
        String prefix = path.getFileName().toString();
        return prefix.length() >= 3 ? prefix : (prefix + "___").substring(0, 3);
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