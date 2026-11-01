package kienzle.desktop.app;

import kienzle.desktop.contract.ContractDocument;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.ContractStatus;
import kienzle.desktop.contract.DocumentKind;
import kienzle.desktop.contract.SignatureSource;
import kienzle.desktop.pdf.Hashing;
import kienzle.desktop.pdf.PdfStorage;
import org.apache.pdfbox.Loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/** Attaches a document signed on a mobile device ({@code .leasesigned}) to its contract. */
public class SignedReturnImportService {
    private final ContractService contracts;
    private final PdfStorage storage;
    private final SignedReturnCodec codec = new SignedReturnCodec();

    public SignedReturnImportService(ContractService contracts, PdfStorage storage) {
        this.contracts = contracts;
        this.storage = storage;
    }

    public ContractEntry importSigned(Path file) throws IOException {
        SignedReturnCodec.SignedReturn signed = codec.read(file);
        SignedReturnCodec.Manifest manifest = signed.manifest();
        if (!SignedReturnCodec.SCHEMA_VERSION.equals(manifest.schemaVersion())) {
            throw new IOException("Unsupported signed return schema: " + manifest.schemaVersion());
        }
        String actualHash = Hashing.sha256(signed.pdfBytes());
        if (!actualHash.equals(manifest.signedPdfSha256())) {
            throw new IOException("Signed PDF hash mismatch");
        }
        try (var ignored = Loader.loadPDF(signed.pdfBytes())) {
            // Rejects content that is not a readable PDF.
        } catch (IOException e) {
            throw new IOException("Signed document is not a valid PDF", e);
        }

        ContractEntry entry = contracts.list().stream()
                .filter(candidate -> candidate.id().equals(manifest.contractId()))
                .findFirst()
                .orElseThrow(() -> new IOException("Unknown contract " + manifest.contractId()));
        if (entry.status() != ContractStatus.AWAITING_SIGNATURE) {
            throw new IOException("Contract is not awaiting a signature: " + entry.status());
        }
        String originalHash = entry.documents().stream()
                .filter(document -> document.kind() == DocumentKind.ORIGINAL)
                .reduce((first, second) -> second)
                .map(ContractDocument::sha256)
                .orElseThrow(() -> new IOException("Contract has no original PDF"));
        if (!Objects.equals(originalHash, manifest.originalPdfSha256())) {
            throw new IOException("Signed return was made from a different version of the contract");
        }

        String path = storage.store(entry.id(), "signed-mobile-" + UUID.randomUUID() + ".pdf", signed.pdfBytes());
        return contracts.attachSigned(entry.id(), SignatureSource.MOBILE, path, actualHash,
                blankToNull(manifest.signerName()), blankToNull(manifest.deviceInfo()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
