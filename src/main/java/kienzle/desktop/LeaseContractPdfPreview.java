package kienzle.desktop;

import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LeaseContractPdfPreview {
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 54.0f;
    private static final float RIGHT = PAGE_WIDTH - LEFT;
    private static final float CONTENT_WIDTH = RIGHT - LEFT;
    private static final float BODY_SIZE = 9.5f;
    private static final float PARCEL_ROW_HEIGHT = 30f;
    private static final float[] PARCEL_COLUMN_WIDTHS = {66, 76, 62, 82, 116, 85};
    private static final float PARCEL_HEADER_HEIGHT = 34f;
    private static final float PAGE_TOP = PAGE_HEIGHT - 58;
    private static final float PAGE_BOTTOM = 45f;
    private static final float SIGNATURE_TOP = 130f;
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public List<Image> render(Map<String, String> values) throws IOException {
        try (PDDocument document = Loader.loadPDF(generatePdf(values))) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<Image> renderedPages = new ArrayList<>();
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                BufferedImage renderedPage = renderer.renderImageWithDPI(index, 120.0f);
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(renderedPage, "png", png);
                renderedPages.add(new Image(new java.io.ByteArrayInputStream(png.toByteArray())));
            }
            return renderedPages;
        }
    }

    public byte[] generatePdf(Map<String, String> values) throws IOException {
        try (PDDocument document = new PDDocument();
             PDDocument scratch = new PDDocument();
             ByteArrayOutputStream pdf = new ByteArrayOutputStream()) {
            Layout layout = layout(scratch, blocks(values));
            List<List<Placement>> pages = layout.pages();
            if (layout.endY() < SIGNATURE_TOP) {
                pages.add(new ArrayList<>());
            }
            for (int index = 0; index < pages.size(); index++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    for (Placement placement : pages.get(index)) {
                        placement.drawer().draw(content, placement.y());
                    }
                    if (index == pages.size() - 1) {
                        drawSignatures(content, values);
                    }
                    pageNumber(content, index + 1);
                }
            }
            document.save(pdf);
            return pdf.toByteArray();
        }
    }

    /** Draws a part of the contract starting at {@code y} and returns the y below it. */
    @FunctionalInterface
    private interface Drawer {
        float draw(PDPageContentStream content, float y) throws IOException;
    }

    /** A part of the contract that is never split across pages; {@code tableRow} parts repeat the table header. */
    private record Block(float gapBefore, Drawer drawer, boolean tableRow) {
    }

    private record Placement(Drawer drawer, float y) {
    }

    private record Layout(List<List<Placement>> pages, float endY) {
    }

    private List<Block> blocks(Map<String, String> values) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(0, (content, y) -> drawHeader(content, values, y), false));

        List<String[]> rows = parcelRows(values);
        String sum = sumText(values);
        Drawer start = (content, y) -> {
            y = sectionHeading(content, "§ 1", "Pachtgegenstand", y);
            y -= 10;
            y = drawParcelHeader(content, y);
            y = drawParcelRow(content, rows.get(0), y);
            return rows.size() == 1 ? drawParcelSum(content, sum, y) : y;
        };
        blocks.add(new Block(20, start, false));
        for (int index = 1; index < rows.size(); index++) {
            String[] cells = rows.get(index);
            boolean last = index == rows.size() - 1;
            blocks.add(new Block(0, (content, y) -> {
                y = drawParcelRow(content, cells, y);
                return last ? drawParcelSum(content, sum, y) : y;
            }, true));
        }

        blocks.add(new Block(24, (content, y) -> drawDuration(content, values, y), false));
        blocks.add(new Block(18, (content, y) -> {
            y = sectionHeading(content, "§ 3", "Pachtzins", y);
            return drawRentTerms(content, values, y - 10);
        }, false));
        blocks.add(new Block(12, (content, y) -> drawBankDetails(content, values, y), false));
        blocks.add(new Block(20, (content, y) -> drawPaymentDefault(content, values, y), false));
        blocks.add(new Block(20, (content, y) -> {
            y = sectionHeading(content, "§ 5", "Vorzeitige Kündigung", y);
            return editableParagraph(content, value(values, "paragraphFour"), y - 10);
        }, false));
        blocks.add(new Block(20, (content, y) -> drawOtherAgreements(content, values, y), false));
        return blocks;
    }

    /** Places the blocks top to bottom and starts a new page whenever the next block does not fit. */
    private Layout layout(PDDocument scratch, List<Block> blocks) throws IOException {
        List<List<Placement>> pages = new ArrayList<>();
        List<Placement> current = new ArrayList<>();
        pages.add(current);
        float y = PAGE_TOP;
        boolean atTop = true;
        for (Block block : blocks) {
            float gap = atTop ? 0 : block.gapBefore();
            float height = measure(scratch, block.drawer());
            if (!atTop && y - gap - height < PAGE_BOTTOM) {
                current = new ArrayList<>();
                pages.add(current);
                y = PAGE_TOP;
                gap = 0;
                if (block.tableRow()) {
                    current.add(new Placement(this::drawParcelHeader, y));
                    y = drawParcelHeaderHeight(y);
                }
            }
            y -= gap;
            current.add(new Placement(block.drawer(), y));
            y -= height;
            atTop = false;
        }
        return new Layout(pages, y);
    }

    private float measure(PDDocument scratch, Drawer drawer) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        scratch.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(scratch, page)) {
            return PAGE_HEIGHT - drawer.draw(content, PAGE_HEIGHT);
        }
    }

    private float rowHeight(String[] cells) {
        int lines = 1;
        for (int index = 0; index < cells.length; index++) {
            lines = Math.max(lines, wrappedLineCount(cells[index], PARCEL_COLUMN_WIDTHS[index] - 10, 8.0f));
        }
        return Math.max(PARCEL_ROW_HEIGHT, 13f + (lines - 1) * 9.5f + 9f);
    }

    private int wrappedLineCount(String value, float maxWidth, float size) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        int lines = 1;
        StringBuilder line = new StringBuilder();
        try {
            for (String word : sanitize(value).split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (BODY_FONT.getStringWidth(candidate) / 1000.0f * size > maxWidth && !line.isEmpty()) {
                    lines++;
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        return lines;
    }

    /** Table cells per parcel; a contract without parcels still gets one blank row to fill in by hand. */
    private List<String[]> parcelRows(Map<String, String> values) {
        int count;
        try {
            count = Integer.parseInt(value(values, "parcelCount"));
        } catch (NumberFormatException exception) {
            count = 0;
        }
        List<String[]> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String prefix = "parcel" + index;
            String rent = value(values, prefix + "Rent");
            rows.add(new String[]{
                    value(values, prefix + "Number"), value(values, prefix + "District"),
                    value(values, prefix + "Size"), value(values, prefix + "Type"),
                    value(values, prefix + "Designation"), rent.isBlank() ? "" : moneyValue(rent) + " EUR"});
        }
        if (rows.isEmpty()) {
            rows.add(new String[]{"", "", "", "", "", ""});
        }
        return rows;
    }

    private float drawHeader(PDPageContentStream content, Map<String, String> values, float y) throws IOException {
        y = centeredTitle(content, "Pachtvertrag", y);
        y -= 22;
        y = paragraph(content, "Zwischen", y, BODY_SIZE, BODY_FONT);
        y -= 5;
        y = partyBlock(content, "Verpächter(in)", value(values, "lessor"), value(values, "lessorAddress"), y);
        y -= 8;
        y = paragraph(content, "und", y, BODY_SIZE, BODY_FONT);
        y -= 5;
        y = partyBlock(content, "Pächter(in)", value(values, "tenant"), value(values, "tenantAddress"), y);
        y -= 12;
        return paragraph(content, "wird der nachstehende Pachtvertrag geschlossen:", y, BODY_SIZE, BODY_FONT);
    }

    private float drawDuration(PDPageContentStream content, Map<String, String> values, float y) throws IOException {
        y = sectionHeading(content, "§ 2", "Pachtdauer", y);
        y -= 10;
        y = paragraph(content, "Das Pachtverhältnis wird abgeschlossen auf "
                + blankIfEmpty(value(values, "leaseDuration")) + ".", y, BODY_SIZE, BODY_FONT);
        y -= 4;
        y = paragraph(content, "Das Pachtverhältnis beginnt am "
                + blankIfEmpty(value(values, "startDate")) + ".", y, BODY_SIZE, BODY_FONT);
        if ("Bestimmte Dauer".equals(value(values, "leaseDuration"))
                && !value(values, "leaseEndDate").isBlank()) {
            y -= 4;
            y = paragraph(content, "Das Pachtverhältnis endet am "
                    + value(values, "leaseEndDate") + ".", y, BODY_SIZE, BODY_FONT);
        }
        return y;
    }

    private float drawPaymentDefault(PDPageContentStream content, Map<String, String> values, float y)
            throws IOException {
        y = sectionHeading(content, "§ 4", "Zahlungsverzug und Gerichtsstand", y);
        y -= 8;
        y = paragraph(content,
                "Sollte der/die Pächter(in) seiner/ihrer Zahlungsverpflichtung nicht rechtzeitig nachkommen, "
                        + "ist der/die Verpächter(in) berechtigt, den Pachtvertrag zu lösen und das Pachtobjekt "
                        + "anderweitig zu verpachten. Forderungsaufrechnungen werden nicht anerkannt.",
                y, BODY_SIZE, BODY_FONT);
        y -= 10;
        y = paragraph(content, "Für Streitigkeiten aus diesem Pachtvertrag ist folgendes Amtsgericht zuständig:",
                y, BODY_SIZE, BODY_FONT);
        y -= 4;
        return paragraph(content, blankIfEmpty(value(values, "court")), y, BODY_SIZE, BODY_FONT);
    }

    private float drawOtherAgreements(PDPageContentStream content, Map<String, String> values, float y)
            throws IOException {
        y = sectionHeading(content, "§ 6", "Sonstige Vereinbarungen", y);
        y -= 10;
        y = editableParagraph(content, value(values, "paragraphFive"), y);
        String description = value(values, "description");
        if (!description.isBlank()) {
            y -= 4;
            y = paragraph(content, "Weitere Beschreibung: " + description, y, BODY_SIZE, BODY_FONT);
        }
        return y;
    }

    private float centeredTitle(PDPageContentStream content, String title, float y) throws IOException {
        float fontSize = 17.0f;
        float width = BOLD_FONT.getStringWidth(title) / 1000.0f * fontSize;
        text(content, title, (PAGE_WIDTH - width) / 2, y, fontSize, BOLD_FONT);
        return y - 24;
    }

    private float partyBlock(PDPageContentStream content, String title, String name, String address, float y)
            throws IOException {
        text(content, title, LEFT, y, 11.0f, BOLD_FONT);
        y -= 17;
        y = labeledLine(content, "Name, Vorname", name, y);
        y = labeledLine(content, "Anschrift (Straße, Haus-Nr., PLZ, Ort)", address, y);
        return y;
    }

    private float labeledLine(PDPageContentStream content, String label, String value, float y) throws IOException {
        text(content, label, LEFT + 4, y + 10, 7.0f, BODY_FONT);
        line(content, LEFT, y, RIGHT, y);
        if (!value.isBlank()) {
            fitText(content, value, LEFT + 4, y + 2, CONTENT_WIDTH - 8, 10.0f, BODY_FONT);
        }
        return y - 23;
    }

    private float sectionHeading(PDPageContentStream content, String paragraph, String title, float y)
            throws IOException {
        String heading = paragraph.isBlank() ? title : paragraph + "   " + title;
        float width = BOLD_FONT.getStringWidth(heading) / 1000.0f * 12.0f;
        text(content, heading, Math.max(LEFT, (PAGE_WIDTH - width) / 2), y, 12.0f, BOLD_FONT);
        return y - 17;
    }

    private String sumText(Map<String, String> values) {
        return moneyValue(blankIfEmpty(value(values, "totalRent"))) + " EUR";
    }

    private float drawParcelHeaderHeight(float y) {
        return y - PARCEL_HEADER_HEIGHT;
    }

    private float drawParcelHeader(PDPageContentStream content, float y) throws IOException {
        String[] headers = {"Flurstücknr.", "Gemarkung", "Größe m²", "Wirtschaftsart", "Bezeichnung", "Pachtzins EUR"};
        float x = LEFT;
        for (int index = 0; index < PARCEL_COLUMN_WIDTHS.length; index++) {
            float width = PARCEL_COLUMN_WIDTHS[index];
            content.setNonStrokingColor(new Color(225, 225, 225));
            content.addRect(x, y - PARCEL_HEADER_HEIGHT, width, PARCEL_HEADER_HEIGHT);
            content.fill();
            rectangle(content, x, y - PARCEL_HEADER_HEIGHT, width, PARCEL_HEADER_HEIGHT);
            fitText(content, headers[index], x + 5, y - 20, width - 10, 8.0f, BOLD_FONT);
            x += width;
        }
        return y - PARCEL_HEADER_HEIGHT;
    }

    private float drawParcelRow(PDPageContentStream content, String[] cells, float y) throws IOException {
        float height = rowHeight(cells);
        float x = LEFT;
        for (int index = 0; index < PARCEL_COLUMN_WIDTHS.length; index++) {
            float width = PARCEL_COLUMN_WIDTHS[index];
            rectangle(content, x, y - height, width, height);
            drawWrapped(content, cells[index], x + 5, y - 13, width - 10, 8.0f, BODY_FONT, 9.5f);
            x += width;
        }
        return y - height;
    }

    private float drawParcelSum(PDPageContentStream content, String sum, float y) throws IOException {
        float sumHeight = 28;
        float sumBottom = y - sumHeight;
        content.setNonStrokingColor(new Color(242, 242, 242));
        content.addRect(LEFT, sumBottom, CONTENT_WIDTH, sumHeight);
        content.fill();
        rectangle(content, LEFT, sumBottom, CONTENT_WIDTH, sumHeight);
        text(content, "Summe Pachtzins", LEFT + 8, sumBottom + 10, 9.0f, BOLD_FONT);
        float totalWidth = BOLD_FONT.getStringWidth(sanitize(sum)) / 1000.0f * 9.0f;
        text(content, sum, RIGHT - totalWidth - 8, sumBottom + 10, 9.0f, BOLD_FONT);
        return sumBottom - 8;
    }

    private float drawRentTerms(PDPageContentStream content, Map<String, String> values, float y)
            throws IOException {
        String introductoryRent = moneyValue(value(values, "introductoryRent"));
        String rent = moneyValue(value(values, "rent"));
        if (Boolean.parseBoolean(value(values, "hasIntroductoryRent")) && !introductoryRent.isBlank()) {
            y = paragraph(content, "Vom " + blankIfEmpty(value(values, "introductoryRentFrom"))
                    + " bis zum " + blankIfEmpty(value(values, "introductoryRentUntil"))
                    + " beträgt der Pachtzins " + introductoryRent + " EUR.", y, BODY_SIZE, BODY_FONT);
            y -= 4;
            y = paragraph(content, "In Worten: " + blankIfEmpty(value(values, "introductoryRentWords")),
                    y, BODY_SIZE, BODY_FONT);
            y -= 5;
            y = paragraph(content, "Nach dem " + blankIfEmpty(value(values, "rentIncreaseDate"))
                    + " beträgt der jährliche Pachtzins " + blankIfEmpty(rent) + " EUR.",
                    y, BODY_SIZE, BODY_FONT);
        } else {
            y = paragraph(content, "Der jährliche Pachtzins beträgt " + blankIfEmpty(rent) + " EUR.",
                    y, BODY_SIZE, BODY_FONT);
        }
        y -= 4;
        return paragraph(content, "In Worten: " + blankIfEmpty(value(values, "rentWords")), y, BODY_SIZE, BODY_FONT);
    }

    private float drawBankDetails(PDPageContentStream content, Map<String, String> values, float y)
            throws IOException {
        text(content, "Der Pachtzins ist " + blankIfEmpty(value(values, "rentDueInterval"))
                + " am " + blankIfEmpty(value(values, "rentDueDay"))
                + " fällig, erstmals am " + blankIfEmpty(value(values, "firstRentDate")) + ".",
                LEFT, y, BODY_SIZE, BODY_FONT);
        y -= 18;
        text(content, "in voller Höhe an", LEFT, y, BODY_SIZE, BODY_FONT);
        y -= 7;

        float recipientHeight = 30;
        float recipientBottom = y - recipientHeight;
        rectangle(content, LEFT, recipientBottom, CONTENT_WIDTH, recipientHeight);
        formCell(content, "Name des Zahlungsempfängers", value(values, "rentRecipient"),
                LEFT, recipientBottom, CONTENT_WIDTH, recipientHeight);

        y = recipientBottom - 18;
        text(content, "unaufgefordert auf das folgende Konto zu entrichten:", LEFT, y, BODY_SIZE, BODY_FONT);
        y -= 8;

        float bankRowHeight = 30;
        float accountRowHeight = 36;
        float transferBottom = y - bankRowHeight - accountRowHeight;
        float accountsTop = y - bankRowHeight;
        rectangle(content, LEFT, transferBottom, CONTENT_WIDTH, bankRowHeight + accountRowHeight);
        line(content, LEFT, accountsTop, RIGHT, accountsTop);
        formCell(content, "Geldinstitut", value(values, "bankName"), LEFT, accountsTop, CONTENT_WIDTH, bankRowHeight);

        float ibanWidth = CONTENT_WIDTH * 0.43f;
        float bicSplit = LEFT + ibanWidth;
        line(content, bicSplit, transferBottom, bicSplit, accountsTop);
        drawSeparatedAccountField(content, "IBAN", value(values, "iban"),
            LEFT, transferBottom, ibanWidth, accountRowHeight);
        drawSeparatedAccountField(content, "BIC", value(values, "bic"), bicSplit, transferBottom,
            CONTENT_WIDTH - ibanWidth, accountRowHeight);
        return transferBottom;
    }

        private void formCell(PDPageContentStream content, String label, String value,
                  float x, float bottom, float width, float height) throws IOException {
        text(content, label, x + 4, bottom + height - 7, 6.5f, BODY_FONT);
        fitText(content, blankIfEmpty(value), x + 4, bottom + 5, width - 8, 9.0f, BODY_FONT);
    }

    /** Draws one box per character; unused boxes stay empty so the field can be completed by hand. */
    private void drawSeparatedAccountField(PDPageContentStream content, String label, String value,
                                           float x, float bottom, float width, float height) throws IOException {
        text(content, label, x + 4, bottom + height - 7, 6.5f, BODY_FONT);
        boolean iban = label.equals("IBAN");
        String normalized = value.replaceAll("\\s+", "").toUpperCase(java.util.Locale.ROOT);
        if (normalized.isBlank() && iban) {
            normalized = "DE";
        }
        int cells = Math.max(iban ? 22 : 11, normalized.length());
        float cellWidth = iban ? (width - 8) / cells : CONTENT_WIDTH * 0.0245f;
        float fieldLeft = x + 4;
        float fieldBottom = bottom + 3;
        float fieldTop = bottom + 22;
        for (int index = 0; index < cells; index++) {
            float cellLeft = fieldLeft + index * cellWidth;
            if (index > 0) {
                line(content, cellLeft, fieldBottom, cellLeft, fieldTop);
            }
            if (index < normalized.length()) {
                String character = String.valueOf(normalized.charAt(index));
                float glyphWidth = BODY_FONT.getStringWidth(character) / 1000.0f * 10.0f;
                text(content, character, cellLeft + (cellWidth - glyphWidth) / 2, fieldBottom + 5, 10.0f,
                        BODY_FONT);
            }
        }
    }

    private float editableParagraph(PDPageContentStream content, String value, float y) throws IOException {
        if (value.isBlank()) {
            return paragraph(content, "____________________________", y, BODY_SIZE, BODY_FONT);
        }
        for (String line : value.split("\\R", -1)) {
            if (line.isBlank()) {
                y -= 5;
            } else {
                y = drawWrapped(content, line, LEFT, y, CONTENT_WIDTH, BODY_SIZE, BODY_FONT, BODY_SIZE + 3);
            }
        }
        return y;
    }

    private float paragraph(PDPageContentStream content, String value, float y, float size, PDType1Font font)
            throws IOException {
        return drawWrapped(content, value, LEFT, y, CONTENT_WIDTH, size, font, size + 3);
    }

    private float drawWrapped(PDPageContentStream content, String value, float x, float y, float maxWidth,
                              float size, PDType1Font font, float leading) throws IOException {
        if (value == null || value.isBlank()) {
            text(content, "____________________________", x, y, size, BODY_FONT);
            return y - leading;
        }
        String[] words = sanitize(value).split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.getStringWidth(candidate) / 1000.0f * size > maxWidth && !currentLine.isEmpty()) {
                text(content, currentLine.toString(), x, y, size, font);
                y -= leading;
                currentLine.setLength(0);
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    currentLine.append(' ');
                }
                currentLine.append(word);
            }
        }
        if (!currentLine.isEmpty()) {
            text(content, currentLine.toString(), x, y, size, font);
            y -= leading;
        }
        return y;
    }

    private void drawSignatures(PDPageContentStream content, Map<String, String> values) throws IOException {
        float signatureY = 92;
        float middle = PAGE_WIDTH / 2;
        String lessorPlaceDate = signatureDate(value(values, "lessorSignatureDate"));
        String tenantPlaceDate = signatureDate(value(values, "tenantSignatureDate"));
        text(content, blankIfEmpty(lessorPlaceDate), LEFT, signatureY + 25, 8.0f, BODY_FONT);
        text(content, blankIfEmpty(tenantPlaceDate), middle + 10, signatureY + 25, 8.0f, BODY_FONT);
        line(content, LEFT, signatureY, middle - 20, signatureY);
        line(content, middle + 10, signatureY, RIGHT, signatureY);
        text(content, "Unterschrift Verpächter(in)", LEFT, signatureY - 13, 8.0f, BOLD_FONT);
        text(content, "Unterschrift Pächter(in)", middle + 10, signatureY - 13, 8.0f, BOLD_FONT);
    }

    private String signatureDate(String value) {
        return value.replace(", den ", ", den ");
    }

    private void pageNumber(PDPageContentStream content, int page) throws IOException {
        String label = "- " + page + " -";
        float width = BODY_FONT.getStringWidth(label) / 1000.0f * 9.0f;
        text(content, label, (PAGE_WIDTH - width) / 2, 28, 9.0f, BODY_FONT);
    }

    private void text(PDPageContentStream content, String value, float x, float y, float size, PDType1Font font)
            throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(Color.BLACK);
        content.newLineAtOffset(x, y);
        content.showText(sanitize(value));
        content.endText();
    }

    private void fitText(PDPageContentStream content, String value, float x, float y, float maxWidth,
                         float preferredSize, PDType1Font font) throws IOException {
        if (value == null || value.isBlank()) {
            text(content, "________________", x, y, preferredSize, BODY_FONT);
            return;
        }
        String safeValue = sanitize(value);
        float size = preferredSize;
        while (size > 6.0f && font.getStringWidth(safeValue) / 1000.0f * size > maxWidth) {
            size -= 0.5f;
        }
        text(content, safeValue, x, y, size, font);
    }

    private void line(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.setStrokingColor(new Color(80, 80, 80));
        content.setLineWidth(0.6f);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private void rectangle(PDPageContentStream content, float x, float y, float width, float height)
            throws IOException {
        content.setStrokingColor(new Color(100, 100, 100));
        content.setLineWidth(0.5f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private String value(Map<String, String> values, String key) {
        return values.getOrDefault(key, "").trim();
    }

    private String blankIfEmpty(String value) {
        return value.isBlank() ? "________________" : value;
    }

    private String moneyValue(String value) {
        return value.replace("€", "").replaceAll("(?i)\\s*EUR\\s*$", "").trim();
    }

    private String sanitize(String value) {
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').replace("€", "EUR");
    }
}