package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;

import java.util.List;

/** Encapsulates the core lifecycle operations for contracts. */
public final class ContractLifecycleUseCase {
    private final ContractService contracts;

    public ContractLifecycleUseCase(ContractService contracts) {
        this.contracts = contracts;
    }

    public List<ContractEntry> list() {
        return contracts.list();
    }

    public List<ContractEntry> listArchived() {
        return contracts.listArchived();
    }

    public ContractEntry save(ContractEntry current, LeaseContract contract) {
        return current == null ? contracts.create(contract) : contracts.update(current.id(), contract);
    }

    public void archive(ContractEntry entry) {
        contracts.archive(entry.id());
    }

    public ContractEntry restore(ContractEntry entry) {
        return contracts.restore(entry.id());
    }

    public ContractEntry markAwaitingSignature(ContractEntry entry) {
        return contracts.markAwaitingSignature(entry.id());
    }

    public ContractEntry revokeAwaitingSignature(ContractEntry entry) {
        return contracts.revokeAwaitingSignature(entry.id());
    }
}
