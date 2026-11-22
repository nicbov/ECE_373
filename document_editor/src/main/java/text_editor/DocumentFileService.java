package text_editor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Handles reading and writing the document file: the file that holds the
 * actual words of the document plus its formatting tags.
 */
public class DocumentFileService {

    private final Path documentPath;

    public DocumentFileService(Path documentPath) {
        this.documentPath = documentPath;
    }

    public Path getDocumentPath() {
        return documentPath;
    }

    public boolean exists() {
        return Files.exists(documentPath);
    }

    /** Creates an empty document file (and parent folders) if it is missing. */
    public void create() {
        try {
            if (documentPath.getParent() != null) {
                Files.createDirectories(documentPath.getParent());
            }
            if (!Files.exists(documentPath)) {
                Files.createFile(documentPath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create document file: " + documentPath, e);
        }
    }

    /** Reads the whole document, including formatting tags, as raw text. */
    public String load() {
        try {
            return Files.readString(documentPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read document file: " + documentPath, e);
        }
    }

    /** Overwrites the document file with the given raw text. */
    public void save(String content) {
        try {
            Files.writeString(documentPath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write document file: " + documentPath, e);
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(documentPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete document file: " + documentPath, e);
        }
    }
}