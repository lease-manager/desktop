package kienzle.desktop.pdf;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.LeaseContractPdfPreview;
import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ContractPdfServiceTest {
    @TempDir
    Path dir;

    @Test
    void createPdf_storesStampedFileAndRegistersHash() throws Exception {
        var contracts = new ContractService(new JsonContractRepository(dir));
        var pdfs = new ContractPdfService(contracts, new LeaseContractPdfPreview(), new PdfStamper(), new PdfStorage(dir));
        var workflow = new ContractWorkflow(contracts, pdfs);

        ContractEntry draft = workflow.save(null, LeaseContract.builder().name("N").build());
        ContractEntry created = workflow.createPdf(draft);

        assertEquals(ContractStatus.PDF_CREATED, created.status());
        ContractDocument doc = created.documents().get(0);
        assertEquals(DocumentKind.ORIGINAL, doc.kind());
        Path file = workflow.originalPdf(created).orElseThrow();
        assertEquals(doc.sha256(), Hashing.sha256(Files.readAllBytes(file)));

        ContractEntry awaiting = workflow.markAwaitingSignature(created);
        assertEquals(ContractStatus.AWAITING_SIGNATURE, awaiting.status());
    }

    @Test
    void creatingAnotherPdf_keepsPreviousVersionAndItsHashIntact() throws Exception {
        var contracts = new ContractService(new JsonContractRepository(dir));
        var pdfs = new ContractPdfService(contracts, new LeaseContractPdfPreview(), new PdfStamper(), new PdfStorage(dir));
        var workflow = new ContractWorkflow(contracts, pdfs);

        ContractEntry draft = workflow.save(null, LeaseContract.builder().name("N").build());
        ContractEntry firstVersion = workflow.createPdf(draft);
        ContractDocument firstDocument = firstVersion.documents().get(0);
        Path firstFile = pdfs.locate(firstDocument.path());
        byte[] firstBytes = Files.readAllBytes(firstFile);

        ContractEntry secondVersion = workflow.createPdf(firstVersion);
        ContractDocument secondDocument = secondVersion.documents().get(1);

        assertNotEquals(firstDocument.path(), secondDocument.path());
        assertEquals(firstDocument.sha256(), Hashing.sha256(Files.readAllBytes(firstFile)));
        assertArrayEquals(firstBytes, Files.readAllBytes(firstFile));
        assertTrue(Files.exists(pdfs.locate(secondDocument.path())));
    }
}
