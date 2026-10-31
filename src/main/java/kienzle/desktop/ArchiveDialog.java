package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import java.util.List;
import java.util.function.Consumer;

/** Modal window listing archived contracts; the selected one can be restored. */
final class ArchiveDialog {
    private static final double WINDOW_WIDTH = 460.0;
    private static final double WINDOW_HEIGHT = 420.0;

    private final ContractWorkflow workflow;
    private final Consumer<ContractEntry> onRestored;
    private final UiFeedback feedback = new UiFeedback();
    private final Stage stage = new Stage();
    private final ListView<ContractEntry> list = new ListView<>();
    private final Button restoreButton = new Button(I18n.get("contract.archive.restore"));

    ArchiveDialog(ContractWorkflow workflow, Stage owner, Consumer<ContractEntry> onRestored) {
        this.workflow = workflow;
        this.onRestored = onRestored;
        configureStage(owner);
        stage.setScene(new Scene(createContent(), WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    void showAndWait() {
        refreshEntries();
        stage.showAndWait();
    }

    private void configureStage(Stage owner) {
        stage.setTitle(I18n.get("contract.archive.title"));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
    }

    private void refreshEntries() {
        list.setItems(FXCollections.observableArrayList(archivedEntries()));
        list.getSelectionModel().clearSelection();
    }

    private List<ContractEntry> archivedEntries() {
        return workflow.listArchived().stream()
                .sorted(Comparator.comparing(ContractEntry::createdAt).reversed())
                .toList();
    }

    private static String entrySummary(ContractEntry entry) {
        return entry.contract().name() + "\n" + ContractStatusText.describe(entry);
    }

    private VBox createContent() {
        configureList();
        configureActions();
        return createArchivePanel();
    }

    private VBox createArchivePanel() {
        VBox content = new VBox(10.0,
                new Label(I18n.get("contract.archive.description")),
                list,
                new HBox(8.0, restoreButton, closeButton()));
        content.setPadding(new Insets(14.0));
        VBox.setVgrow(list, Priority.ALWAYS);
        return content;
    }

    private void configureList() {
        list.setPlaceholder(new Label(I18n.get("contract.archive.empty")));
        list.setCellFactory(listView -> new ArchiveListCell());
        bindRestoreButton();
    }

    private void bindRestoreButton() {
        restoreButton.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
    }

    private void configureActions() {
        restoreButton.setOnAction(event -> restoreSelected());
    }

    private final class ArchiveListCell extends ListCell<ContractEntry> {
        @Override
        protected void updateItem(ContractEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setText(null);
                return;
            }
            setText(entrySummary(entry));
        }
    }

    private Button closeButton() {
        Button closeButton = new Button(I18n.get("dialog.close"));
        closeButton.setOnAction(event -> stage.close());
        return closeButton;
    }

    private void restoreSelected() {
        ContractEntry selected = selectedEntry();
        if (selected == null) {
            return;
        }
        restoreEntry(selected);
    }

    private ContractEntry selectedEntry() {
        return list.getSelectionModel().getSelectedItem();
    }

    private void restoreEntry(ContractEntry selected) {
        if (!confirmRestore()) {
            return;
        }
        try {
            ContractEntry restored = workflow.restore(selected);
            onRestored.accept(restored);
            close();
        } catch (RuntimeException exception) {
            reportRestoreFailure(exception);
        }
    }

    private boolean confirmRestore() {
        return feedback.confirm(I18n.get("contract.archive.restore.confirm"));
    }

    private void reportRestoreFailure(RuntimeException exception) {
        feedback.showError(exception.getMessage());
    }

    private void close() {
        stage.close();
    }
}
