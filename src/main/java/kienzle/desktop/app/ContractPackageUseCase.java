package kienzle.desktop.app;

import kienzle.desktop.contract.ContractEntry;

import java.io.IOException;
import java.nio.file.Path;

/** Encapsulates package import/export operations behind a single use-case boundary. */
public final class ContractPackageUseCase {
    private final ContractPackageService packages;

    public ContractPackageUseCase(ContractPackageService packages) {
        this.packages = packages;
    }

    public ContractPackageService.ContractPackage exportPackage(ContractEntry entry, Path targetFile) throws IOException {
        return packages.exportPackage(entry, targetFile);
    }

    public ContractEntry importPackage(Path packageFile) throws IOException {
        return packages.importPackage(packageFile);
    }

    public ContractPackageService.ContractPackage readPackage(Path packageFile) throws IOException {
        return packages.readPackage(packageFile);
    }

    public ContractEntry importSignedReturn(Path file) throws IOException {
        return packages.importSignedReturn(file);
    }
}
