package kienzle.desktop.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Creates and restores complete, integrity-checked copies of one local data directory. */
public final class BackupService {
    public static final String BACKUP_EXTENSION = ".leasebackup";

    private static final String FORMAT_VERSION = "lease-manager-backup:v1";
    private static final String MANIFEST_ENTRY = "manifest.json";
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final int MAX_FILE_COUNT = 10_000;
    private static final long MAX_FILE_BYTES = 1024L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 10L * 1024 * 1024 * 1024;
    private static final int MAX_MANIFEST_BYTES = 8 * 1024 * 1024;
    private static final long MAX_ARCHIVE_BYTES = MAX_TOTAL_BYTES + 256L * 1024 * 1024;
    private static final Gson GSON = new Gson();

    private final Path dataDir;

    public BackupService(Path dataDir) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
    }

    public Path createBackup(Path targetFile) throws IOException {
        Path sourceRoot = dataDir.toRealPath();
        if (!Files.isDirectory(sourceRoot)) {
            throw new IOException("Lease Manager data path is not a directory");
        }

        Path target = targetFile.toAbsolutePath().normalize();
        Path targetParent = target.getParent();
        if (targetParent == null || target.getFileName() == null) {
            throw new IOException("Backup target must have a parent directory");
        }
        if (target.startsWith(sourceRoot)) {
            throw new IOException("Backup must be stored outside the Lease Manager data directory");
        }
        if (!Files.isDirectory(targetParent)) {
            throw new IOException("Backup target directory does not exist: " + targetParent);
        }
        Path realParent = targetParent.toRealPath();
        Path realTarget = realParent.resolve(target.getFileName());
        if (realTarget.startsWith(sourceRoot)) {
            throw new IOException("Backup must be stored outside the Lease Manager data directory");
        }
        if (Files.exists(realTarget)) {
            throw new IOException("Backup target already exists: " + realTarget);
        }

        List<Path> files = collectFiles(sourceRoot);
        Path temporary = Files.createTempFile(realParent, ".lease-manager-backup-", ".tmp");
        try {
            setPrivateFilePermissions(temporary);
            List<FileRecord> records = new ArrayList<>();
            long[] totalBytes = {0};
            try (OutputStream fileOutput = Files.newOutputStream(temporary);
                 ZipOutputStream zip = new ZipOutputStream(fileOutput)) {
                for (Path file : files) {
                    if (Files.isSymbolicLink(file)
                            || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Data file changed while creating backup: " + file);
                    }
                    String relativePath = zipPath(sourceRoot.relativize(file));
                    zip.putNextEntry(new ZipEntry("data/" + relativePath));
                    FileRecord record;
                    try (InputStream input = Files.newInputStream(file)) {
                        record = copyAndHash(input, zip, relativePath, totalBytes);
                    }
                    zip.closeEntry();
                    records.add(record);
                }

                byte[] manifest = manifestJson(records).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                if (manifest.length > MAX_MANIFEST_BYTES) {
                    throw new IOException("Backup manifest exceeds the maximum size");
                }
                zip.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
                zip.write(manifest);
                zip.closeEntry();
            }

            moveWithoutReplacing(temporary, realTarget);
            setPrivateFilePermissions(realTarget);
            return realTarget;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Restores into a new directory below {@code destinationParent}; existing data is never replaced. */
    public Path restoreToNewDirectory(Path backupFile, Path destinationParent) throws IOException {
        Path archive = backupFile.toRealPath();
        if (!Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Backup source is not a regular file");
        }
        if (Files.size(archive) > MAX_ARCHIVE_BYTES) {
            throw new IOException("Backup archive exceeds the maximum size");
        }
        Path parent = destinationParent.toAbsolutePath().normalize();
        if (parent.startsWith(dataDir)) {
            throw new IOException("Restore destination must be outside the current data directory");
        }
        if (!Files.isDirectory(parent)) {
            throw new IOException("Restore destination is not an existing directory");
        }
        parent = parent.toRealPath();
        if (Files.exists(dataDir) && parent.startsWith(dataDir.toRealPath())) {
            throw new IOException("Restore destination must be outside the current data directory");
        }

        Path staging = Files.createTempDirectory(parent, ".lease-manager-restore-");
        IOException failure = null;
        try {
            setPrivateDirectoryPermissions(staging);
            Map<String, FileRecord> restoredFiles = new HashMap<>();
            Set<String> entries = new HashSet<>();
            Manifest manifest = null;
            long[] totalBytes = {0};

            try (InputStream input = Files.newInputStream(archive);
                 ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (entry.isDirectory() || !entries.add(name)) {
                        throw new IOException("Backup contains a directory or duplicate entry: " + name);
                    }
                    if (MANIFEST_ENTRY.equals(name)) {
                        manifest = parseManifest(readBounded(zip, MAX_MANIFEST_BYTES, name));
                    } else if (name.startsWith("data/")) {
                        String relativePath = safeRelativePath(name.substring("data/".length()));
                        if (restoredFiles.containsKey(relativePath)) {
                            throw new IOException("Backup contains a duplicate data path: " + relativePath);
                        }
                        Path target = staging.resolve(relativePath).normalize();
                        if (!target.startsWith(staging)) {
                            throw new IOException("Backup path escapes the restore directory: " + name);
                        }
                        Files.createDirectories(target.getParent());
                        FileRecord record;
                        try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                            record = copyAndHash(zip, output, relativePath, totalBytes);
                        }
                        setPrivateFilePermissions(target);
                        restoredFiles.put(relativePath, record);
                    } else {
                        throw new IOException("Unexpected entry in backup: " + name);
                    }
                    zip.closeEntry();
                }
            }

            if (manifest == null) {
                throw new IOException("Backup does not contain a manifest");
            }
            validateManifest(manifest, restoredFiles);

            Path restored = parent.resolve("lease-manager-restored-" + UUID.randomUUID());
            moveWithoutReplacing(staging, restored);
            return restored;
        } catch (IOException e) {
            failure = e;
            throw e;
        } finally {
            try {
                deleteTree(staging);
            } catch (IOException cleanupFailure) {
                if (failure != null) {
                    failure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    private static List<Path> collectFiles(Path sourceRoot) throws IOException {
        List<Path> files = new ArrayList<>();
        long totalBytes = 0;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(candidate -> !candidate.equals(sourceRoot)).sorted().toList()) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Symbolic links cannot be included in a backup: " + path);
                }
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Unsupported file in data directory: " + path);
                }
                long size = Files.size(path);
                if (size > MAX_FILE_BYTES || size > MAX_TOTAL_BYTES - totalBytes) {
                    throw new IOException("Lease Manager data exceeds the maximum backup size");
                }
                totalBytes += size;
                files.add(path);
                if (files.size() > MAX_FILE_COUNT) {
                    throw new IOException("Lease Manager data contains too many files to back up");
                }
            }
        }
        return files;
    }

    private static FileRecord copyAndHash(InputStream input, OutputStream output, String relativePath,
                                          long[] totalBytes) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[BUFFER_SIZE];
        long size = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read > MAX_FILE_BYTES - size || read > MAX_TOTAL_BYTES - totalBytes[0]) {
                throw new IOException("Backup data exceeds configured size limits");
            }
            output.write(buffer, 0, read);
            digest.update(buffer, 0, read);
            size += read;
            totalBytes[0] += read;
        }
        return new FileRecord(relativePath, size, HexFormat.of().formatHex(digest.digest()));
    }

    private static String manifestJson(List<FileRecord> files) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("formatVersion", FORMAT_VERSION);
        manifest.addProperty("createdAt", Instant.now().toString());
        JsonArray entries = new JsonArray();
        for (FileRecord file : files) {
            JsonObject entry = new JsonObject();
            entry.addProperty("path", file.path());
            entry.addProperty("size", file.size());
            entry.addProperty("sha256", file.sha256());
            entries.add(entry);
        }
        manifest.add("files", entries);
        return GSON.toJson(manifest);
    }

    private static Manifest parseManifest(byte[] bytes) throws IOException {
        try {
            JsonElement parsed = GSON.fromJson(
                    new String(bytes, java.nio.charset.StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Backup manifest must be a JSON object");
            }
            JsonObject object = parsed.getAsJsonObject();
            if (!FORMAT_VERSION.equals(requiredString(object, "formatVersion"))) {
                throw new IOException("Unsupported backup format");
            }
            Instant.parse(requiredString(object, "createdAt"));
            JsonElement filesElement = object.get("files");
            if (filesElement == null || !filesElement.isJsonArray()) {
                throw new IOException("Backup manifest does not contain a file list");
            }
            JsonArray files = filesElement.getAsJsonArray();
            if (files.size() > MAX_FILE_COUNT) {
                throw new IOException("Backup manifest contains too many files");
            }
            List<FileRecord> records = new ArrayList<>();
            Set<String> paths = new HashSet<>();
            long totalBytes = 0;
            for (JsonElement element : files) {
                if (!element.isJsonObject()) {
                    throw new IOException("Backup manifest contains an invalid file record");
                }
                JsonObject file = element.getAsJsonObject();
                String path = safeRelativePath(requiredString(file, "path"));
                long size = requiredLong(file, "size");
                String sha256 = requiredString(file, "sha256");
                if (size < 0 || size > MAX_FILE_BYTES || size > MAX_TOTAL_BYTES - totalBytes) {
                    throw new IOException("Backup manifest contains an invalid file size");
                }
                if (sha256.length() != 64 || !isHex(sha256)) {
                    throw new IOException("Backup manifest contains an invalid SHA-256 hash");
                }
                if (!paths.add(path)) {
                    throw new IOException("Backup manifest contains a duplicate path: " + path);
                }
                totalBytes += size;
                records.add(new FileRecord(path, size, sha256));
            }
            return new Manifest(records);
        } catch (JsonParseException | IllegalArgumentException | java.time.DateTimeException e) {
            throw new IOException("Backup manifest is malformed", e);
        }
    }

    private static void validateManifest(Manifest manifest, Map<String, FileRecord> restoredFiles)
            throws IOException {
        if (manifest.files().size() != restoredFiles.size()) {
            throw new IOException("Backup manifest does not match the archive contents");
        }
        for (FileRecord expected : manifest.files()) {
            FileRecord actual = restoredFiles.get(expected.path());
            if (actual == null || actual.size() != expected.size() || !actual.sha256().equals(expected.sha256())) {
                throw new IOException("Backup integrity check failed for " + expected.path());
            }
        }
    }

    private static byte[] readBounded(InputStream input, int limit, String entryName) throws IOException {
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) {
            throw new IOException("Backup entry exceeds the maximum size: " + entryName);
        }
        return bytes;
    }

    private static String requiredString(JsonObject object, String name) throws IOException {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IOException("Backup manifest is missing string field: " + name);
        }
        return element.getAsString();
    }

    private static long requiredLong(JsonObject object, String name) throws IOException {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IOException("Backup manifest is missing numeric field: " + name);
        }
        try {
            return element.getAsBigDecimal().longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IOException("Backup manifest contains an invalid number: " + name, e);
        }
    }

    private static String safeRelativePath(String path) throws IOException {
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\")
                || path.matches("^[A-Za-z]:.*")) {
            throw new IOException("Backup contains an invalid relative path");
        }
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment) || segment.contains(":")) {
                throw new IOException("Backup contains an invalid relative path: " + path);
            }
        }
        try {
            Path relative = Path.of(path);
            if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
                throw new IOException("Backup contains an invalid relative path: " + path);
            }
            return String.join("/", segments);
        } catch (InvalidPathException e) {
            throw new IOException("Backup contains an invalid relative path: " + path, e);
        }
    }

    private static String zipPath(Path relative) throws IOException {
        String path = relative.toString().replace(relative.getFileSystem().getSeparator(), "/");
        return safeRelativePath(path);
    }

    private static boolean isHex(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!((character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f')
                    || (character >= 'A' && character <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static MessageDigest sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    private static void moveWithoutReplacing(Path source, Path target) throws IOException {
        Files.move(source, target);
    }

    private static void setPrivateFilePermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystems use their platform's default permissions.
        }
    }

    private static void setPrivateDirectoryPermissions(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystems use their platform's default permissions.
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record FileRecord(String path, long size, String sha256) {
    }

    private record Manifest(List<FileRecord> files) {
    }
}
