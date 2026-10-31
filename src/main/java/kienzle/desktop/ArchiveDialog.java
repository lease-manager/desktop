package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;

import java.util.Comparator;
import java.util.function.Consumer;

/** Modal window listing archived contracts; the selected one can be restored. */
final class ArchiveDialog {
    private final ContractWorkflow workflow;
    private final Consumer<ContractEntry> onRestored;
    private final Stage stage = new Stage();
    private final ListView<ContractEntry> list = new ListView<>();
    private final Button restoreButton = new Button(I18n.get("contract.archive.restore"));

    ArchiveDialog(ContractWorkflow workflow, Stage owner, Consumer<ContractEntry> onRestored) {
        this.workflow = workflow;
        this.onRestored = onRestored;
        stage.setTitle(I18n.get("contract.archive.title"));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setScene(new Scene(buildContent(), 460.0, 420.0));
    }

    void showAndWait() {
        list.setItems(FXCollections.observableArrayList(workflow.listArchived().stream()
                .sorted(Comparator.comparing(ContractEntry::createdAt).reversed())
                .toList()));
        stage.showAndWait();
    }

    private VBox buildContent() {
        list.setPlaceholder(new Label(I18n.get("contract.archive.empty")));
        list.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ContractEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null
                        : entry.contract().name() + "\n" + ContractStatusText.describe(entry));
            }
        });
        restoreButton.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        restoreButton.setOnAction(event -> restoreSelected());
        Button closeButton = new Button(I18n.get("dialog.close"));
        closeButton.setOnAction(event -> stage.close());

        VBox content = new VBox(10.0, new Label(I18n.get("contract.archive.description")), list,
                new HBox(8.0, restoreButton, closeButton));
        content.setPadding(new Insets(14.0));
        VBox.setVgrow(list, Priority.ALWAYS);
        return content;
    }

    private void restoreSelected() {
        ContractEntry selected = list.getSelectionModel().getSelectedItem();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, I18n.get("contract.archive.restore.confirm"));
        confirm.initOwner(stage);
        if (selected == null || confirm.showAndWait().filter(button -> button == ButtonType.OK).isEmpty()) {
            return;
        }
        try {
            onRestored.accept(workflow.restore(selected));
            stage.close();
        } catch (RuntimeException exception) {
            new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
        }
    }
}
