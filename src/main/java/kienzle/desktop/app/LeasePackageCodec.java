package kienzle.desktop.app;

import java.io.IOException;
import java.nio.file.Path;

public interface LeasePackageCodec {
    void writePackage(Path targetFile, ContractPackageService.LeasePackageManifest manifest, byte[] pdfBytes) throws IOException;

    ContractPackageService.ContractPackage readPackage(Path packageFile) throws IOException;
}
