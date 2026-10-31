package kienzle.desktop.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Plans where a contract's documents go when it is archived or restored, and validates their paths. */
final class DocumentRelocator {

    /** Strategy: which folders are involved and how a file is renamed on the way. */
    enum Direction {
        ARCHIVE {
            @Override
            Path folder(Path dataDir, UUID contractId, boolean source) {
                return source ? activeFolder(dataDir, contractId) : archiveFolder(dataDir, contractId);
            }

            @Override
            String targetName(ContractDocument document, String fileName) {
                return document.id() + "-" + fileName;
            }
        },
        RESTORE {
            @Override
            Path folder(Path dataDir, UUID contractId, boolean source) {
                return source ? archiveFolder(dataDir, contractId) : activeFolder(dataDir, contractId);
            }

            @Override
            String targetName(ContractDocument document, String fileName) {
                String prefix = document.id() + "-";
                if (!fileName.startsWith(prefix) || fileName.length() == prefix.length()) {
                    throw new IllegalArgumentException("Archived document has an invalid name: " + fileName);
                }
                return fileName.substring(prefix.length());
            }
        };

        abstract Path folder(Path dataDir, UUID contractId, boolean source);

        abstract String targetName(ContractDocument document, String fileName);
    }

    private final Path dataDir;

    DocumentRelocator(Path dataDir) {
        this.dataDir = dataDir;
    }

    /** Registers the file moves in {@code moves} and returns the entry with the new document paths. */
    ContractEntry plan(ContractEntry entry, Direction direction, FileMoveTransaction moves) throws IOException {
        Path sourceFolder = direction.folder(dataDir, entry.id(), true);
        Path targetFolder = direction.folder(dataDir, entry.id(), false);
        Map<Path, Path> targets = new HashMap<>();
        List<ContractDocument> relocated = new ArrayList<>();

        for (ContractDocument document : entry.documents()) {
            Path source = requireFileInside(sourceFolder, document.path());
            Path target = targets.get(source);
            if (target == null) {
                target = targetFolder.resolve(direction.targetName(document, source.getFileName().toString()));
                if (Files.exists(target)) {
                    throw new IllegalStateException("Target file already exists: " + target);
                }
                targets.put(source, target);
                moves.add(source, target);
            }
            relocated.add(document.withPath(dataDir.relativize(target).toString()));
        }
        return entry.withDocuments(relocated);
    }

    private Path requireFileInside(Path folder, String relativePath) throws IOException {
        Path relative = Path.of(relativePath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("Document path must be relative: " + relativePath);
        }
        Path file = dataDir.resolve(relative).normalize();
        if (!file.startsWith(folder)) {
            throw new IllegalArgumentException("Document path is outside " + folder + ": " + relativePath);
        }
        Path realFile = file.toRealPath();
        if (!realFile.startsWith(folder.toRealPath()) || !Files.isRegularFile(realFile)) {
            throw new IllegalArgumentException("Document is not a regular file inside " + folder + ": "
                    + relativePath);
        }
        return file;
    }

    private static Path activeFolder(Path dataDir, UUID contractId) {
        return dataDir.resolve("documents").resolve(contractId.toString());
    }

    private static Path archiveFolder(Path dataDir, UUID contractId) {
        return dataDir.resolve("archive").resolve("documents").resolve(contractId.toString());
    }
}
