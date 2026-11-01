package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.TestContracts;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.ContractStatus;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.Hashing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContractPackageServiceTest {
    @TempDir
    Path dir;

    private LeaseContract sample() {
        return TestContracts.sampleContract().toBuilder()
                .name("Name")
                .lessor("Lessor")
                .lessorLocation("Ort")
                .tenant("Tenant")
                .tenantLocation("Ort")
                .parcelNumber("1/2")
                .district("Flur")
                .type("Acker")
                .description("Desc")
                .rentRecipient("Rcpt")
                .iban("DE00")
                .bic("BIC")
                .court("AG")
                .paragraphFour("p4")
                .paragraphFive("p5")
                .lessorAddress("Addr1")
                .tenantAddress("Addr2")
                .introductoryRentWords("words1")
                .rentWords("words2")
                .build();
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

        ContractEntry imported = serviceUnderTest.importPackage(archive);
        assertEquals(ContractStatus.PDF_CREATED, imported.status());
        assertNotEquals(entry.id(), imported.id());
        assertTrue(Files.exists(dataDir.resolve(imported.documents().get(0).path())));
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

        assertThrows(IOException.class, () -> serviceUnderTest.readPackage(tamperedArchive));
        assertThrows(IOException.class, () -> serviceUnderTest.importPackage(tamperedArchive));
        assertEquals(1, new JsonContractRepository(dataDir).findAll().size(),
                "A package with an invalid signature must not create a contract");
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
