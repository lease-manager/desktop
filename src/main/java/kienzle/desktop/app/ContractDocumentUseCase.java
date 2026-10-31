package kienzle.desktop.app;

import kienzle.desktop.contract.ContractDocument;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.DocumentKind;
import kienzle.desktop.pdf.ContractPdfService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Encapsulates document and PDF-specific operations for a contract. */
public final class ContractDocumentUseCase {
    private final ContractPdfService pdfs;

    public ContractDocumentUseCase(ContractPdfService pdfs) {
        this.pdfs = pdfs;
    }

    public ContractEntry createOriginalPdf(ContractEntry entry) throws IOException {
        return pdfs.createOriginalPdf(entry);
    }

    public Optional<Path> originalPdf(ContractEntry entry) {
        return entry.documents().stream()
                .filter(document -> document.kind() == DocumentKind.ORIGINAL)
                .reduce((first, second) -> second)
                .map(ContractDocument::path)
                .map(pdfs::locate);
    }
}
