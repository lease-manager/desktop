package kienzle.desktop.app;

import kienzle.desktop.LeaseContract;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.Hashing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LeasePackageImportService {
    private final Path dataDir;
    private final LeasePackageCodec codec;
    private final ContractService contracts;

    public LeasePackageImportService(ContractService contracts) {
        this(contracts, JsonContractRepository.defaultDataDir(), new ZipLeasePackageCodec());
    }

    public LeasePackageImportService(ContractService contracts, Path dataDir) {
        this(contracts, dataDir, new ZipLeasePackageCodec());
    }

    public LeasePackageImportService(ContractService contracts, Path dataDir, LeasePackageCodec codec) {
        this.contracts = contracts;
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.codec = codec;
    }

    public ContractEntry importPackage(Path packageFile) throws IOException {
        ContractPackageService.ContractPackage imported = codec.readPackage(packageFile);
        imported.validate();

        LeaseContract contract = imported.manifest().contract();
        ContractEntry created = contracts.create(contract);
        String relativePdfPath = storeImportedPdf(created.id(), imported.pdfBytes());
        return contracts.registerOriginalPdf(created.id(), relativePdfPath, Hashing.sha256(imported.pdfBytes()));
    }

    public String storeImportedPdf(UUID contractId, byte[] pdfBytes) throws IOException {
        Path target = dataDir.resolve("documents").resolve(contractId.toString()).resolve("imported-original.pdf");
        Files.createDirectories(target.getParent());
        Files.write(target, pdfBytes);
        return dataDir.relativize(target).toString();
    }
}
