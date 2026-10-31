package kienzle.desktop.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/** Adds the contract ID as QR code (page footer) and as PDF metadata so signed copies can be matched. */
public class PdfStamper {
    public static final String PROPERTY_CONTRACT_ID = "LeaseContractId";
    public static final String PROPERTY_CONTENT_HASH = "LeaseContentHash";
    private static final float QR_SIZE = 46f;
    private static final float MARGIN = 54f;

    public StampedPdf stamp(byte[] pdf, UUID contractId) throws IOException {
        QrPayload payload = new QrPayload(contractId, Hashing.sha256(pdf));
        PDImageXObject[] qr = new PDImageXObject[1];
        try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            qr[0] = LosslessFactory.createFromImage(document, QrCodes.encode(payload.encode(), 300));
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (PDPage page : document.getPages()) {
                try (var content = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    content.drawImage(qr[0], MARGIN, 14f, QR_SIZE, QR_SIZE);
                    content.beginText();
                    content.setFont(font, 7f);
                    content.newLineAtOffset(MARGIN + QR_SIZE + 8f, 28f);
                    content.showText("ID " + contractId);
                    content.endText();
                }
            }
            PDDocumentInformation info = document.getDocumentInformation();
            info.setKeywords(payload.encode());
            info.setCustomMetadataValue(PROPERTY_CONTRACT_ID, contractId.toString());
            info.setCustomMetadataValue(PROPERTY_CONTENT_HASH, payload.contentHash());
            document.save(out);
            byte[] bytes = out.toByteArray();
            return new StampedPdf(bytes, Hashing.sha256(bytes), payload);
        }
    }
}
