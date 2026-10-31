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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static kienzle.desktop.TestContracts.sampleContract;

class ContractWorkflowTest {
    @TempDir
    Path tempDir;

    @Test
    void lifecycleAndPackageAccessRemainAvailable() throws Exception {
        ContractService contracts = new ContractService(new JsonContractRepository(tempDir));
        ContractPdfService pdfs = new ContractPdfService(contracts, new kienzle.desktop.LeaseContractPdfPreview(),
                new PdfStamper(), new PdfStorage(tempDir));
        ContractWorkflow workflow = new ContractWorkflow(contracts, pdfs, new ContractPackageService(tempDir));

        LeaseContract contract = sampleContract();
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
}
