package kienzle.desktop.pdf;

import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.SignatureSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Imports a scan of the signed contract (PDF or image): matches it via QR code and stores it unchanged. */
public class ScanImportService {
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final long MAX_SCAN_BYTES = 50L * 1024 * 1024;
    private static final float RENDER_DPI = 200f;

    private final ContractService contracts;
    private final PdfStorage storage;

    public ScanImportService(ContractService contracts, PdfStorage storage) {
        this.contracts = contracts;
        this.storage = storage;
    }

    /** @return the QR payload found in the scan, if any page or the image contains one */
    public Optional<QrPayload> detectPayload(Path scan) throws IOException {
        String extension = requireSupportedFile(scan);
        if (extension.equals("pdf")) {
            return detectInPdf(scan);
        }
        BufferedImage image = ImageIO.read(scan.toFile());
        if (image == null) {
            throw new IOException("Not a readable image: " + scan.getFileName());
        }
        return detectInImage(image);
    }

    public ContractEntry attachScan(UUID contractId, Path scan) throws IOException {
        String extension = requireSupportedFile(scan);
        byte[] bytes = Files.readAllBytes(scan);
        String path = storage.store(contractId, "signed-scan-" + UUID.randomUUID() + "." + extension, bytes);
        return contracts.attachSigned(contractId, SignatureSource.SCAN, path, Hashing.sha256(bytes), null, null);
    }

    private static Optional<QrPayload> detectInPdf(Path scan) throws IOException {
        try (PDDocument document = Loader.loadPDF(scan.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                Optional<QrPayload> payload = detectInImage(renderer.renderImageWithDPI(page, RENDER_DPI));
                if (payload.isPresent()) {
                    return payload;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<QrPayload> detectInImage(BufferedImage image) {
        return QrCodes.decode(image).flatMap(QrPayload::parse);
    }

    /**
     * Checks the scan's QR hash against the hash embedded in the given original PDF.
     * An original without an embedded hash (legacy) cannot be compared and counts as matching.
     */
    public boolean matchesOriginal(QrPayload payload, Path originalPdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(originalPdf.toFile())) {
            String expected = document.getDocumentInformation()
                    .getCustomMetadataValue(PdfStamper.PROPERTY_CONTENT_HASH);
            return expected == null || expected.equals(payload.contentHash());
        }
    }

    private static String requireSupportedFile(Path scan) throws IOException {
        String name = scan.getFileName().toString();
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!name.contains(".") || !SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported scan file type: " + name);
        }
        if (!Files.isRegularFile(scan)) {
            throw new IOException("Scan file not found: " + name);
        }
        if (Files.size(scan) > MAX_SCAN_BYTES) {
            throw new IllegalArgumentException("Scan file is too large: " + name);
        }
        return extension;
    }
}
