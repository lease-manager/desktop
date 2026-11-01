package kienzle.desktop.app;

import kienzle.desktop.LeaseContractPdfPreview;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.ContractPdfService;
import kienzle.desktop.pdf.PdfStamper;
import kienzle.desktop.pdf.PdfStorage;

import java.nio.file.Path;

/** Composition root: wires the services once and hands them to the UI (constructor injection). */
public record AppContext(ContractWorkflow workflow, ContractPackageService packages, PairingService pairing) {

    public Path dataDir() {
        return packages.dataDir();
    }

    public AppContext(ContractWorkflow workflow) {
        this(workflow, new ContractPackageService(), null);
    }

    public static AppContext create(Path dataDir) {
        ContractService contracts = new ContractService(new JsonContractRepository(dataDir));
        ContractPdfService pdfs = new ContractPdfService(contracts, new LeaseContractPdfPreview(),
                new PdfStamper(), new PdfStorage(dataDir));
        ContractPackageService packages = new ContractPackageService(dataDir);
        ContractWorkflow workflow = new ContractWorkflow(contracts, pdfs, packages);
        return new AppContext(workflow, packages, new PairingService(dataDir, deviceName()));
    }

    public static AppContext createDefault() {
        return create(JsonContractRepository.defaultDataDir());
    }

    private static String deviceName() {
        try {
            return "Desktop " + java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.io.IOException e) {
            return "Desktop";
        }
    }
}
