package kienzle.desktop.app;

import kienzle.desktop.TestContracts;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.ContractStatus;
import kienzle.desktop.contract.DocumentKind;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.contract.SignatureSource;
import kienzle.desktop.pdf.Hashing;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignedReturnImportServiceTest {
    @TempDir
    Path dir;

    private ContractService contracts;
    private ContractPackageService packages;
    private ContractEntry awaiting;
    private final SignedReturnCodec codec = new SignedReturnCodec();

    @BeforeEach
    void setUp() throws IOException {
        Path dataDir = dir.resolve("data");
        contracts = new ContractService(new JsonContractRepository(dataDir));
        packages = new ContractPackageService(dataDir);
        ContractEntry entry = contracts.create(TestContracts.sampleContract());
        Path original = dataDir.resolve("documents").resolve(entry.id().toString()).resolve("original.pdf");
        Files.createDirectories(original.getParent());
        byte[] originalBytes = pdf();
        Files.write(original, originalBytes);
        entry = contracts.registerOriginalPdf(entry.id(), dataDir.relativize(original).toString(),
                Hashing.sha256(originalBytes));
        awaiting = contracts.markAwaitingSignature(entry.id());
    }

    private static byte[] pdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(out);
            return out.toByteArray();
        }
    }

    private Path signedFile(String originalHash, byte[] signedPdf, String signedHash) throws IOException {
        Path file = dir.resolve("return-" + System.nanoTime() + ".leasesigned");
        codec.write(file, new SignedReturnCodec.Manifest(SignedReturnCodec.SCHEMA_VERSION, awaiting.id(),
                originalHash, signedHash, Instant.parse("2026-11-01T10:00:00Z"), "Max", "Android"), signedPdf);
        return file;
    }

    private String originalHash() {
        return awaiting.documents().stream().filter(d -> d.kind() == DocumentKind.ORIGINAL)
                .findFirst().orElseThrow().sha256();
    }

    @Test
    void validReturn_marksContractSignedByMobile() throws Exception {
        byte[] signed = pdf();
        Path file = signedFile(originalHash(), signed, Hashing.sha256(signed));

        ContractEntry result = packages.importSignedReturn(file);

        assertEquals(ContractStatus.SIGNED, result.status());
        assertEquals(SignatureSource.MOBILE, result.signature().source());
        assertEquals("Max", result.signature().signerName());
        assertTrue(result.documents().stream().anyMatch(d -> d.kind() == DocumentKind.SIGNED_MOBILE));
    }

    @Test
    void differentOriginalVersion_isRejected() throws Exception {
        byte[] signed = pdf();
        Path file = signedFile("0".repeat(64), signed, Hashing.sha256(signed));

        assertThrows(IOException.class, () -> packages.importSignedReturn(file));
        assertEquals(ContractStatus.AWAITING_SIGNATURE, contracts.list().get(0).status());
    }

    @Test
    void tamperedPdf_isRejected() throws Exception {
        Path file = signedFile(originalHash(), pdf(), "1".repeat(64));

        assertThrows(IOException.class, () -> packages.importSignedReturn(file));
    }

    @Test
    void nonPdfContent_isRejected() throws Exception {
        byte[] junk = "not a pdf".getBytes(StandardCharsets.UTF_8);
        Path file = signedFile(originalHash(), junk, Hashing.sha256(junk));

        assertThrows(IOException.class, () -> packages.importSignedReturn(file));
    }

    @Test
    void secondReturn_isRejectedBecauseContractIsAlreadySigned() throws Exception {
        byte[] signed = pdf();
        Path file = signedFile(originalHash(), signed, Hashing.sha256(signed));
        packages.importSignedReturn(file);

        assertThrows(IOException.class, () -> packages.importSignedReturn(file));
    }

    @Test
    void unexpectedArchiveEntry_isRejected() throws Exception {
        Path file = dir.resolve("evil.leasesigned");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("../evil.txt"));
            zip.write(1);
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> packages.importSignedReturn(file));
    }
}
