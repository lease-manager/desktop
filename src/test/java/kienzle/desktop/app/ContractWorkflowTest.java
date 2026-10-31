package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.ContractPdfService;
import kienzle.desktop.pdf.PdfStamper;
import kienzle.desktop.pdf.PdfStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContractWorkflowTest {
    @TempDir
    Path tempDir;

    @Test
    void lifecycleAndPackageAccessRemainAvailable() throws Exception {
        ContractService contracts = new ContractService(new JsonContractRepository(tempDir));
        ContractPdfService pdfs = new ContractPdfService(contracts, new kienzle.desktop.LeaseContractPdfPreview(),
                new PdfStamper(), new PdfStorage(tempDir));
        ContractWorkflow workflow = new ContractWorkflow(contracts, pdfs, new ContractPackageService(tempDir));

        LeaseContract contract = sample();
        ContractEntry created = workflow.save(null, contract);
        workflow.createPdf(created);
        assertEquals(1, workflow.list().size());

        workflow.archive(created);
        assertEquals(1, workflow.listArchived().size());

        ContractEntry restored = workflow.restore(workflow.listArchived().get(0));
        assertEquals(created.id(), restored.id());

        Path pdfFile = tempDir.resolve("export.leasepack");
        workflow.exportPackage(restored, pdfFile);
        assertEquals(true, Files.exists(pdfFile));
        assertNotNull(workflow.readPackage(pdfFile));
    }

    private LeaseContract sample() {
        return new LeaseContract(
                "Pachtvertrag",
                "Verpächter GmbH",
                "Berlin",
                LocalDate.of(2025, 1, 1),
                "Pächter",
                "Berlin",
                LocalDate.of(2025, 1, 2),
                "123/45",
                "Flur 1",
                new BigDecimal("4800.50"),
                "Ackerland",
                "Beschreibung",
                new BigDecimal("2350.00"),
                "Unbestimmte Dauer",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("1900.00"),
                true,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2025, 2, 1),
                "Empfänger",
                "Bank",
                "DE89 3704 0044 0532 0130 00",
                "GENODEF1RFS",
                "Amtsgericht Berlin",
                "6 Monate",
                MonthDay.of(10, 31),
                "§ 4",
                "§ 5",
                "Musterstraße 1",
                "Musterstraße 2",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 12, 31),
                "jährlich",
                "1.",
                "Wortform 1",
                "Wortform 2"
        );
    }
}
