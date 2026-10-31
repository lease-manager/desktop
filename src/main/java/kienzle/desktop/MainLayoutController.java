package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javafx.util.StringConverter;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.app.AppContext;
import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractStatus;

public class MainLayoutController {

    @FXML
    private ListView<ContractEntry> contractList;

    @FXML
    private ComboBox<StatusFilter> statusFilter;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button createPdfButton;

    @FXML
    private Button openPdfButton;

    @FXML
    private Button awaitingButton;

    @FXML
    private Button germanButton;

    @FXML
    private Button englishButton;

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblVerpaechterTxt;

    private final ObservableList<ContractEntry> entries = FXCollections.observableArrayList();
    private final FilteredList<ContractEntry> visibleEntries = new FilteredList<>(entries);
    @FXML
    private ContractFormController contractFormController;

    private final AppContext context;
    private final ContractWorkflow workflow;
    private ContractEntry currentEntry;

    public MainLayoutController(AppContext context) {
        this.context = context;
        this.workflow = context.workflow();
    }

    @FXML
    public void initialize() {
        germanButton.setGraphic(FlagIcons.germany());
        germanButton.setTooltip(new Tooltip(I18n.get("language.de")));
        englishButton.setGraphic(FlagIcons.unitedKingdom());
        englishButton.setTooltip(new Tooltip(I18n.get("language.en")));
        entries.setAll(workflow.list());
        statusFilter.setItems(FXCollections.observableArrayList(StatusFilter.values()));
        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(StatusFilter filter) {
                return filter == null ? "" : I18n.get("filter." + filter.name().toLowerCase(Locale.ROOT));
            }

            @Override
            public StatusFilter fromString(String text) {
                return null;
            }
        });
        statusFilter.valueProperty().addListener((obs, oldValue, filter) -> applyFilter(filter));
        statusFilter.setValue(StatusFilter.ALL);

        contractList.setItems(visibleEntries);
        contractList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ContractEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null : entry.contract().name() + "\n" + ContractStatusText.describe(entry));
            }
        });
        contractList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, entry) -> {
            if (entry != null) {
                selectEntry(entry);
            }
        });

        if (entries.isEmpty()) {
            startNewContract();
        } else {
            contractList.getSelectionModel().selectFirst();
        }
    }

    private void applyFilter(StatusFilter filter) {
        visibleEntries.setPredicate(filter == null ? StatusFilter.ALL : filter);
    }

    @FXML
    private void startNewContract() {
        contractList.getSelectionModel().clearSelection();
        currentEntry = null;
        showContract(ContractDefaults.newContract());
        updateEntryControls();
    }

    @FXML
    private void saveContract() {
        try {
            LeaseContract contract = contractFormController.read();
            show(workflow.save(currentEntry, contract));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
        }
    }

    @FXML
    private void archiveContract() {
        if (currentEntry == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, I18n.get("contract.archive.confirm"));
        if (confirm.showAndWait().filter(button -> button == javafx.scene.control.ButtonType.OK).isEmpty()) {
            return;
        }
        workflow.archive(currentEntry);
        entries.removeIf(entry -> entry.id().equals(currentEntry.id()));
        startNewContract();
    }

    @FXML
    private void showArchive() {
        new ArchiveDialog(workflow, (Stage) contractList.getScene().getWindow(), entries::add).showAndWait();
    }

    @FXML
    private void createPdf() {
        runOnCurrent(() -> show(workflow.createPdf(workflow.save(currentEntry, contractFormController.read()))));
    }

    @FXML
    private void openPdf() {
        if (currentEntry == null) {
            return;
        }
        runOnCurrent(() -> {
            java.nio.file.Path pdf = workflow.originalPdf(currentEntry)
                    .orElseThrow(() -> new IllegalStateException(I18n.get("pdf.missing")));
            if (!java.awt.Desktop.isDesktopSupported()) {
                throw new IllegalStateException(I18n.get("pdf.open.unsupported") + pdf);
            }
            java.awt.Desktop.getDesktop().open(pdf.toFile());
        });
    }

    @FXML
    private void markAwaitingSignature() {
        if (currentEntry != null) {
            runOnCurrent(() -> show(workflow.markAwaitingSignature(currentEntry)));
        }
    }

    private interface Action {
        void run() throws IOException;
    }

    private void runOnCurrent(Action action) {
        try {
            action.run();
        } catch (IOException | RuntimeException exception) {
            new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
        }
    }

    private void show(ContractEntry saved) {
        replaceEntry(saved);
        contractList.getSelectionModel().select(saved);
        selectEntry(saved);
    }

    private void replaceEntry(ContractEntry saved) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(saved.id())) {
                entries.set(index, saved);
                return;
            }
        }
        entries.add(saved);
    }

    private void selectEntry(ContractEntry entry) {
        currentEntry = entry;
        showContract(entry.contract());
        updateEntryControls();
    }

    private void showContract(LeaseContract contract) {
        contractFormController.fill(contract);
        lblTitle.setText(contract.name());
        lblVerpaechterTxt.setText(I18n.get("lessor.prefix") + contract.lessor());
    }

    private void updateEntryControls() {
        ContractStatus status = currentEntry == null ? null : currentEntry.status();
        saveButton.setDisable(status != null && status != ContractStatus.DRAFT);
        createPdfButton.setDisable(status != null && status != ContractStatus.DRAFT && status != ContractStatus.PDF_CREATED);
        openPdfButton.setDisable(status == null || status == ContractStatus.DRAFT);
        awaitingButton.setDisable(status != ContractStatus.PDF_CREATED);
        deleteButton.setDisable(currentEntry == null);
        statusLabel.setText(currentEntry == null
                ? I18n.get("status.new") : ContractStatusText.describe(currentEntry));
    }










    @FXML
    private void switchToGerman() {
        switchLanguage(Locale.GERMAN);
    }

    @FXML
    private void switchToEnglish() {
        switchLanguage(Locale.ENGLISH);
    }

    private void switchLanguage(Locale locale) {
        if (locale.getLanguage().equals(I18n.locale().getLanguage())) {
            return;
        }
        I18n.setLocale(locale);
        try {
            I18n.saveToConfig(I18n.defaultConfigPath());
        } catch (IOException ignored) {
            // The language still changes for this session if the config cannot be written.
        }
        Scene scene = contractList.getScene();
        int selectedIndex = contractList.getSelectionModel().getSelectedIndex();
        try {
            ViewLoader.Loaded loaded = ViewLoader.load(context);
            loaded.controller().contractList.getSelectionModel().select(Math.max(selectedIndex, 0));
            Parent root = loaded.root();
            scene.setRoot(root);
            if (scene.getWindow() instanceof Stage stage) {
                stage.setTitle(I18n.get("app.title"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FXML
    private void showPreview() {
        try {
            Map<String, String> values = ContractPdfValues.from(contractFormController.read());
            VBox pages = new VBox(18.0);
            pages.setPadding(new Insets(20.0));
            pages.setStyle("-fx-background-color: #d8d8d8;");
            for (javafx.scene.image.Image page : new LeaseContractPdfPreview().render(values)) {
                ImageView pageView = new ImageView(page);
                pageView.setPreserveRatio(true);
                pageView.setFitWidth(790.0);
                pages.getChildren().add(pageView);
            }

            ScrollPane scrollPane = new ScrollPane(pages);
            scrollPane.setFitToWidth(true);
            Stage previewStage = new Stage();
            previewStage.setTitle(I18n.get("preview.title") + contractFormController.contractName());
            previewStage.initModality(Modality.WINDOW_MODAL);
            previewStage.initOwner(contractList.getScene().getWindow());
            previewStage.setScene(new Scene(scrollPane, 900.0, 850.0));
            previewStage.show();
        } catch (IOException | IllegalArgumentException exception) {
            new Alert(Alert.AlertType.ERROR,
                    I18n.get("preview.error") + exception.getMessage()).showAndWait();
        }
    }


}
