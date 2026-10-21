package kienzle.desktop;

import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaseContractPdfPreviewTest {

    @Test
    void render_shouldCreateNewTwoPageContractUsingEnteredValues() throws Exception {
        Map<String, String> values = Map.ofEntries(
            Map.entry("lessor", "Verpächter Test"),
            Map.entry("lessorAddress", "Musterstraße 1, 10115 Berlin"),
            Map.entry("tenant", "Pächter Test"),
            Map.entry("tenantAddress", "Pachtweg 2, 10117 Berlin"),
            Map.entry("parcelNumber", "12/3"),
            Map.entry("district", "Senden"),
            Map.entry("size", "4800"),
            Map.entry("type", "Ackerland"),
            Map.entry("description", "Nordfeld"),
            Map.entry("rent", "1500,00"),
            Map.entry("totalRent", "1500,00"),
            Map.entry("leaseDuration", "unbestimmte"),
            Map.entry("hasIntroductoryRent", "true"),
            Map.entry("startDate", "01.01.2025"),
            Map.entry("introductoryRentFrom", "01.01.2025"),
            Map.entry("introductoryRentUntil", "31.12.2025"),
            Map.entry("introductoryRent", "1200,00"),
            Map.entry("introductoryRentWords", "eintausendzweihundert Euro"),
            Map.entry("rentIncreaseDate", "01.01.2026"),
            Map.entry("rentWords", "eintausendfünfhundert Euro"),
                Map.entry("rentDueDay", "3. Werktag"),
                Map.entry("firstRentDate", "01.01.2025"),
                Map.entry("paymentClause", "eines jeden Jahres, erstmals am 01.01.2025"),
            Map.entry("rentRecipient", "Verpächter Test"),
            Map.entry("bankName", "Musterbank"),
                Map.entry("iban", "12345678901234567890"),
            Map.entry("bic", "MUSTDEFF"),
            Map.entry("court", "Amtsgericht Musterstadt"),
            Map.entry("terminationNotice", "drei"),
            Map.entry("gardeningYearEnd", "30.11"),
            Map.entry("lessorSignatureDate", "Berlin, den 01.01.2025"),
            Map.entry("tenantSignatureDate", "Berlin, den 01.01.2025"),
            Map.entry("paragraphFour", "Individueller § 4 Text für die Vorschau."),
            Map.entry("paragraphFive", "Individueller § 5 Text für die Vorschau.")
        );
        LeaseContractPdfPreview preview = new LeaseContractPdfPreview();
        List<Image> pages = preview.render(values);

        assertEquals(2, pages.size());
        assertTrue(pages.get(0).getWidth() > 0);
        assertTrue(pages.get(0).getHeight() > 0);
        assertTrue(pages.get(1).getWidth() > 0);
        assertTrue(pages.get(1).getHeight() > 0);

        try (PDDocument document = Loader.loadPDF(preview.generatePdf(values))) {
            assertEquals(2, document.getNumberOfPages());
            String pdfText = new PDFTextStripper().getText(document);
            assertTrue(pdfText.contains("Verpächter Test"));
            assertTrue(pdfText.contains("Amtsgericht Musterstadt"));
            assertTrue(pdfText.contains("Summe Pachtzins"));
            assertTrue(pdfText.contains("1500,00 EUR"));
            assertFalse(pdfText.contains("EUR EUR"));
            assertTrue(pdfText.contains("in voller Höhe an"));
            assertTrue(pdfText.contains("unaufgefordert auf das folgende Konto zu entrichten"));
            assertTrue(pdfText.contains("Name des Zahlungsempfängers"));
            assertTrue(pdfText.contains("IBAN"));
            assertTrue(pdfText.contains("BIC"));
            assertTrue(pdfText.contains("Individueller § 4 Text für die Vorschau."));
            assertTrue(pdfText.contains("Individueller § 5 Text für die Vorschau."));
            assertFalse(pdfText.contains("Kienzle, Doris"));
            assertFalse(pdfText.contains("Senden, den"));
        }
    }

    @Test
    void generatePdf_shouldOmitIntroductoryRentWhenFlagIsFalse() throws Exception {
        Map<String, String> values = Map.of(
                "leaseDuration", "Unbestimmte Dauer",
                "hasIntroductoryRent", "false",
                "introductoryRent", "900",
                "introductoryRentFrom", "01.01.2025",
                "introductoryRentUntil", "31.12.2025"
        );

        try (PDDocument document = Loader.loadPDF(new LeaseContractPdfPreview().generatePdf(values))) {
            String pdfText = new PDFTextStripper().getText(document);
            assertFalse(pdfText.contains("Vom 01.01.2025 bis zum 31.12.2025"));
        }
    }
}
