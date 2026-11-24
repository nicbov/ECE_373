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
 * File format, one line per entry, oldest first:
 *   U<TAB>command<TAB>inverse   (edit that can be undone)
 *   R<TAB>command<TAB>inverse   (edit that can be redone, most recent undo last)
 *   C<TAB>n<TAB>name            (named checkpoint at version n, see below)
 *   C<TAB>n                     (legacy unnamed checkpoint; loads as "checkpoint-n")
 *
 * A version is the number of edits applied: version 0 is the state before any
 * edit and version n is the state after the n-th queued edit (undo and redo
 * edits together form the queue, so valid versions are 0..undo+redo).
 *
 * Checkpoints stay put while the user undoes and redoes. They are dropped only
 * when a new edit (or discardRedo) throws away the branch they belonged to.
 */
public class StackHistoryService {

    /** One edit: the command that was applied and the command that reverses it. */
    public record Edit(String command, String inverse) { }

    /** A named bookmark on a document version. */
    public record CheckPoint(String name, int version) { }

    private final Path historyPath;
    private final Deque<Edit> undoStack = new ArrayDeque<>();
    private final Deque<Edit> redoStack = new ArrayDeque<>();
    private final List<CheckPoint> checkPoints = new ArrayList<>();

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

    /** Deletes the history file if it exists. */
    public void delete() {
        try {
            Files.deleteIfExists(historyPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete history file: " + historyPath, e);
        }
    }

    /**
     * Records a newly applied edit. A new edit invalidates the redo stack, and
     * with it every checkpoint that pointed into the discarded redo branch.
     */
    public synchronized void record(Edit edit) {
        if (edit == null || edit.command() == null || edit.inverse() == null) {
            throw new IllegalArgumentException("An edit and its command snapshots are required.");
        }
        undoStack.push(edit);
        List<Edit> redoBefore = new ArrayList<>(redoStack);
        List<CheckPoint> checkPointsBefore = new ArrayList<>(checkPoints);
        redoStack.clear();
        checkPoints.removeIf(c -> c.version() >= undoStack.size());
        try {
            persist();
        } catch (RuntimeException e) {
            undoStack.pop();
            redoStack.addAll(redoBefore);
            checkPoints.clear();
            checkPoints.addAll(checkPointsBefore);
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
        try {
            persist();
        } catch (RuntimeException e) {
            redoStack.pop();
            undoStack.push(edit);
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

    /**
     * Throws away every undone edit (and the checkpoints that pointed at them),
     * making the current version the newest one.
     */
    public synchronized void discardRedo() {
        if (redoStack.isEmpty()) {
            return;
        }
        List<Edit> redoBefore = new ArrayList<>(redoStack);
        List<CheckPoint> checkPointsBefore = new ArrayList<>(checkPoints);
        redoStack.clear();
        checkPoints.removeIf(c -> c.version() > undoStack.size());
        try {
            persist();
        } catch (RuntimeException e) {
            redoStack.addAll(redoBefore);
            checkPoints.clear();
            checkPoints.addAll(checkPointsBefore);
            throw e;
        }
    }

    // ---- checkpoints --------------------------------------------------------

    /** Marks the current version as a checkpoint (legacy unnamed form). */
    public synchronized void addCheckPoint() {
        int version = getCurrentVersion();
        boolean exists = checkPoints.stream().anyMatch(c -> c.version() == version);
        if (!exists) {
            addCheckPoint(defaultCheckPointName(version), version);
        }
    }

    /** Adds a named checkpoint on the given version (0..undo+redo). Names are unique. */
    public synchronized CheckPoint addCheckPoint(String name, int version) {
        requireName(name);
        int total = undoStack.size() + redoStack.size();
        if (version < 0 || version > total) {
            throw new IllegalArgumentException("Version " + version + " is outside 0.." + total);
        }
        if (findCheckPoint(name).isPresent()) {
            throw new IllegalStateException("A checkpoint named '" + name + "' already exists.");
        }
        CheckPoint added = new CheckPoint(name, version);
        checkPoints.add(added);
        try {
            persist();
        } catch (RuntimeException e) {
            checkPoints.remove(added);
            throw e;
        }
        return added;
    }

    /** Renames a checkpoint, keeping its version. */
    public synchronized CheckPoint renameCheckPoint(String oldName, String newName) {
        requireName(newName);
        CheckPoint existing = findCheckPoint(oldName).orElseThrow(
                () -> new IllegalArgumentException("No checkpoint named '" + oldName + "'"));
        if (oldName.equals(newName)) {
            return existing;
        }
        if (findCheckPoint(newName).isPresent()) {
            throw new IllegalStateException("A checkpoint named '" + newName + "' already exists.");
        }
        int index = checkPoints.indexOf(existing);
        CheckPoint renamed = new CheckPoint(newName, existing.version());
        checkPoints.set(index, renamed);
        try {
            persist();
        } catch (RuntimeException e) {
            checkPoints.set(index, existing);
            throw e;
        }
        return renamed;
    }

    /** Removes the named checkpoint; returns false if there was none. */
    public synchronized boolean removeCheckPoint(String name) {
        Optional<CheckPoint> existing = findCheckPoint(name);
        if (existing.isEmpty()) {
            return false;
        }
        int index = checkPoints.indexOf(existing.get());
        checkPoints.remove(index);
        try {
            persist();
        } catch (RuntimeException e) {
            checkPoints.add(index, existing.get());
            throw e;
        }
        return true;
    }

    public synchronized Optional<CheckPoint> findCheckPoint(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return checkPoints.stream().filter(c -> c.name().equals(name)).findFirst();
    }

    /** Versions of all checkpoints (kept for older callers; see getNamedCheckPoints). */
    public synchronized List<Integer> getCheckPoints() {
        List<Integer> versions = new ArrayList<>();
        for (CheckPoint c : checkPoints) {
            versions.add(c.version());
        }
        return List.copyOf(versions);
    }

    /** All checkpoints in the order they were created. */
    public synchronized List<CheckPoint> getNamedCheckPoints() { return List.copyOf(checkPoints); }

    // ---- history views ---------------------------------------------------------

    /** All applied edits, oldest first. */
    public synchronized List<Edit> getVersionHistory() {
        List<Edit> list = new ArrayList<>(undoStack);
        java.util.Collections.reverse(list);
        return list;
    }

    /** All undone edits in the order redo would reapply them (next edit first). */
    public synchronized List<Edit> getRedoHistory() {
        return new ArrayList<>(redoStack);
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
                case "C" -> {
                    int version = Integer.parseInt(parts[1]);
                    String name = parts.length > 2
                            ? unescape(parts[2]) : defaultCheckPointName(version);
                    checkPoints.add(new CheckPoint(name, version));
                }
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
        for (CheckPoint c : checkPoints) {
            lines.add("C\t" + c.version() + "\t" + escape(c.name()));
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

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A checkpoint name is required.");
        }
    }

    private static String defaultCheckPointName(int version) {
        return "checkpoint-" + version;
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