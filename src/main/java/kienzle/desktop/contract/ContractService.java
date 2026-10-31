package kienzle.desktop.contract;

import kienzle.desktop.LeaseContract;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Workflow rules: DRAFT → PDF_CREATED → AWAITING_SIGNATURE → SIGNED; AWAITING_SIGNATURE can be revoked. */
public class ContractService {
    private final ContractRepository repository;
    private final Clock clock;

    public ContractService(ContractRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public ContractService(ContractRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ContractEntry> list() {
        return repository.findAll();
    }

    public List<ContractEntry> listArchived() {
        return repository.findArchived();
    }

    public ContractEntry create(LeaseContract contract) {
        ContractEntry entry = ContractEntry.draft(contract, clock.instant());
        repository.save(entry);
        return entry;
    }

    public ContractEntry update(UUID id, LeaseContract contract) {
        ContractEntry entry = get(id);
        if (entry.status() != ContractStatus.DRAFT) {
            throw new IllegalStateException("Only drafts can be edited, status is " + entry.status());
        }
        return store(entry.withContract(contract, clock.instant()));
    }

    public ContractEntry registerOriginalPdf(UUID id, String relativePath, String sha256) {
        ContractEntry entry = get(id);
        requireStatus(entry, ContractStatus.DRAFT, ContractStatus.PDF_CREATED);
        return store(entry.withDocument(document(id, DocumentKind.ORIGINAL, relativePath, sha256), clock.instant())
                .withStatus(ContractStatus.PDF_CREATED, clock.instant()));
    }

    public ContractEntry markAwaitingSignature(UUID id) {
        ContractEntry entry = get(id);
        requireStatus(entry, ContractStatus.PDF_CREATED);
        return store(entry.withStatus(ContractStatus.AWAITING_SIGNATURE, clock.instant()));
    }

    public ContractEntry revokeAwaitingSignature(UUID id) {
        ContractEntry entry = get(id);
        requireStatus(entry, ContractStatus.AWAITING_SIGNATURE);
        return store(entry.withStatus(ContractStatus.PDF_CREATED, clock.instant()));
    }

    public ContractEntry attachSigned(UUID id, SignatureSource source, String relativePath, String sha256,
                                      String signerName, String deviceInfo) {
        ContractEntry entry = get(id);
        requireStatus(entry, ContractStatus.AWAITING_SIGNATURE);
        DocumentKind kind = source == SignatureSource.MOBILE ? DocumentKind.SIGNED_MOBILE : DocumentKind.SIGNED_SCAN;
        var now = clock.instant();
        return store(entry.withDocument(document(id, kind, relativePath, sha256), now)
                .withSignature(new SignatureInfo(id, source, now, signerName, deviceInfo), now)
                .withStatus(ContractStatus.SIGNED, now));
    }

    public void archive(UUID id) {
        repository.archive(get(id));
    }

    public ContractEntry restore(UUID id) {
        return repository.restore(id);
    }

    private ContractEntry get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown contract " + id));
    }

    private ContractEntry store(ContractEntry entry) {
        repository.save(entry);
        return entry;
    }

    private ContractDocument document(UUID id, DocumentKind kind, String path, String sha256) {
        return new ContractDocument(UUID.randomUUID(), id, kind, path, sha256, clock.instant());
    }

    private void requireStatus(ContractEntry entry, ContractStatus... allowed) {
        for (ContractStatus s : allowed) {
            if (entry.status() == s) {
                return;
            }
        }
        throw new IllegalStateException("Invalid status " + entry.status() + " for this operation");
    }
}
