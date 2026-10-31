package kienzle.desktop.pdf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Stores contract documents below {@code <dataDir>/documents/<contractId>/}. */
public class PdfStorage {
    private final Path dataDir;

    public PdfStorage(Path dataDir) {
        this.dataDir = dataDir;
    }

    /** @return the path relative to the data directory */
    public String store(UUID contractId, String fileName, byte[] content) {
        Path relative = Path.of("documents", contractId.toString(), fileName);
        try {
            Path target = dataDir.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return relative.toString();
    }

    public Path resolve(String relativePath) {
        return dataDir.resolve(relativePath);
    }
}
