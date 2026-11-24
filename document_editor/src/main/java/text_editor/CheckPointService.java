package text_editor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Named checkpoints on a document's edit history.
 *
 * A checkpoint bookmarks a version: version n is the document after the n-th
 * queued edit in the {@link StackHistoryService}. Names live in the history
 * service, so they persist with the history file and survive restarts.
 *
 * This class never reads or writes document or history files itself. Everything
 * goes through {@link Document}, {@link StackHistoryService} and
 * {@link TextEditService}:
 *   - restore moves the real document with TextEditService undo/redo, so the
 *     edit queue stays intact and the move can itself be undone or redone;
 *   - preview rebuilds the version inside a throw-away Document in a temp
 *     folder, so the real document and history are only read.
 */
public final class CheckPointService {

    private static final String PREVIEW_PREFIX = "preview-";

    private final Document document;
    private final TextEditService editService;

    public CheckPointService(Document document) {
        this(document, new TextEditService(requireDocument(document)));
    }

    CheckPointService(Document document, TextEditService editService) {
        this.document = requireDocument(document);
        if (editService == null) {
            throw new IllegalArgumentException("Text edit service is required.");
        }
        this.editService = editService;
    }

    // ---- creating ---------------------------------------------------------------

    /** Names the document as it is right now. At least one edit must have been made. */
    public StackHistoryService.CheckPoint createAtCurrentVersion(String name) {
        StackHistoryService history = history();
        synchronized (history) {
            int version = history.getCurrentVersion();
            if (version == 0) {
                throw new IllegalStateException("There are no edits to checkpoint yet.");
            }
            return history.addCheckPoint(cleanName(name), version);
        }
    }

    /**
     * Names the document as it is right after a queued edit. Edits are numbered
     * from 1 (oldest); undone edits that can still be redone are included.
     */
    public StackHistoryService.CheckPoint createAtEdit(String name, int editNumber) {
        StackHistoryService history = history();
        synchronized (history) {
            int total = totalVersions(history);
            if (editNumber < 1 || editNumber > total) {
                throw new IllegalArgumentException(
                        "Edit number " + editNumber + " is outside 1.." + total);
            }
            return history.addCheckPoint(cleanName(name), editNumber);
        }
    }

    // ---- managing ---------------------------------------------------------------

    public StackHistoryService.CheckPoint rename(String name, String newName) {
        return history().renameCheckPoint(cleanName(name), cleanName(newName));
    }

    /** Removes the checkpoint only (the document and its edits are untouched). */
    public boolean delete(String name) {
        return history().removeCheckPoint(cleanName(name));
    }

    public Optional<StackHistoryService.CheckPoint> get(String name) {
        return history().findCheckPoint(cleanName(name));
    }

    /** All checkpoints ordered by version, then by creation order. */
    public List<StackHistoryService.CheckPoint> list() {
        List<StackHistoryService.CheckPoint> all = new ArrayList<>(history().getNamedCheckPoints());
        all.sort(Comparator.comparingInt(StackHistoryService.CheckPoint::version));
        return all;
    }

    /** Checkpoints sitting exactly on the document's current version. */
    public List<StackHistoryService.CheckPoint> getPresentCheckPoints() {
        StackHistoryService history = history();
        synchronized (history) {
            int current = history.getCurrentVersion();
            return list().stream().filter(c -> c.version() == current).toList();
        }
    }

    public boolean isPresent(String name) {
        return getPresentCheckPoints().stream().anyMatch(c -> c.name().equals(cleanName(name)));
    }

    // ---- restoring --------------------------------------------------------------

    /**
     * Moves the document to the checkpoint's version using undo/redo. Later edits
     * stay in the queue and can be redone; every checkpoint is kept.
     */
    public void restore(String name) {
        StackHistoryService history = history();
        synchronized (history) {
            moveTo(history, require(history, name).version());
        }
    }

    /**
     * Restores the checkpoint and makes it the present version for good: edits
     * after it are discarded from the queue (so cannot be redone) along with the
     * checkpoints that pointed at them.
     */
    public void setAsPresent(String name) {
        StackHistoryService history = history();
        synchronized (history) {
            moveTo(history, require(history, name).version());
            history.discardRedo();
        }
    }

    // ---- previewing -------------------------------------------------------------

