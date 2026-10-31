package kienzle.desktop.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {
    List<ContractEntry> findAll();

    List<ContractEntry> findArchived();

    Optional<ContractEntry> findById(UUID id);

    void save(ContractEntry entry);

    void archive(ContractEntry entry);

    ContractEntry restore(UUID id);

    /** Permanently removes the contract record and its documents. */
    void delete(UUID id);
}
