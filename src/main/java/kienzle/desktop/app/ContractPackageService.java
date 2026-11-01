package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.Hashing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ContractPackageService {
    /** Legacy format, signed with the local HMAC key. */
    public static final String PACKAGE_SCHEMA_VERSION = "leasepack:v1";
    /** Current format, signed with the Ed25519 key of the sending device. */
    public static final String PACKAGE_SCHEMA_V2 = "leasepack:v2";

    private final Path dataDir;
    private final LeasePackageExportService exportService;
    private final LeasePackageImportService importService;
    private final LeasePackageCodec codec;
    private final PairingService pairing;
    private final SignedReturnImportService signedReturns;

    public ContractPackageService() {
        this(JsonContractRepository.defaultDataDir());
    }

    public ContractPackageService(Path dataDir) {
        this(dataDir, new ZipLeasePackageCodec());
    }

    public ContractPackageService(Path dataDir, LeasePackageCodec codec) {
        this(dataDir, codec, new PairingService(dataDir, "Desktop"));
    }

    public ContractPackageService(Path dataDir, LeasePackageCodec codec, PairingService pairing) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.codec = codec;
        this.pairing = pairing;
        kienzle.desktop.contract.ContractService contracts =
                new kienzle.desktop.contract.ContractService(new JsonContractRepository(dataDir));
        this.signedReturns = new SignedReturnImportService(contracts, new kienzle.desktop.pdf.PdfStorage(this.dataDir), pairing);
        this.exportService = new LeasePackageExportService(dataDir, codec, pairing);
        this.importService = new LeasePackageImportService(new kienzle.desktop.contract.ContractService(
                new JsonContractRepository(dataDir)), dataDir, codec, pairing);
    }

    public ContractPackage exportPackage(ContractEntry entry, Path targetFile) throws IOException {
        return exportService.exportPackage(entry, targetFile);
    }

    public ContractPackage readPackage(Path packageFile) throws IOException {
        ContractPackage packageData = codec.readPackage(packageFile);
        packageData.validate();
        PackageSecurity.validate(dataDir, pairing, packageData.manifest());
        return packageData;
    }

    public ContractEntry importPackage(Path packageFile) throws IOException {
        return importService.importPackage(packageFile);
    }

    public ContractEntry importSignedReturn(Path file) throws IOException {
        return signedReturns.importSigned(file);
    }

    public Path dataDir() {
        return dataDir;
    }

    public String storeImportedPdf(UUID contractId, byte[] pdfBytes) throws IOException {
        Path target = dataDir.resolve("documents").resolve(contractId.toString()).resolve("imported-original.pdf");
        Files.createDirectories(target.getParent());
        Files.write(target, pdfBytes);
        return dataDir.relativize(target).toString();
    }

    public record LeasePackageManifest(
            String schemaVersion,
            UUID contractId,
            Instant createdAt,
            String originalPdfSha256,
            String contractName,
            String contractJson,
            String packageSignature,
            String senderId
    ) {
        public LeasePackageManifest {
            Objects.requireNonNull(schemaVersion, "schemaVersion");
            Objects.requireNonNull(contractId, "contractId");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(originalPdfSha256, "originalPdfSha256");
            Objects.requireNonNull(contractName, "contractName");
            Objects.requireNonNull(contractJson, "contractJson");
            packageSignature = packageSignature == null ? "" : packageSignature;
            senderId = senderId == null ? "" : senderId;
        }

        /** Legacy (v1) manifest without a sender. */
        public LeasePackageManifest(String schemaVersion, UUID contractId, Instant createdAt,
                                    String originalPdfSha256, String contractName, String contractJson,
                                    String packageSignature) {
            this(schemaVersion, contractId, createdAt, originalPdfSha256, contractName, contractJson,
                    packageSignature, "");
        }

        public LeaseContract contract() {
            return LeasePackageSerializer.deserialize(contractJson);
        }

        public String signedPayload() {
            String base = String.join("|",
                    schemaVersion,
                    contractId.toString(),
                    createdAt.toString(),
                    originalPdfSha256,
                    contractName,
                    contractJson);
            return PACKAGE_SCHEMA_V2.equals(schemaVersion) ? base + "|" + senderId : base;
        }

        public LeasePackageManifest withoutSignature() {
            return new LeasePackageManifest(schemaVersion, contractId, createdAt, originalPdfSha256, contractName,
                    contractJson, "", senderId);
        }
    }

    public record ContractPackage(LeasePackageManifest manifest, byte[] pdfBytes) {
        public void validate() throws IOException {
            if (!PACKAGE_SCHEMA_VERSION.equals(manifest.schemaVersion())
                    && !PACKAGE_SCHEMA_V2.equals(manifest.schemaVersion())) {
                throw new IOException("Unsupported package schema: " + manifest.schemaVersion());
            }
            String actualHash = Hashing.sha256(pdfBytes);
            if (!Objects.equals(actualHash, manifest.originalPdfSha256())) {
                throw new IOException("Package hash mismatch for contract " + manifest.contractId());
            }
        }
    }
}
