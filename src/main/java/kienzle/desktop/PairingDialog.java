package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kienzle.desktop.app.PairingService;
import kienzle.desktop.pdf.QrCodes;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

/** Modal window to pair this installation with a mobile device and to manage the paired devices. */
final class PairingDialog {
    private static final int QR_SIZE = 240;

    private final PairingService pairing;
    private final UiFeedback feedback = new UiFeedback();
    private final Stage stage = new Stage();
    private final ListView<PairingService.Peer> peers = new ListView<>();
    private final TextArea codeInput = new TextArea();

    PairingDialog(PairingService pairing, Stage owner) {
        this.pairing = pairing;
        stage.setTitle(I18n.get("pairing.title"));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setScene(new Scene(createContent(), 520, 700));
    }

    void showAndWait() {
        refreshPeers();
        stage.showAndWait();
    }

    private VBox createContent() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(14));
        root.getChildren().add(new Label(I18n.get("pairing.own")));
        try {
            ImageView qr = new ImageView(toFxImage(QrCodes.encode(pairing.ownPairingCode(), QR_SIZE)));
            Label fingerprint = new Label(I18n.get("pairing.fingerprint") + " " + pairing.ownFingerprint());
            root.getChildren().addAll(qr, fingerprint);
        } catch (IOException e) {
            root.getChildren().add(new Label(I18n.get("pairing.error") + e.getMessage()));
        }

        root.getChildren().add(new Label(I18n.get("pairing.peer")));
        codeInput.setPromptText(I18n.get("pairing.paste"));
        codeInput.setPrefRowCount(3);
        codeInput.setWrapText(true);
        Button accept = new Button(I18n.get("pairing.accept"));
        accept.setOnAction(event -> accept(codeInput.getText()));
        Button fromImage = new Button(I18n.get("pairing.fromImage"));
        fromImage.setOnAction(event -> acceptFromImage());
        root.getChildren().addAll(codeInput, new HBox(8, accept, fromImage));

        root.getChildren().add(new Label(I18n.get("pairing.paired")));
        peers.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PairingService.Peer peer, boolean empty) {
                super.updateItem(peer, empty);
                setText(empty || peer == null ? null : peer.name() + "\n" + peer.fingerprint());
            }
        });
        Button remove = new Button(I18n.get("pairing.remove"));
        remove.setOnAction(event -> removeSelected());
        root.getChildren().addAll(peers, remove);
        return root;
    }

    private void acceptFromImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("pairing.fromImage"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG/JPEG", "*.png", "*.jpg", "*.jpeg"));
        java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            BufferedImage image = javax.imageio.ImageIO.read(file);
            Optional<String> code = image == null ? Optional.empty() : QrCodes.decode(image);
            if (code.isEmpty()) {
                feedback.showError(I18n.get("pairing.noQr"));
                return;
            }
            accept(code.get());
        } catch (IOException e) {
            feedback.showError(I18n.get("pairing.error") + e.getMessage());
        }
    }

    private void accept(String code) {
        try {
            PairingService.Peer peer = pairing.acceptPairingCode(code);
            codeInput.clear();
            refreshPeers();
            feedback.showInfo(I18n.get("pairing.done") + peer.name() + "\n" + peer.fingerprint());
        } catch (IOException e) {
            feedback.showError(I18n.get("pairing.error") + e.getMessage());
        }
    }

    private void removeSelected() {
        PairingService.Peer peer = peers.getSelectionModel().getSelectedItem();
        if (peer == null || !feedback.confirm(I18n.get("pairing.remove.confirm") + peer.name())) {
            return;
        }
        try {
            pairing.remove(peer.id());
            refreshPeers();
        } catch (IOException e) {
            feedback.showError(I18n.get("pairing.error") + e.getMessage());
        }
    }

    private void refreshPeers() {
        try {
            peers.setItems(FXCollections.observableArrayList(pairing.peers()));
        } catch (IOException e) {
            feedback.showError(I18n.get("pairing.error") + e.getMessage());
        }
    }

    private static WritableImage toFxImage(BufferedImage source) {
        WritableImage image = new WritableImage(source.getWidth(), source.getHeight());
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                writer.setArgb(x, y, source.getRGB(x, y));
            }
        }
        return image;
    }
}