    /**
     * Rebuilds the checkpoint's version in a temporary copy of the document and
     * returns it read-only. The real document, its file and its history are not
     * changed. Close the preview to delete the temporary files.
     */
    public Preview preview(String name) {
        StackHistoryService history = history();
        StackHistoryService.CheckPoint checkPoint;
        String raw;
        List<StackHistoryService.Edit> steps;
        int target;
        int current;

        synchronized (history) {
            checkPoint = require(history, name);
            target = checkPoint.version();
            current = history.getCurrentVersion();
            if (target > totalVersions(history)) {
                throw new IllegalStateException("Checkpoint is outside the edit history.");
            }
            raw = document.getFileService().load();
            if (target < current) {
                steps = new ArrayList<>(history.getVersionHistory().subList(target, current));
            } else if (target > current) {
                steps = new ArrayList<>(history.getRedoHistory().subList(0, target - current));
            } else {
                steps = List.of();
            }
        }

        Path directory = createTemporaryDirectory();
        Document copy = null;
        try {
            copy = Document.create(PREVIEW_PREFIX + UUID.randomUUID(), directory);
            copy.getFileService().save(raw);
            copy.getSectionTree().replaceWith(DocumentMarkupCodec.decode(raw));
            for (StackHistoryService.Edit edit : steps) {
                copy.getHistoryService().record(edit);
            }

            TextEditService copyEditor = new TextEditService(copy);
            if (target < current) {
                for (int i = 0; i < steps.size(); i++) {
                    requireStep(copyEditor.undo());
                }
            } else if (target > current) {
                // Move the copied edits onto the redo stack without touching the text.
                for (int i = 0; i < steps.size(); i++) {
                    copy.getHistoryService().undo();
                }
                for (int i = 0; i < steps.size(); i++) {
                    requireStep(copyEditor.redo());
                }
            }
            return new Preview(checkPoint, copy, directory);
        } catch (RuntimeException failure) {
            discardQuietly(copy, directory, failure);
            throw failure;
        }
    }

    /** A read-only view of a document at a checkpoint, backed by temporary files. */
    public static final class Preview implements AutoCloseable {

        private final StackHistoryService.CheckPoint checkPoint;
        private final Document copy;
        private final Path directory;
        private boolean closed;

        private Preview(StackHistoryService.CheckPoint checkPoint, Document copy, Path directory) {
            this.checkPoint = checkPoint;
            this.copy = copy;
            this.directory = directory;
        }

        public StackHistoryService.CheckPoint getCheckPoint() { return checkPoint; }

        /** The raw tagged text of the previewed version. */
        public String getRawText() { return DocumentMarkupCodec.encode(copy.getSectionTree()); }

        /** The previewed structure. Treat it as read-only. */
        public SectionTree getSectionTree() { return copy.getSectionTree(); }

        public SectionTreeReadView readView() { return new SectionTreeReadView(copy.getSectionTree()); }

        public Path getTemporaryDirectory() { return directory; }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            copy.getFileService().delete();
            copy.getHistoryService().delete();
            try {
                Files.deleteIfExists(directory);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not delete preview folder: " + directory, e);
            }
            closed = true;
        }
    }

    // ---- helpers ----------------------------------------------------------------

    private void moveTo(StackHistoryService history, int target) {
        if (target > totalVersions(history)) {
            throw new IllegalStateException("Checkpoint is outside the edit history.");
        }
        while (history.getCurrentVersion() > target) {
            requireStep(editService.undo());
        }
        while (history.getCurrentVersion() < target) {
            requireStep(editService.redo());
        }
    }

    private static void requireStep(boolean moved) {
        if (!moved) {
            throw new IllegalStateException("Edit history ended before the checkpoint was reached.");
        }
    }

    private static int totalVersions(StackHistoryService history) {
        return history.getCurrentVersion() + history.getRedoHistory().size();
    }

    private StackHistoryService.CheckPoint require(StackHistoryService history, String name) {
        return history.findCheckPoint(cleanName(name)).orElseThrow(
                () -> new IllegalArgumentException("No checkpoint named '" + name + "'"));
    }

    private StackHistoryService history() {
        return document.getHistoryService();
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A checkpoint name is required.");
        }
        return name.trim();
    }

    private static Document requireDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document is required.");
        }
        return document;
    }

    private static Path createTemporaryDirectory() {
        try {
            return Files.createTempDirectory("checkpoint-preview-");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create preview folder.", e);
        }
    }

    private static void discardQuietly(Document copy, Path directory, RuntimeException failure) {
        try {
            if (copy != null) {
                copy.getFileService().delete();
                copy.getHistoryService().delete();
            }
            Files.deleteIfExists(directory);
        } catch (IOException | RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}