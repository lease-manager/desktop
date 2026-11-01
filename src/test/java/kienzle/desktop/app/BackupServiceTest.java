package kienzle.desktop.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {
    @TempDir
    Path dir;

    @Test
    void backupMustBeWrittenOutsideTheLiveDataDirectory() throws Exception {
        Path source = dir.resolve("source");
        Files.createDirectories(source);

        assertThrows(IOException.class, () -> new BackupService(source)
                .createBackup(source.resolve("nested.leasebackup")));
        assertFalse(Files.exists(source.resolve("nested.leasebackup")));
    }

    @Test
    void backupRestoresAllFilesIntoANewDataDirectory() throws Exception {
        Path source = dir.resolve("source");
        Files.createDirectories(source.resolve("contracts"));
        Files.createDirectories(source.resolve("documents/contract-1"));
        Files.createDirectories(source.resolve("archive/contracts"));
        Files.writeString(source.resolve("contracts/contract-1.json"), "contract");
        Files.writeString(source.resolve("documents/contract-1/original.pdf"), "pdf");
        Files.writeString(source.resolve("archive/contracts/contract-2.json"), "archived");
        Files.write(source.resolve("package-signing.key"), new byte[32]);
        Path backupDirectory = dir.resolve("backups");
        Path restoreParent = dir.resolve("restored");
        Files.createDirectories(backupDirectory);
        Files.createDirectories(restoreParent);
        Path backup = backupDirectory.resolve("snapshot.leasebackup");

        new BackupService(source).createBackup(backup);
        Path restored = new BackupService(source).restoreToNewDirectory(backup, restoreParent);

        assertNotEquals(source, restored);
        assertEquals("contract", Files.readString(restored.resolve("contracts/contract-1.json")));
        assertEquals("pdf", Files.readString(restored.resolve("documents/contract-1/original.pdf")));
        assertEquals("archived", Files.readString(restored.resolve("archive/contracts/contract-2.json")));
        assertArrayEquals(new byte[32], Files.readAllBytes(restored.resolve("package-signing.key")));
        assertEquals("contract", Files.readString(source.resolve("contracts/contract-1.json")));
    }

    @Test
    void restoreRejectsPathTraversalAndCleansUpStagingDirectory() throws Exception {
        Path backup = dir.resolve("traversal.leasebackup");
        writeArchive(backup, "data/../../escaped.txt", "unexpected");
        Path destination = dir.resolve("destination");
        Files.createDirectories(destination);

        assertThrows(IOException.class,
                () -> new BackupService(dir.resolve("unused")).restoreToNewDirectory(backup, destination));
        assertFalse(Files.exists(dir.resolve("escaped.txt")));
        try (var children = Files.list(destination)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void restoreRejectsFilesThatDoNotMatchTheManifest() throws Exception {
        Path backup = dir.resolve("tampered.leasebackup");
        String manifest = """
                {
                  "formatVersion": "lease-manager-backup:v1",
                  "createdAt": "2026-01-01T00:00:00Z",
                  "files": [
                    {"path": "contracts/contract.json", "size": 5, "sha256": "%s"}
                  ]
                }
                """.formatted("0".repeat(64));
        writeArchive(backup,
                "data/contracts/contract.json", "hello",
                "manifest.json", manifest);
        Path destination = dir.resolve("destination");
        Files.createDirectories(destination);

        assertThrows(IOException.class,
                () -> new BackupService(dir.resolve("unused")).restoreToNewDirectory(backup, destination));
        try (var children = Files.list(destination)) {
            assertEquals(0, children.count());
        }
    }

    private static void writeArchive(Path archive, String... namesAndContents) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (int index = 0; index < namesAndContents.length; index += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[index]));
                zip.write(namesAndContents[index + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
