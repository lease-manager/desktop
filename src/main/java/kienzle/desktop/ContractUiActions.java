package kienzle.desktop;

import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Thin application-layer facade for UI actions that orchestrate workflow calls. */
public final class ContractUiActions {
    private final ContractWorkflow workflow;

    public ContractUiActions(ContractWorkflow workflow) {
        this.workflow = workflow;
    }

    public ContractEntry save(ContractEntry current, LeaseContract contract) {
        return workflow.save(current, contract);
    }

    public void archive(ContractEntry current) {
        workflow.archive(current);
    }

    public ContractEntry createPdf(ContractEntry current, LeaseContract contract) throws IOException {
        return workflow.createPdf(workflow.save(current, contract));
    }

    public Optional<Path> originalPdf(ContractEntry current) {
        return workflow.originalPdf(current);
    }

    public Optional<Path> currentDocument(ContractEntry current) {
        return workflow.currentDocument(current);
    }

    public Optional<UUID> detectScanTarget(Path scan) throws IOException {
        return workflow.detectScanTarget(scan);
    }

    public ContractEntry attachScan(ContractEntry target, Path scan) throws IOException {
        return workflow.attachScan(target, scan);
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
