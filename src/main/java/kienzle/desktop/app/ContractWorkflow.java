package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.contract.ContractDocument;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.DocumentKind;
import kienzle.desktop.pdf.ContractPdfService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Facade for the UI: one entry point for contract persistence, PDF creation and status changes. */
public class ContractWorkflow {
    private final ContractService contracts;
    private final ContractPdfService pdfs;

    public ContractWorkflow(ContractService contracts, ContractPdfService pdfs) {
        this.contracts = contracts;
        this.pdfs = pdfs;
    }

    public List<ContractEntry> list() {
        return contracts.list();
    }

    public List<ContractEntry> listArchived() {
        return contracts.listArchived();
    }

    /** Creates a new draft, or updates {@code current} when it is not {@code null}. */
    public ContractEntry save(ContractEntry current, LeaseContract contract) {
        return current == null ? contracts.create(contract) : contracts.update(current.id(), contract);
    }

    public void archive(ContractEntry entry) {
        contracts.archive(entry.id());
    }

    public ContractEntry restore(ContractEntry entry) {
        return contracts.restore(entry.id());
    }

    public ContractEntry createPdf(ContractEntry entry) throws IOException {
        return pdfs.createOriginalPdf(entry);
    }

    public ContractEntry markAwaitingSignature(ContractEntry entry) {
        return contracts.markAwaitingSignature(entry.id());
    }

    public Optional<Path> originalPdf(ContractEntry entry) {
        return entry.documents().stream()
                .filter(document -> document.kind() == DocumentKind.ORIGINAL)
                .reduce((first, second) -> second)
                .map(ContractDocument::path)
                .map(pdfs::locate);
    }
}
