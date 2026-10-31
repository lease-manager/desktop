package kienzle.desktop.contract;

import kienzle.desktop.LeaseContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.*;

class ContractServiceTest {
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
