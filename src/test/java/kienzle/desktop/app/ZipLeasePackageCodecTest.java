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

    @Test
    void readPackageRejectsFileThatIsNoArchive() throws Exception {
        Path garbage = dir.resolve("garbage.leasepack");
        java.nio.file.Files.write(garbage, "not a zip at all".getBytes(StandardCharsets.UTF_8));
        Path empty = dir.resolve("empty.leasepack");
        java.nio.file.Files.write(empty, new byte[0]);

        assertThrows(IOException.class, () -> codec.readPackage(garbage));
        assertThrows(IOException.class, () -> codec.readPackage(empty));
    }

    @Test
    void readPackageRejectsMissingPdfAndMissingManifest() throws Exception {
        Path noPdf = dir.resolve("nopdf.leasepack");
        writeArchive(noPdf, "manifest.json", "{}");
        Path noManifest = dir.resolve("nomanifest.leasepack");
        writeArchive(noManifest, "contract.pdf", "pdf");

        assertThrows(IOException.class, () -> codec.readPackage(noPdf));
        assertThrows(IOException.class, () -> codec.readPackage(noManifest));
    }

    @Test
    void readPackageRejectsPathTraversalEntryNames() throws Exception {
        Path archive = dir.resolve("traversal.leasepack");
        writeArchive(archive, "../manifest.json", "{}", "contract.pdf", "pdf");

        assertThrows(IOException.class, () -> codec.readPackage(archive));
    }

    @Test
    void readPackageRejectsOversizedManifest() throws Exception {
        Path archive = dir.resolve("big.leasepack");
        writeArchive(archive, "manifest.json", "x".repeat(1024 * 1024 + 10), "contract.pdf", "pdf");

        assertThrows(IOException.class, () -> codec.readPackage(archive));
    }

    @Test
    void writeThenReadRoundTripsManifestAndPdf() throws Exception {
        var manifest = new ContractPackageService.LeasePackageManifest("leasepack:v1", java.util.UUID.randomUUID(),
                java.time.Instant.parse("2026-01-01T10:00:00Z"), "abc", "Name", "{\"a\":1}", "sig");
        Path archive = dir.resolve("roundtrip.leasepack");
        codec.writePackage(archive, manifest, new byte[]{1, 2, 3});

        var read = codec.readPackage(archive);
        org.junit.jupiter.api.Assertions.assertEquals(manifest, read.manifest());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[]{1, 2, 3}, read.pdfBytes());
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
