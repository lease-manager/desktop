package kienzle.desktop.app;

import kienzle.desktop.contract.ContractDocument;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.DocumentKind;
import kienzle.desktop.pdf.ContractPdfService;
import kienzle.desktop.pdf.QrPayload;
import kienzle.desktop.pdf.ScanImportService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Encapsulates document and PDF-specific operations for a contract. */
public final class ContractDocumentUseCase {
    private final ContractPdfService pdfs;
    private final ScanImportService scans;

    public ContractDocumentUseCase(ContractPdfService pdfs) {
        this.pdfs = pdfs;
        this.scans = pdfs.scanImporter();
    }

    public ContractEntry createOriginalPdf(ContractEntry entry) throws IOException {
        return pdfs.createOriginalPdf(entry);
    }

    public Optional<QrPayload> detectScanTarget(Path scan) throws IOException {
        return scans.detectPayload(scan);
    }

    /** @return false if the scan was made from an older version of the contract PDF */
    public boolean isCurrentScan(ContractEntry entry, QrPayload payload) throws IOException {
        Optional<Path> original = originalPdf(entry);
        return original.isEmpty() || scans.matchesOriginal(payload, original.get());
    }

    public ContractEntry attachScan(ContractEntry entry, Path scan) throws IOException {
        return scans.attachScan(entry.id(), scan);
    }

    /** The signed document if one exists, otherwise the latest original PDF. */
    public Optional<Path> currentDocument(ContractEntry entry) {
        return latest(entry, DocumentKind.SIGNED_SCAN, DocumentKind.SIGNED_MOBILE)
                .or(() -> latest(entry, DocumentKind.ORIGINAL))
                .map(ContractDocument::path)
                .map(pdfs::locate);
    }

    public Optional<Path> originalPdf(ContractEntry entry) {
        return latest(entry, DocumentKind.ORIGINAL)
                .map(ContractDocument::path)
                .map(pdfs::locate);
    }

    private static Optional<ContractDocument> latest(ContractEntry entry, DocumentKind... kinds) {
        Set<DocumentKind> wanted = EnumSet.copyOf(List.of(kinds));
        return entry.documents().stream()
                .filter(document -> wanted.contains(document.kind()))
                .reduce((first, second) -> second);
    }
}
