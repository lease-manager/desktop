package kienzle.desktop.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/** Adds the contract ID as QR code (top right of every page) and as PDF metadata so signed copies can be matched. */
public class PdfStamper {
    public static final String PROPERTY_CONTRACT_ID = "LeaseContractId";
    public static final String PROPERTY_CONTENT_HASH = "LeaseContentHash";
    private static final float QR_SIZE = 46f;
    private static final float MARGIN = 54f;
    private static final float TOP_OFFSET = 10f;
    private static final float ID_FONT_SIZE = 7f;

    public StampedPdf stamp(byte[] pdf, UUID contractId) throws IOException {
        QrPayload payload = new QrPayload(contractId, Hashing.sha256(pdf));
        PDImageXObject[] qr = new PDImageXObject[1];
        try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            qr[0] = LosslessFactory.createFromImage(document, QrCodes.encode(payload.encode(), 300));
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            String idText = "ID " + contractId;
            float idWidth = font.getStringWidth(idText) / 1000f * ID_FONT_SIZE;
            for (PDPage page : document.getPages()) {
                PDRectangle box = page.getMediaBox();
                float qrX = box.getWidth() - MARGIN - QR_SIZE;
                float qrY = box.getHeight() - TOP_OFFSET - QR_SIZE;
                try (var content = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    content.drawImage(qr[0], qrX, qrY, QR_SIZE, QR_SIZE);
                    content.beginText();
                    content.setFont(font, ID_FONT_SIZE);
                    content.newLineAtOffset(qrX - 8f - idWidth, qrY + QR_SIZE - ID_FONT_SIZE);
                    content.showText(idText);
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
