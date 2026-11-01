package kienzle.desktop.contract;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Stores active contracts under {@code contracts/} and archives records with their documents. */
public class JsonContractRepository implements ContractRepository {

    private final Path root;
    private final DocumentRelocator documents;
    private final Path contractsDir;
    private final Path archivedContractsDir;
    private final Gson gson = ContractJson.pretty();

    public JsonContractRepository(Path dataDir) {
        this.root = dataDir.toAbsolutePath().normalize();
        this.documents = new DocumentRelocator(root);
        this.contractsDir = root.resolve("contracts");
        this.archivedContractsDir = root.resolve("archive").resolve("contracts");
        try {
            Files.createDirectories(contractsDir);
            Files.createDirectories(archivedContractsDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path defaultDataDir() {
        return Path.of(System.getProperty("user.home"), ".lease-manager");
    }

    @Override
    public List<ContractEntry> findAll() {
        return readEntries(contractsDir);
    }

    @Override
    public List<ContractEntry> findArchived() {
        return readEntries(archivedContractsDir);
    }

    private List<ContractEntry> readEntries(Path directory) {
        Map<UUID, ContractEntry> result = new LinkedHashMap<>();
        try (var files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(p -> {
                ContractEntry entry = read(p);
                result.put(entry.id(), entry);
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<ContractEntry> list = new ArrayList<>(result.values());
        list.sort(java.util.Comparator.comparing(ContractEntry::createdAt));
        return list;
    }

    @Override
    public Optional<ContractEntry> findById(UUID id) {
        Path file = fileFor(id);
        return Files.exists(file) ? Optional.of(read(file)) : Optional.empty();
    }

    @Override
    public void save(ContractEntry entry) {
        Path target = fileFor(entry.id());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(tmp, gson.toJson(entry), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void archive(ContractEntry entry) {
        relocate(entry, DocumentRelocator.Direction.ARCHIVE, fileFor(entry.id()), archivedFileFor(entry.id()));
    }

    @Override
    public ContractEntry restore(UUID id) {
        ContractEntry archived = read(archivedFileFor(id));
        return relocate(archived, DocumentRelocator.Direction.RESTORE, archivedFileFor(id), fileFor(id));
    }

    @Override
    public void delete(UUID id) {
        Path folder = root.resolve("documents").resolve(id.toString());
        try {
            if (Files.exists(folder)) {
                try (var paths = Files.walk(folder)) {
                    for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                        Files.delete(path);
                    }
                }
            }
            Files.deleteIfExists(fileFor(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Moves the documents and the record from one store to the other; either everything moves or nothing. */
    private ContractEntry relocate(ContractEntry entry, DocumentRelocator.Direction direction,
                                   Path fromRecord, Path toRecord) {
        Path tmp = toRecord.resolveSibling(toRecord.getFileName() + ".tmp");
        FileMoveTransaction moves = new FileMoveTransaction();
        try {
            if (Files.exists(toRecord)) {
                throw new IllegalStateException("Contract already exists in target store: " + entry.id());
            }
            ContractEntry relocated = documents.plan(entry, direction, moves);
            Files.writeString(tmp, gson.toJson(relocated), StandardCharsets.UTF_8);
            moves.add(tmp, toRecord);
            moves.commit();
            try {
                Files.delete(fromRecord);
            } catch (IOException e) {
                moves.rollback(e);
                throw e;
            }
            return relocated;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw new UncheckedIOException(e);
        }
    }


    private Path fileFor(UUID id) {
        return contractsDir.resolve(id + ".json");
    }

    private Path archivedFileFor(UUID id) {
        return archivedContractsDir.resolve(id + ".json");
    }

    private ContractEntry read(Path file) {
        try {
            return gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), ContractEntry.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
