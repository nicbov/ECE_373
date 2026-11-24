package text_editor;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Metadata for one document. Each document is backed by two files:
 *   1. the document file  ({@code <name>.doc})     - the words plus formatting tags
 *   2. the history file   ({@code <name>.history}) - queue of every edit made
 */
public class Document {

    public static final String DOCUMENT_EXTENSION = ".doc";
    public static final String HISTORY_EXTENSION = ".history";

    private final String name;
    private final String pathDocument;
    private final String pathHistory;
    private final Instant createdAt;
    private Instant lastModified;

    private final DocumentFileService fileService;
    private final StackHistoryService historyService;
    private final SectionTree sectionTree; // exactly one per document, never replaced
    private String editGroupStart;

    /** Creates a brand new document and both of its files in the given folder. */
    public static Document create(String name, Path directory) {
        Document doc = new Document(name, directory);
        doc.fileService.create();
        doc.historyService.create();
        return doc;
    }

    /** Opens an existing document, loading its edit history from disk. */
    public static Document open(String name, Path directory) {
        Document doc = new Document(name, directory);
        if (!doc.fileService.exists()) {
            throw new IllegalArgumentException("No document named '" + name + "' in " + directory);
        }
        doc.sectionTree.replaceWith(DocumentMarkupCodec.decode(doc.fileService.load()));
        doc.historyService.create(); // no-op if the history file already exists
        doc.historyService.load();
        return doc;
    }

    private Document(String name, Path directory) {
        if (name == null || name.isBlank() || !name.matches("[\\w\\- ]+")) {
            throw new IllegalArgumentException("Invalid document name: " + name);
        }
        this.name = name;
        Path documentFile = directory.resolve(name + DOCUMENT_EXTENSION);
        Path historyFile = directory.resolve(name + HISTORY_EXTENSION);
        this.pathDocument = documentFile.toString();
        this.pathHistory = historyFile.toString();
        this.fileService = new DocumentFileService(documentFile);
        this.historyService = new StackHistoryService(historyFile);
        this.sectionTree = new SectionTree();
        this.createdAt = Instant.now();
        this.lastModified = this.createdAt;
    }

    public String getName() { return name; }

    public String getPathDocument() { return pathDocument; }

    public String getPathHistory() { return pathHistory; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastModified() { return lastModified; }

    public void touch() { lastModified = Instant.now(); }

    public DocumentFileService getFileService() { return fileService; }

    public StackHistoryService getHistoryService() { return historyService; }

    /** The document's one and only section tree. It always exists and cannot be replaced. */
    public SectionTree getSectionTree() { return sectionTree; }

    boolean isEditGroupActive() { return editGroupStart != null; }

    String getEditGroupStart() { return editGroupStart; }

    void beginEditGroup(String snapshot) {
        if (editGroupStart != null) {
            throw new IllegalStateException("An edit group is already active.");
        }
        editGroupStart = snapshot;
    }

    void clearEditGroup() { editGroupStart = null; }

    @Override
    public String toString() {
        return "Document{name='" + name + "', doc=" + pathDocument + ", history=" + pathHistory + "}";
    }
}