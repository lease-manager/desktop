package kienzle.desktop.app;

import kienzle.desktop.contract.ContractDocument;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.Hashing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class LeasePackageExportService {
    private final Path dataDir;
    private final LeasePackageCodec codec;

    public LeasePackageExportService() {
        this(JsonContractRepository.defaultDataDir());
    }

    public LeasePackageExportService(Path dataDir) {
        this(dataDir, new ZipLeasePackageCodec());
    }

    public LeasePackageExportService(Path dataDir, LeasePackageCodec codec) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.codec = codec;
    }

    public ContractPackageService.ContractPackage exportPackage(ContractEntry entry, Path targetFile) throws IOException {
        ContractDocument original = entry.documents().stream()
                .filter(document -> document.kind().name().startsWith("ORIGINAL"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("Contract " + entry.id() + " has no original PDF"));

        Path pdfPath = resolveRelativePath(original.path());
        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        String sha256 = Hashing.sha256(pdfBytes);

        ContractPackageService.LeasePackageManifest manifest = new ContractPackageService.LeasePackageManifest(
                ContractPackageService.PACKAGE_SCHEMA_VERSION,
                entry.id(),
                Instant.now(),
                sha256,
                entry.contract().name(),
                LeasePackageSerializer.serialize(entry.contract()),
                "");
        String signature = PackageSecurity.sign(dataDir, manifest);
        ContractPackageService.LeasePackageManifest signedManifest = new ContractPackageService.LeasePackageManifest(
                manifest.schemaVersion(),
                manifest.contractId(),
                manifest.createdAt(),
                manifest.originalPdfSha256(),
                manifest.contractName(),
                manifest.contractJson(),
                signature);

        Files.createDirectories(targetFile.toAbsolutePath().getParent());
        codec.writePackage(targetFile, signedManifest, pdfBytes);
        return new ContractPackageService.ContractPackage(signedManifest, pdfBytes);
    }

    private Path resolveRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Contract document path is missing");
        }
        Path path = Path.of(relativePath);
        return path.isAbsolute() ? path : dataDir.resolve(path).normalize();
    }
}
