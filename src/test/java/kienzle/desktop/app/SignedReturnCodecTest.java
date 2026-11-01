package kienzle.desktop.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SignedReturnCodecTest {
    @TempDir
    Path dir;

    private final SignedReturnCodec codec = new SignedReturnCodec();

    @Test
    void read_rejectsFileThatIsNoArchive() throws Exception {
        Path garbage = dir.resolve("garbage.leasesigned");
        Files.write(garbage, "nope".getBytes(StandardCharsets.UTF_8));
        Path empty = dir.resolve("empty.leasesigned");
        Files.write(empty, new byte[0]);

        assertThrows(IOException.class, () -> codec.read(garbage));
        assertThrows(IOException.class, () -> codec.read(empty));
    }

    @Test
    void read_rejectsMissingEntriesAndMalformedManifest() throws Exception {
        Path noPdf = archive("nopdf.leasesigned", "manifest.json", "{}");
        Path noManifest = archive("nomanifest.leasesigned", "signed.pdf", "pdf");
        Path malformed = archive("malformed.leasesigned", "manifest.json", "{not json", "signed.pdf", "pdf");
        Path wrongType = archive("wrong.leasesigned", "manifest.json", "[]", "signed.pdf", "pdf");

        for (Path file : new Path[]{noPdf, noManifest, malformed, wrongType}) {
            assertThrows(IOException.class, () -> codec.read(file), file.toString());
        }
    }

    @Test
    void read_rejectsTraversalAndOversizedEntries() throws Exception {
        Path traversal = archive("traversal.leasesigned", "../signed.pdf", "pdf", "manifest.json", "{}");
        Path big = archive("big.leasesigned", "manifest.json", "x".repeat(1024 * 1024 + 10), "signed.pdf", "pdf");

        assertThrows(IOException.class, () -> codec.read(traversal));
        assertThrows(IOException.class, () -> codec.read(big));
    }

    private Path archive(String name, String... namesAndContents) throws IOException {
        Path file = dir.resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (int index = 0; index < namesAndContents.length; index += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[index]));
                zip.write(namesAndContents[index + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return file;
    }
}
