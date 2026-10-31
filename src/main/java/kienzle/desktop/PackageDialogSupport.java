package kienzle.desktop;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kienzle.desktop.app.ContractPackageService;
import kienzle.desktop.app.ContractWorkflow;

import java.io.IOException;
import java.nio.file.Path;

/** Centralizes package dialog orchestration so the main controller stays focused on navigation/state. */
public final class PackageDialogSupport {
    private PackageDialogSupport() {
    }

    public static FileChooser buildChooser(String title, String initialFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("package.filetype"), "*.leasepack"));
        if (initialFileName != null && !initialFileName.isBlank()) {
            chooser.setInitialFileName(initialFileName);
        }
        return chooser;
    }

    public static boolean confirmImport(ContractWorkflow workflow, Stage owner, Path packageFile) {
        try {
            ContractPackageService.ContractPackage pkg = workflow.readPackage(packageFile);
            PackageImportDialog dialog = new PackageImportDialog(owner, pkg);
            return dialog.showAndWait();
        } catch (IOException | RuntimeException exception) {
            new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
            return false;
        }
    }
}
