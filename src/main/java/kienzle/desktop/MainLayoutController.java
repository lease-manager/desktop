package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import kienzle.desktop.app.AppContext;
import kienzle.desktop.app.ContractWorkflow;
import kienzle.desktop.contract.ContractEntry;
import kienzle.desktop.contract.ContractStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

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

    private final ContractListState listState = new ContractListState();
    @FXML
    private ContractFormController contractFormController;

    private final AppContext context;
    private final ContractWorkflow workflow;
    private final UiFeedback feedback = new UiFeedback();
    private final PackageFlowFacade packageFlow;
    private final ContractUiActions uiActions;

    public MainLayoutController(AppContext context) {
        this.context = context;
        this.workflow = context.workflow();
        this.packageFlow = new PackageFlowFacade(workflow, feedback);
        this.uiActions = new ContractUiActions(workflow);
    }

    @FXML
    public void initialize() {
        contractFormController.setOnValidationChanged(this::updateEntryControls);
        configureLanguageButtons();
        listState.setAll(workflow.list());
        configureStatusFilter();
        configureContractList();
        selectInitialContract();
    }

    private void configureLanguageButtons() {
        germanButton.setGraphic(FlagIcons.germany());
        germanButton.setTooltip(new Tooltip(I18n.get("language.de")));
        englishButton.setGraphic(FlagIcons.unitedKingdom());
        englishButton.setTooltip(new Tooltip(I18n.get("language.en")));
    }

    private void configureStatusFilter() {
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
    }

    private void configureContractList() {
        contractList.setItems(listState.visibleEntries());
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
    }

    private void selectInitialContract() {
        if (listState.isEmpty()) {
            startNewContract();
            return;
        }
        contractList.getSelectionModel().selectFirst();
    }

    private void applyFilter(StatusFilter filter) {
        listState.applyFilter(filter);
    }

    @FXML
    private void startNewContract() {
        contractList.getSelectionModel().clearSelection();
        listState.clearCurrentEntry();
        showContract(ContractDefaults.newContract());
        updateEntryControls();
    }

    @FXML
    private void saveContract() {
        if (contractFormController.hasValidationErrors()) {
            updateEntryControls();
            return;
        }
        try {
            LeaseContract contract = contractFormController.read();
            show(uiActions.save(listState.currentEntry(), contract));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            feedback.showError(exception.getMessage());
        }
    }

    @FXML
    private void archiveContract() {
        if (!listState.hasCurrentEntry()) {
            return;
        }
        if (!feedback.confirm(I18n.get("contract.archive.confirm"))) {
            return;
        }
        uiActions.archive(listState.currentEntry());
        listState.removeCurrentEntry();
        startNewContract();
    }

    @FXML
    private void showArchive() {
        new ArchiveDialog(workflow, (Stage) contractList.getScene().getWindow(), this::handleRestoredArchivedEntry)
                .showAndWait();
    }

    private void handleRestoredArchivedEntry(ContractEntry restored) {
        replaceEntry(restored);
        if (listState.isFilteredOut(restored)) {
            statusFilter.setValue(StatusFilter.ALL);
        }
        listState.ensureVisible(contractList, restored);
        show(restored);
    }

    @FXML
    private void exportContractPackage() {
        if (!listState.hasCurrentEntry()) {
            return;
        }
        FileChooser chooser = PackageDialogSupport.buildChooser(I18n.get("package.export.title"), exportFileName());
        java.io.File file = chooser.showSaveDialog(contractList.getScene().getWindow());
        if (file == null) {
            return;
        }
        runOnCurrent(() -> packageFlow.exportPackage(listState.currentEntry(), file.toPath()));
    }

    private String exportFileName() {
        String name = listState.currentEntry().contract().name();
        return name.isBlank() ? "contract.leasepack" : name + ".leasepack";
    }

    @FXML
    private void importContractPackage() {
        java.io.File file = PackageDialogSupport.buildChooser(I18n.get("package.import.title"), null)
                .showOpenDialog(contractList.getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!PackageDialogSupport.confirmImport(workflow, (Stage) contractList.getScene().getWindow(), file.toPath())) {
            return;
        }
        runOnCurrent(() -> importSelectedPackage(file.toPath()));
    }

    private void importSelectedPackage(Path path) throws IOException {
        ContractEntry imported = packageFlow.importPackage(path);
        listState.addEntry(imported);
        show(imported);
    }

    @FXML
    private void createPdf() {
        runOnCurrent(() -> show(uiActions.createPdf(listState.currentEntry(), contractFormController.read())));
    }

    @FXML
    private void openPdf() {
        if (!listState.hasCurrentEntry()) {
            return;
        }
        runOnCurrent(() -> openSelectedPdf());
    }

    private void openSelectedPdf() {
        Path pdf = uiActions.originalPdf(listState.currentEntry())
                .orElseThrow(() -> new IllegalStateException(I18n.get("pdf.missing")));
        Path allowedPdf = validateAllowedDocumentPath(pdf);
        openDocument(allowedPdf);
    }

    private void openDocument(Path documentPath) {
        if (!java.awt.Desktop.isDesktopSupported()) {
            throw new IllegalStateException(I18n.get("pdf.open.unsupported") + documentPath);
        }
        try {
            java.awt.Desktop.getDesktop().open(documentPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException(I18n.get("pdf.missing"), e);
        }
    }

    private Path validateAllowedDocumentPath(Path documentPath) {
        Path root = context.dataDir().toAbsolutePath().normalize();
        Path resolved = documentPath.toAbsolutePath().normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalStateException(I18n.get("pdf.open.unsupported") + documentPath);
        }
        try {
            Path realPath = resolved.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
            if (!Files.isRegularFile(realPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException(I18n.get("pdf.missing"));
            }
            if (!realPath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                throw new IllegalStateException(I18n.get("pdf.open.unsupported") + documentPath);
            }
            return realPath;
        } catch (IOException e) {
            throw new IllegalStateException(I18n.get("pdf.missing"), e);
        }
    }

    @FXML
    private void markAwaitingSignature() {
        if (listState.hasCurrentEntry()) {
            runOnCurrent(() -> show(uiActions.markAwaitingSignature(listState.currentEntry())));
        }
    }

    private interface Action {
        void run() throws IOException;
    }

    private void runOnCurrent(Action action) {
        try {
            action.run();
        } catch (IOException | RuntimeException exception) {
            feedback.showError(exception.getMessage());
        }
    }

    private void show(ContractEntry saved) {
        replaceEntry(saved);
        contractList.getSelectionModel().select(saved);
        selectEntry(saved);
    }

    private void replaceEntry(ContractEntry saved) {
        listState.replaceEntry(saved);
    }

    private void selectEntry(ContractEntry entry) {
        listState.setCurrentEntry(entry);
        if (entry == null) {
            return;
        }
        showContract(entry.contract());
        updateEntryControls();
    }

    private void showContract(LeaseContract contract) {
        contractFormController.fill(contract);
        lblTitle.setText(contract.name());
        lblVerpaechterTxt.setText(I18n.get("lessor.prefix") + contract.lessor());
    }

    private void updateEntryControls() {
        ContractEntry currentEntry = listState.currentEntry();
        ContractStatus status = currentEntry == null ? null : currentEntry.status();

        saveButton.setDisable(isLockedForEditing(status) || contractFormController.hasValidationErrors());
        createPdfButton.setDisable(!canCreatePdf(status));
        openPdfButton.setDisable(!canOpenPdf(status));
        awaitingButton.setDisable(!isAwaitingSignatureEligible(status));
        deleteButton.setDisable(currentEntry == null);
        statusLabel.setText(currentEntry == null
                ? I18n.get("status.new") : ContractStatusText.describe(currentEntry));
    }

    private boolean isLockedForEditing(ContractStatus status) {
        return status != null && status != ContractStatus.DRAFT;
    }

    private boolean canCreatePdf(ContractStatus status) {
        return status == null || status == ContractStatus.DRAFT || status == ContractStatus.PDF_CREATED;
    }

    private boolean canOpenPdf(ContractStatus status) {
        return status != null && status != ContractStatus.DRAFT;
    }

    private boolean isAwaitingSignatureEligible(ContractStatus status) {
        return status == ContractStatus.PDF_CREATED;
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
        ContractViewUiSupport.switchLanguage(context, contractList.getScene(),
                locale, contractList.getSelectionModel().getSelectedIndex());
    }

    public void selectListIndex(int index) {
        contractList.getSelectionModel().select(Math.max(index, 0));
    }

    @FXML
    private void showPreview() {
        try {
            Map<String, String> values = ContractPdfValues.from(contractFormController.read());
            VBox pages = new VBox(18.0);
            pages.setPadding(new Insets(20.0));
            pages.setStyle("-fx-background-color: #d8d8d8;");
            pages.getChildren().addAll(renderPreviewPages(values));

            ScrollPane scrollPane = new ScrollPane(pages);
            scrollPane.setFitToWidth(true);
            Stage previewStage = createPreviewStage(scrollPane);
            previewStage.show();
        } catch (IOException | IllegalArgumentException exception) {
            feedback.showError(I18n.get("preview.error") + exception.getMessage());
        }
    }

    private java.util.List<ImageView> renderPreviewPages(Map<String, String> values) throws IOException {
        return new LeaseContractPdfPreview().render(values).stream()
                .map(page -> {
                    ImageView pageView = new ImageView(page);
                    pageView.setPreserveRatio(true);
                    pageView.setFitWidth(790.0);
                    return pageView;
                })
                .toList();
    }

    private Stage createPreviewStage(ScrollPane scrollPane) {
        Stage previewStage = new Stage();
        previewStage.setTitle(I18n.get("preview.title") + contractFormController.contractName());
        previewStage.initModality(Modality.WINDOW_MODAL);
        previewStage.initOwner(contractList.getScene().getWindow());
        previewStage.setScene(new Scene(scrollPane, 900.0, 850.0));
        return previewStage;
    }


}
