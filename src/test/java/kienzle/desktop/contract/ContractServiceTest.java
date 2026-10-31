package kienzle.desktop.contract;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.TestContracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContractServiceTest {
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
    void fullWorkflow_persistsAndReloads() {
        var service = new ContractService(new JsonContractRepository(dir));
        var entry = service.create(sample());
        assertEquals(ContractStatus.DRAFT, entry.status());

        service.registerOriginalPdf(entry.id(), "pdf/a.pdf", "abc");
        service.markAwaitingSignature(entry.id());
        service.attachSigned(entry.id(), SignatureSource.SCAN, "pdf/a-signed.pdf", "def", null, null);

        var reloaded = new ContractService(new JsonContractRepository(dir)).list();
        assertEquals(1, reloaded.size());
        var loaded = reloaded.get(0);
        assertEquals(ContractStatus.SIGNED, loaded.status());
        assertEquals(SignatureSource.SCAN, loaded.signature().source());
        assertEquals(2, loaded.documents().size());
        assertEquals(DocumentKind.SIGNED_SCAN, loaded.documents().get(1).kind());
        assertEquals(sample(), loaded.contract());
    }

    @Test
    void revokeAwaitingSignature_returnsToPdfCreatedOnlyFromAwaiting() {
        var service = new ContractService(new JsonContractRepository(dir));
        var entry = service.create(sample());
        assertThrows(IllegalStateException.class, () -> service.revokeAwaitingSignature(entry.id()));
        service.registerOriginalPdf(entry.id(), "p", "h");
        service.markAwaitingSignature(entry.id());

        assertEquals(ContractStatus.PDF_CREATED, service.revokeAwaitingSignature(entry.id()).status());
        assertEquals(ContractStatus.PDF_CREATED, service.list().get(0).status());

        service.markAwaitingSignature(entry.id());
        service.attachSigned(entry.id(), SignatureSource.SCAN, "p2", "h2", null, null);
        assertThrows(IllegalStateException.class, () -> service.revokeAwaitingSignature(entry.id()));
    }

    @Test
    void invalidTransitions_areRejected() {
        var service = new ContractService(new JsonContractRepository(dir));
        var entry = service.create(sample());
        assertThrows(IllegalStateException.class, () -> service.markAwaitingSignature(entry.id()));
        assertThrows(IllegalStateException.class,
                () -> service.attachSigned(entry.id(), SignatureSource.MOBILE, "p", "h", null, null));
        service.registerOriginalPdf(entry.id(), "p", "h");
        assertThrows(IllegalStateException.class, () -> service.update(entry.id(), sample()));
    }

    @Test
    void archive_removesContractFromActiveListAndPreservesRecordAndDocuments() throws Exception {
        var repository = new JsonContractRepository(dir);
        var service = new ContractService(repository);
        var entry = service.create(sample());
        Path document = dir.resolve("documents").resolve(entry.id().toString()).resolve("original.pdf");
        Files.createDirectories(document.getParent());
        Files.writeString(document, "pdf");
        ContractEntry withDocument = service.registerOriginalPdf(
                entry.id(), dir.relativize(document).toString(), "abc");
        ContractDocument original = withDocument.documents().get(0);

        service.archive(entry.id());

        assertTrue(service.list().isEmpty());
        assertFalse(Files.exists(document));
        Path archivedRecord = dir.resolve("archive").resolve("contracts").resolve(entry.id() + ".json");
        assertTrue(Files.exists(archivedRecord));
        String archivedJson = Files.readString(archivedRecord);
        Path archivedDocument = dir.resolve("archive").resolve("documents").resolve(entry.id().toString())
                .resolve(original.id() + "-original.pdf");
        assertTrue(Files.exists(archivedDocument));
        assertTrue(archivedJson.contains("archive" + java.io.File.separator + "documents"));
        assertTrue(archivedJson.contains("\"sha256\": \"abc\""));
        assertEquals(1, service.listArchived().size());

        ContractEntry restored = service.restore(entry.id());

        assertTrue(service.listArchived().isEmpty());
        assertEquals(1, service.list().size());
        assertEquals(entry.id(), restored.id());
        assertEquals(original.path(), restored.documents().get(0).path());
        assertEquals("pdf", Files.readString(document));
        assertFalse(Files.exists(archivedRecord));
    }
}
