package kienzle.desktop.contract;

import java.time.Instant;
import java.util.UUID;

/** A file belonging to a contract; {@code path} is relative to the data directory. */
public record ContractDocument(
        UUID id,
        UUID contractId,
        DocumentKind kind,
        String path,
        String sha256,
        Instant createdAt
) {
    public ContractDocument withPath(String newPath) {
        return new ContractDocument(id, contractId, kind, newPath, sha256, createdAt);
    }
}
