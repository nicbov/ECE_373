package text_editor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Singleton that tracks application-level metadata: where documents live,
 * which documents have been created/opened this session, and which one is
 * the current document.
 *
 * The engine does not modify document text itself. execute() records an edit
 * in the document's history file; undo() and redo() hand back the edit so the
 * caller (e.g. a TextEditService) can apply the command or its inverse.
 */
public final class DocumentEngine {

    private static DocumentEngine instance;

    private Path baseDirectory = Path.of("documents");
    private final Map<String, Document> documents = new LinkedHashMap<>();
    private Document document; // current document

    private DocumentEngine() { }

    public static synchronized DocumentEngine getInstance() {
        if (instance == null) {
            instance = new DocumentEngine();
        }
        return instance;
    }

    public Path getBaseDirectory() { return baseDirectory; }

    public void setBaseDirectory(Path baseDirectory) { this.baseDirectory = baseDirectory; }

    /** Creates a new document (document file + history file) and makes it current. */
    public Document createDocument(String name) {
        if (documents.containsKey(name)) {
            throw new IllegalStateException("A document named '" + name + "' is already open.");
        }
        Document created = Document.create(name, baseDirectory);
        documents.put(name, created);
        document = created;
        return created;
    }

    /** Opens an existing document from disk and makes it current. */
    public Document openDocument(String name) {
        Document opened = documents.get(name);
        if (opened == null) {
            opened = Document.open(name, baseDirectory);
            documents.put(name, opened);
        }
        document = opened;
        return opened;
    }

    public void switchDocument(String name) {
        Document target = documents.get(name);
        if (target == null) {
            throw new IllegalArgumentException("Document not open: " + name);
        }
        document = target;
    }

    public Document getDocument() {
        requireCurrent();
        return document;
    }

    public Map<String, Document> getOpenDocuments() {
        return Collections.unmodifiableMap(documents);
    }

    /** Records an edit (and how to reverse it) in the current document's history. */
    public void execute(String command, String inverse) {
        requireCurrent();
        document.getHistoryService().record(new StackHistoryService.Edit(command, inverse));
        document.touch();
    }

    /** Steps back one edit; the caller applies the returned edit's inverse. */
    public Optional<StackHistoryService.Edit> undo() {
        requireCurrent();
        Optional<StackHistoryService.Edit> edit = document.getHistoryService().undo();
        edit.ifPresent(e -> document.touch());
        return edit;
    }

    /** Steps forward one edit; the caller re-applies the returned edit's command. */
    public Optional<StackHistoryService.Edit> redo() {
        requireCurrent();
        Optional<StackHistoryService.Edit> edit = document.getHistoryService().redo();
        edit.ifPresent(e -> document.touch());
        return edit;
    }

    public void closeDocument(String name) {
        documents.remove(name);
        if (document != null && document.getName().equals(name)) {
            document = null;
        }
    }

    private void requireCurrent() {
        if (document == null) {
            throw new IllegalStateException("No current document. Call createDocument or openDocument first.");
        }
    }
}