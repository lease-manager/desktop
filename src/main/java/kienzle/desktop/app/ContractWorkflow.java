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
import java.util.UUID;

/** Facade for the UI: one entry point for contract persistence, PDF creation and status changes. */
public class ContractWorkflow {
    private final ContractLifecycleUseCase lifecycle;
    private final ContractDocumentUseCase documents;
    private final ContractPackageUseCase packages;

    public ContractWorkflow(ContractService contracts, ContractPdfService pdfs) {
        this(contracts, pdfs, new ContractPackageService());
    }

    public ContractWorkflow(ContractService contracts, ContractPdfService pdfs, ContractPackageService packages) {
        this.lifecycle = new ContractLifecycleUseCase(contracts);
        this.documents = new ContractDocumentUseCase(pdfs);
        this.packages = new ContractPackageUseCase(packages);
    }

    public List<ContractEntry> list() {
        return lifecycle.list();
    }

    public List<ContractEntry> listArchived() {
        return lifecycle.listArchived();
    }

    public ContractEntry save(ContractEntry current, LeaseContract contract) {
        return lifecycle.save(current, contract);
    }

    public void archive(ContractEntry entry) {
        lifecycle.archive(entry);
    }

    public ContractEntry restore(ContractEntry entry) {
        return lifecycle.restore(entry);
    }

    public ContractEntry createPdf(ContractEntry entry) throws IOException {
        return documents.createOriginalPdf(entry);
    }

    public ContractEntry markAwaitingSignature(ContractEntry entry) {
        return lifecycle.markAwaitingSignature(entry);
    }

    public ContractEntry revokeAwaitingSignature(ContractEntry entry) {
        return lifecycle.revokeAwaitingSignature(entry);
    }

    public ContractPackageService.ContractPackage exportPackage(ContractEntry entry, Path targetFile) throws IOException {
        return packages.exportPackage(entry, targetFile);
    }

    public ContractEntry importPackage(Path packageFile) throws IOException {
        return packages.importPackage(packageFile);
    }

    public ContractPackageService.ContractPackage readPackage(Path packageFile) throws IOException {
        return packages.readPackage(packageFile);
    }

    public Optional<Path> originalPdf(ContractEntry entry) {
        return documents.originalPdf(entry);
    }

    public Optional<Path> currentDocument(ContractEntry entry) {
        return documents.currentDocument(entry);
    }

    public Optional<UUID> detectScanTarget(Path scan) throws IOException {
        return documents.detectScanTarget(scan);
    }

    public ContractEntry attachScan(ContractEntry entry, Path scan) throws IOException {
        return documents.attachScan(entry, scan);
    }
}
