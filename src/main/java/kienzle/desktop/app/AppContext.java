package kienzle.desktop.app;

import kienzle.desktop.LeaseContractPdfPreview;
import kienzle.desktop.contract.ContractService;
import kienzle.desktop.contract.JsonContractRepository;
import kienzle.desktop.pdf.ContractPdfService;
import kienzle.desktop.pdf.PdfStamper;
import kienzle.desktop.pdf.PdfStorage;

import java.nio.file.Path;

/** Composition root: wires the services once and hands them to the UI (constructor injection). */
public record AppContext(ContractWorkflow workflow) {

    public static AppContext create(Path dataDir) {
        ContractService contracts = new ContractService(new JsonContractRepository(dataDir));
        ContractPdfService pdfs = new ContractPdfService(contracts, new LeaseContractPdfPreview(),
                new PdfStamper(), new PdfStorage(dataDir));
        return new AppContext(new ContractWorkflow(contracts, pdfs));
    }

    public static AppContext createDefault() {
        return create(JsonContractRepository.defaultDataDir());
    }
}
