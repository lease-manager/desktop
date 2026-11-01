package kienzle.desktop.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipLeasePackageCodecTest {
    @TempDir
    Path dir;

    private final ZipLeasePackageCodec codec = new ZipLeasePackageCodec();

    @Test
    void readPackageRejectsMalformedManifest() throws Exception {
        Path archive = dir.resolve("malformed.leasepack");
        writeArchive(archive, "manifest.json", "{}", "contract.pdf", "pdf");

        assertThrows(IOException.class, () -> codec.readPackage(archive));
    }

    @Test
    void readPackageRejectsUnexpectedEntries() throws Exception {
        Path archive = dir.resolve("unexpected.leasepack");
        writeArchive(archive,
                "manifest.json", "{}",
                "contract.pdf", "pdf",
                "extra.txt", "unexpected");

        assertThrows(IOException.class, () -> codec.readPackage(archive));
    }

    private static void writeArchive(Path archive, String... namesAndContents) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
            for (int index = 0; index < namesAndContents.length; index += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[index]));
                zip.write(namesAndContents[index + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
