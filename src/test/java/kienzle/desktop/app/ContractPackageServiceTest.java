package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.Hashing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.*;

class ContractPackageServiceTest {
    @TempDir
    Path dir;

    private LeaseContract sample() {
        return new LeaseContract("Name", "Lessor", "Ort", LocalDate.of(2025, 1, 1), "Tenant", "Ort",
                LocalDate.of(2025, 1, 2), "1/2", "Flur", new BigDecimal("4800.5"), "Acker", "Desc",
                new BigDecimal("2350.00"), "Unbestimmte Dauer", LocalDate.of(2025, 1, 1), new BigDecimal("1900.00"),
                true, LocalDate.of(2027, 1, 1), LocalDate.of(2025, 2, 1), "Rcpt", "Bank", "DE00", "BIC", "AG",
                "6 Monate", MonthDay.of(10, 31), "p4", "p5", "Addr1", "Addr2", null, LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 12, 31), "jährlich", "1.", "words1", "words2");
    }

    @Test
    void exportPackage_containsManifestAndOriginalPdf() throws Exception {
        Path dataDir = dir.resolve("data");
        JsonContractRepository repository = new JsonContractRepository(dataDir);
        ContractService service = new ContractService(repository);
        ContractEntry entry = service.create(sample());

        Path pdfPath = dataDir.resolve("documents").resolve(entry.id().toString()).resolve("original.pdf");
        Files.createDirectories(pdfPath.getParent());
        byte[] pdfBytes = "contract-pdf-content".getBytes(StandardCharsets.UTF_8);
        Files.write(pdfPath, pdfBytes);

        ContractEntry withPdf = service.registerOriginalPdf(
                entry.id(), dataDir.relativize(pdfPath).toString(), Hashing.sha256(pdfBytes));

        Path archive = dir.resolve("export.leasepack");
        ContractPackageService serviceUnderTest = new ContractPackageService(dataDir);
        ContractPackageService.ContractPackage exported = serviceUnderTest.exportPackage(withPdf, archive);

        assertTrue(Files.exists(archive));
        assertEquals(entry.id(), exported.manifest().contractId());
        assertEquals(Hashing.sha256(pdfBytes), exported.manifest().originalPdfSha256());
        assertArrayEquals(pdfBytes, exported.pdfBytes());

        ContractPackageService.ContractPackage reloaded = serviceUnderTest.readPackage(archive);
        assertEquals(exported.manifest(), reloaded.manifest());
        assertArrayEquals(pdfBytes, reloaded.pdfBytes());
    }

    @Test
    void exportPackage_signsManifestAndRejectsTamperedSignature() throws Exception {
        Path dataDir = dir.resolve("data");
        JsonContractRepository repository = new JsonContractRepository(dataDir);
        ContractService service = new ContractService(repository);
        ContractEntry entry = service.create(sample());

        Path pdfPath = dataDir.resolve("documents").resolve(entry.id().toString()).resolve("original.pdf");
        Files.createDirectories(pdfPath.getParent());
        byte[] pdfBytes = "contract-pdf-content".getBytes(StandardCharsets.UTF_8);
        Files.write(pdfPath, pdfBytes);

        ContractEntry withPdf = service.registerOriginalPdf(
                entry.id(), dataDir.relativize(pdfPath).toString(), Hashing.sha256(pdfBytes));

        Path archive = dir.resolve("signed.leasepack");
        ContractPackageService serviceUnderTest = new ContractPackageService(dataDir);
        ContractPackageService.ContractPackage exported = serviceUnderTest.exportPackage(withPdf, archive);

        assertFalse(exported.manifest().packageSignature().isBlank());
        assertEquals(exported.manifest().packageSignature(),
                PackageSecurity.sign(dataDir, exported.manifest().withoutSignature()));

        ContractPackageService.LeasePackageManifest tampered = new ContractPackageService.LeasePackageManifest(
                exported.manifest().schemaVersion(),
                exported.manifest().contractId(),
                exported.manifest().createdAt(),
                exported.manifest().originalPdfSha256(),
                exported.manifest().contractName(),
                exported.manifest().contractJson(),
                exported.manifest().packageSignature() + "0");
        Path tamperedArchive = dir.resolve("tampered-signature.leasepack");
        new ZipLeasePackageCodec().writePackage(tamperedArchive, tampered, exported.pdfBytes());

        assertThrows(Exception.class, () -> serviceUnderTest.readPackage(tamperedArchive));
    }

    @Test
    void readPackage_rejectsTamperedArchive() throws Exception {
        Path dataDir = dir.resolve("data");
        JsonContractRepository repository = new JsonContractRepository(dataDir);
        ContractService service = new ContractService(repository);
        ContractEntry entry = service.create(sample());

        Path pdfPath = dataDir.resolve("documents").resolve(entry.id().toString()).resolve("original.pdf");
        Files.createDirectories(pdfPath.getParent());
        byte[] pdfBytes = "contract-pdf-content".getBytes(StandardCharsets.UTF_8);
        Files.write(pdfPath, pdfBytes);

        ContractEntry withPdf = service.registerOriginalPdf(
                entry.id(), dataDir.relativize(pdfPath).toString(), Hashing.sha256(pdfBytes));

        Path archive = dir.resolve("tampered.leasepack");
        ContractPackageService serviceUnderTest = new ContractPackageService(dataDir);
        serviceUnderTest.exportPackage(withPdf, archive);

        Files.write(archive, new byte[] {1, 2, 3, 4});

        assertThrows(Exception.class, () -> serviceUnderTest.readPackage(archive));
    }
}
