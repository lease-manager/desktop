package kienzle.desktop;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kienzle.desktop.app.ContractPackageService;
import kienzle.desktop.app.ContractWorkflow;

import java.io.IOException;
import java.nio.file.Path;

/** Modal preview for importing a lease package. */
public final class PackageImportDialog {
    private final Stage stage = new Stage();
    private final ContractPackageService.ContractPackage pkg;

    public PackageImportDialog(Stage owner, ContractPackageService.ContractPackage pkg) {
        this.pkg = pkg;
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
    }

    public boolean showAndWait() {
        stage.setTitle(I18n.get("package.import.title"));
        Label title = new Label(pkg.manifest().contractName());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label summary = new Label(
                "ID: " + pkg.manifest().contractId() + "\n"
                        + "Schema: " + pkg.manifest().schemaVersion() + "\n"
                        + "Hash: " + pkg.manifest().originalPdfSha256().substring(0, 16) + "..."
        );
        final boolean[] shouldImport = {false};
        Button importButton = new Button(I18n.get("package.import"));
        Button cancelButton = new Button(I18n.get("dialog.close"));
        importButton.setOnAction(event -> {
            shouldImport[0] = true;
            stage.close();
        });
        cancelButton.setOnAction(event -> stage.close());

        VBox content = new VBox(12.0, title, summary, importButton, cancelButton);
        content.setPadding(new Insets(16.0));
        stage.setScene(new Scene(content, 420.0, 220.0));
        stage.showAndWait();
        return shouldImport[0];
    }
}
