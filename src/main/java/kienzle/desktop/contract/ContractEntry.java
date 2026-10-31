package kienzle.desktop.contract;

import kienzle.desktop.LeaseContract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Persisted aggregate: contract data plus workflow state, documents and signature. */
public record ContractEntry(
        UUID id,
        LeaseContract contract,
        ContractStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<ContractDocument> documents,
        SignatureInfo signature
) {
    public ContractEntry {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public static ContractEntry draft(LeaseContract contract, Instant now) {
        return new ContractEntry(UUID.randomUUID(), contract, ContractStatus.DRAFT, now, now, List.of(), null);
    }

    public ContractEntry withStatus(ContractStatus newStatus, Instant now) {
        return new ContractEntry(id, contract, newStatus, createdAt, now, documents, signature);
    }

    public ContractEntry withContract(LeaseContract newContract, Instant now) {
        return new ContractEntry(id, newContract, status, createdAt, now, documents, signature);
    }

    public ContractEntry withDocument(ContractDocument document, Instant now) {
        List<ContractDocument> all = new java.util.ArrayList<>(documents);
        all.add(document);
        return new ContractEntry(id, contract, status, createdAt, now, all, signature);
    }

    public ContractEntry withDocuments(List<ContractDocument> newDocuments) {
        return new ContractEntry(id, contract, status, createdAt, updatedAt, newDocuments, signature);
    }

    public ContractEntry withSignature(SignatureInfo newSignature, Instant now) {
        return new ContractEntry(id, contract, status, createdAt, now, documents, newSignature);
    }
}
