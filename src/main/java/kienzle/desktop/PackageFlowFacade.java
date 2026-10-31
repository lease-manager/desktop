package kienzle.desktop;

import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;

import java.io.IOException;
import java.nio.file.Path;

/** Facade for the package import/export flow so the controller remains a thin event adapter. */
public final class PackageFlowFacade {
    private final ContractWorkflow workflow;
    private final UiFeedback feedback;

    public PackageFlowFacade(ContractWorkflow workflow, UiFeedback feedback) {
        this.workflow = workflow;
        this.feedback = feedback;
    }

    public void exportPackage(ContractEntry entry, Path targetFile) throws IOException {
        workflow.exportPackage(entry, targetFile);
        feedback.showInfo(I18n.get("package.export.success"));
    }

    public ContractEntry importPackage(Path packageFile) throws IOException {
        ContractEntry imported = workflow.importPackage(packageFile);
        feedback.showInfo(I18n.get("package.import.success") + " " + imported.contract().name());
        return imported;
    }
}
