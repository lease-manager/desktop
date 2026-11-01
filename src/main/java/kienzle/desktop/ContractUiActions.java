package kienzle.desktop;

import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.pdf.QrPayload;
import kienzle.desktop.contract.ContractStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Thin application-layer facade for UI actions that orchestrate workflow calls. */
public final class ContractUiActions {
    private final ContractWorkflow workflow;

    public ContractUiActions(ContractWorkflow workflow) {
        this.workflow = workflow;
    }

    public ContractEntry save(ContractEntry current, LeaseContract contract) {
        return workflow.save(current, contract);
    }

    public void delete(ContractEntry current) {
        workflow.delete(current);
    }

    public void archive(ContractEntry current) {
        workflow.archive(current);
    }

    public ContractEntry createPdf(ContractEntry current, LeaseContract contract) throws IOException {
        // Only drafts can be edited; a contract that already has a PDF is regenerated from its stored data.
        boolean editable = current == null || current.status() == ContractStatus.DRAFT;
        return workflow.createPdf(editable ? workflow.save(current, contract) : current);
    }

    public Optional<Path> originalPdf(ContractEntry current) {
        return workflow.originalPdf(current);
    }

    public Optional<Path> currentDocument(ContractEntry current) {
        return workflow.currentDocument(current);
    }

    public Optional<QrPayload> detectScanTarget(Path scan) throws IOException {
        return workflow.detectScanTarget(scan);
    }

    public boolean isCurrentScan(ContractEntry entry, QrPayload payload) throws IOException {
        return workflow.isCurrentScan(entry, payload);
    }

    public ContractEntry attachScan(ContractEntry target, Path scan) throws IOException {
        return workflow.attachScan(target, scan);
    }

    public ContractEntry importSignedReturn(Path file) throws IOException {
        return workflow.importSignedReturn(file);
    }

    public List<ContractEntry> awaitingSignature() {
        return workflow.list().stream()
                .filter(entry -> entry.status() == ContractStatus.AWAITING_SIGNATURE)
                .toList();
    }

    public ContractEntry markAwaitingSignature(ContractEntry current) {
        return workflow.markAwaitingSignature(current);
    }

    public ContractEntry revokeAwaitingSignature(ContractEntry current) {
        return workflow.revokeAwaitingSignature(current);
    }
}
