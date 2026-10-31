package kienzle.desktop.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import kienzle.desktop.LeaseContractPdfPreview;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PdfStamperTest {
    private final UUID id = UUID.randomUUID();

    @Test
    void payload_roundTrips() {
        var payload = new QrPayload(id, Hashing.sha256(new byte[]{1}));
        assertEquals(payload, QrPayload.parse(payload.encode()).orElseThrow());
        assertTrue(QrPayload.parse("https://example.org").isEmpty());
        assertTrue(QrPayload.parse("lease:v1:nonsense:abc").isEmpty());
    }

    @Test
    void stampedPdf_hasMetadataAndReadableQrOnEveryPage() throws Exception {
        byte[] raw = new LeaseContractPdfPreview().generatePdf(Map.of("lessor", "A", "tenant", "B"));
        StampedPdf stamped = new PdfStamper().stamp(raw, id);

        assertEquals(Hashing.sha256(stamped.bytes()), stamped.sha256());
        assertEquals(Hashing.sha256(raw), stamped.payload().contentHash());
        try (PDDocument doc = Loader.loadPDF(stamped.bytes())) {
            var info = doc.getDocumentInformation();
            assertEquals(id.toString(), info.getCustomMetadataValue(PdfStamper.PROPERTY_CONTRACT_ID));
            assertEquals(stamped.payload().encode(), info.getKeywords());
            var renderer = new PDFRenderer(doc);
            for (int page = 0; page < doc.getNumberOfPages(); page++) {
                var text = QrCodes.decode(renderer.renderImageWithDPI(page, 200f));
                assertEquals(stamped.payload(), QrPayload.parse(text.orElseThrow()).orElseThrow());
            }
        }
    }
}
