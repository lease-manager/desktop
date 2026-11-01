package kienzle.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kienzle.desktop.app.ReturnReceiver;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.pdf.QrCodes;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/** Window that receives signed contracts from paired phones over the local network while it is open. */
final class ReceiveDialog {
    private final Stage stage = new Stage();
    private final TextArea log = new TextArea();
    private final ReturnReceiver receiver;
    private final Consumer<ContractEntry> onImported;

    ReceiveDialog(ReturnReceiver.Importer importer, Stage owner, Consumer<ContractEntry> onImported)
            throws IOException {
        this.onImported = onImported;
        this.receiver = new ReturnReceiver(ReturnReceiver.DEFAULT_PORT, importer, this::onResult);
        stage.setTitle(I18n.get("receive.title"));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setScene(new Scene(createContent(), 480, 520));
        stage.setOnHidden(event -> receiver.close());
    }

    void showAndWait() {
        receiver.start();
        stage.showAndWait();
    }

    private VBox createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(14));
        root.getChildren().add(new Label(I18n.get("receive.info")));
        List<String> urls = receiver.urls();
        if (urls.isEmpty()) {
            root.getChildren().add(new Label(I18n.get("receive.nonetwork")));
        } else {
            root.getChildren().add(new ImageView(PairingDialog.toFxImage(QrCodes.encode(urls.get(0), 220))));
            for (String url : urls) {
                Label label = new Label(url);
                label.setStyle("-fx-font-family: monospace;");
                root.getChildren().add(label);
            }
        }
        log.setEditable(false);
        log.setWrapText(true);
        log.setPromptText(I18n.get("receive.waiting"));
        root.getChildren().addAll(new Label(I18n.get("receive.log")), log);
        return root;
    }

    private void onResult(ContractEntry entry, String error) {
        Platform.runLater(() -> {
            if (entry != null) {
                log.appendText(I18n.get("receive.ok") + entry.contract().name() + "\n");
                onImported.accept(entry);
            } else {
                log.appendText(I18n.get("receive.refused") + error + "\n");
            }
        });
    }
}
