package kienzle.desktop.pdf;

import kienzle.desktop.ContractPdfValues;
import kienzle.desktop.LeaseContractPdfPreview;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/** Renders the contract PDF, stamps QR code + hash, stores it and moves the contract to PDF_CREATED. */
public class ContractPdfService {
    private final ContractService contracts;
    private final LeaseContractPdfPreview renderer;
    private final PdfStamper stamper;
    private final PdfStorage storage;

    public ContractPdfService(ContractService contracts, LeaseContractPdfPreview renderer,
                              PdfStamper stamper, PdfStorage storage) {
        this.contracts = contracts;
        this.renderer = renderer;
        this.stamper = stamper;
        this.storage = storage;
    }

    public ContractEntry createOriginalPdf(ContractEntry entry) throws IOException {
        byte[] raw = renderer.generatePdf(ContractPdfValues.from(entry.contract()));
        StampedPdf stamped = stamper.stamp(raw, entry.id());
        String path = storage.store(entry.id(), "original-" + UUID.randomUUID() + ".pdf", stamped.bytes());
        return contracts.registerOriginalPdf(entry.id(), path, stamped.sha256());
    }

    public Path locate(String relativePath) {
        return storage.resolve(relativePath);
    }

    public ScanImportService scanImporter() {
        return new ScanImportService(contracts, storage);
    }
}
