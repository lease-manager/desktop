package kienzle.desktop;

import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;

import java.io.IOException;
import java.nio.file.Path;
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

    public void archive(ContractEntry current) {
        workflow.archive(current);
    }

    public ContractEntry createPdf(ContractEntry current, LeaseContract contract) throws IOException {
        return workflow.createPdf(workflow.save(current, contract));
    }

    public Optional<Path> originalPdf(ContractEntry current) {
        return workflow.originalPdf(current);
    }

    public ContractEntry markAwaitingSignature(ContractEntry current) {
        return workflow.markAwaitingSignature(current);
    }

    public ContractEntry revokeAwaitingSignature(ContractEntry current) {
        return workflow.revokeAwaitingSignature(current);
    }
}
