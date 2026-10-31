package kienzle.desktop.pdf;

import kienzle.desktop.LeaseContractPdfPreview;
import kienzle.desktop.TestContracts;
import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.ContractStatus;
import kienzle.desktop.contract.DocumentKind;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.contract.SignatureSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanImportServiceTest {
    @TempDir
    Path dir;

    private ContractWorkflow workflow;
    private ContractEntry awaiting;
    private Path originalPdf;

    @BeforeEach
    void setUp() throws Exception {
        ContractService contracts = new ContractService(new JsonContractRepository(dir));
        ContractPdfService pdfs = new ContractPdfService(contracts, new LeaseContractPdfPreview(),
                new PdfStamper(), new PdfStorage(dir));
        workflow = new ContractWorkflow(contracts, pdfs);
        ContractEntry created = workflow.createPdf(workflow.save(null, TestContracts.sampleContract("Scan")));
        originalPdf = workflow.originalPdf(created).orElseThrow();
        awaiting = workflow.markAwaitingSignature(created);
    }

    @Test
    void pdfScan_isMatchedByQrAndStoredUnchanged() throws Exception {
        Path scan = dir.resolve("scan.pdf");
        Files.copy(originalPdf, scan);

        assertEquals(awaiting.id(), workflow.detectScanTarget(scan).orElseThrow().contractId());

        ContractEntry signed = workflow.attachScan(awaiting, scan);

        assertEquals(ContractStatus.SIGNED, signed.status());
        assertEquals(SignatureSource.SCAN, signed.signature().source());
        var document = signed.documents().get(signed.documents().size() - 1);
        assertEquals(DocumentKind.SIGNED_SCAN, document.kind());
        byte[] stored = Files.readAllBytes(dir.resolve(document.path()));
        assertArrayEquals(Files.readAllBytes(scan), stored);
        assertEquals(Hashing.sha256(stored), document.sha256());
        assertEquals(dir.resolve(document.path()), workflow.currentDocument(signed).orElseThrow());
        assertEquals(originalPdf, workflow.originalPdf(signed).orElseThrow());
    }

    @Test
    void imageScan_isMatchedByQr() throws Exception {
        Path scan = dir.resolve("scan.png");
        try (PDDocument document = Loader.loadPDF(originalPdf.toFile())) {
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 200f), "png", scan.toFile());
        }

        assertEquals(awaiting.id(), workflow.detectScanTarget(scan).orElseThrow().contractId());
    }

    @Test
    void imageWithoutQr_hasNoMatch() throws Exception {
        Path scan = dir.resolve("blank.png");
        ImageIO.write(new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB), "png", scan.toFile());

        assertTrue(workflow.detectScanTarget(scan).isEmpty());
    }

    @Test
    void unsupportedOrMissingFile_isRejected() throws Exception {
        Path text = Files.writeString(dir.resolve("scan.txt"), "x");

        assertThrows(IllegalArgumentException.class, () -> workflow.detectScanTarget(text));
        assertThrows(IllegalArgumentException.class, () -> workflow.attachScan(awaiting, text));
        assertThrows(java.io.IOException.class, () -> workflow.attachScan(awaiting, dir.resolve("missing.pdf")));
    }

    @Test
    void attachScan_requiresAwaitingSignature() throws Exception {
        Path scan = dir.resolve("scan.pdf");
        Files.copy(originalPdf, scan);
        ContractEntry back = workflow.revokeAwaitingSignature(awaiting);

        assertThrows(IllegalStateException.class, () -> workflow.attachScan(back, scan));
        assertTrue(workflow.detectScanTarget(scan).isPresent());
    }

    @Test
    void scanOfCurrentVersion_matchesOriginal() throws Exception {
        Path scan = dir.resolve("scan.pdf");
        Files.copy(originalPdf, scan);

        assertTrue(workflow.isCurrentScan(awaiting, workflow.detectScanTarget(scan).orElseThrow()));
    }

    @Test
    void scanOfOlderVersion_isDetectedAsOutdated() throws Exception {
        Path oldScan = dir.resolve("old.pdf");
        Files.copy(originalPdf, oldScan);
        QrPayload oldPayload = workflow.detectScanTarget(oldScan).orElseThrow();
        ContractEntry back = workflow.revokeAwaitingSignature(awaiting);
        ContractEntry changed = workflow.createPdf(back);
        ContractEntry again = workflow.markAwaitingSignature(changed);

        assertFalse(workflow.isCurrentScan(again, oldPayload));
    }
}
