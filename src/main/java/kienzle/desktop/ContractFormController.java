package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Objects;

/** The contract input form: configures the controls, reads them into a {@link LeaseContract} and fills them from one. */
public class ContractFormController {

    @FXML
    private TextField contractNameField;

    @FXML
    private TextField textField;

    @FXML
    private TextField lessorAddressField;

    @FXML
    private TextField lessorLocationField;

    @FXML
    private DatePicker lessorDateField;

    @FXML
    private TextField tenantField;

    @FXML
    private TextField tenantAddressField;

    @FXML
    private TextField tenantLocationField;

    @FXML
    private DatePicker tenantDateField;

    @FXML
    private VBox parcelBox;

    @FXML
    private ComboBox<Amtsgericht> courtComboBox;

    @FXML
    private Label courtAddressLabel;

    @FXML
    private TextField noticeField;

    @FXML
    private DatePicker gardeningYearField;

    @FXML
    private TextArea paragraphFourArea;

    @FXML
    private TextArea paragraphFiveArea;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField rentField;

    @FXML
    private ComboBox<String> durationField;

    @FXML
    private CheckBox hasIntroductoryRentCheckBox;

    @FXML
    private VBox introductoryRentFields;

    @FXML
    private HBox leaseEndDateRow;

    @FXML
    private DatePicker startDateField;

    @FXML
    private TextField introRentField;

    @FXML
    private DatePicker introductoryRentFromField;

    @FXML
    private TextField introductoryRentWordsField;

    @FXML
    private TextField rentWordsField;

    @FXML
    private DatePicker firstRentDateField;

    @FXML
    private TextField recipientField;

    @FXML
    private TextField bankField;

    @FXML
    private TextField ibanField;

    @FXML
    private Label ibanErrorLabel;

    @FXML
    private Label introRentErrorLabel;

    private boolean introRentInvalid;

    @FXML
    private TextField bicField;

    @FXML
    private DatePicker leaseEndDateField;

    @FXML
    private DatePicker introductoryRentUntilField;

    @FXML
    private DatePicker rentIncreaseDateField;

    @FXML
    private TextField rentDueIntervalField;

    @FXML
    private TextField rentDueDayField;

    private final ParcelEditor parcelEditor = new ParcelEditor();
    private Runnable onValidationChanged = () -> {
    };
    private boolean ibanInvalid;

    @FXML
    public void initialize() {
        durationField.setItems(FXCollections.observableArrayList(ContractDefaults.DURATION_INDEFINITE, ContractDefaults.DURATION_FIXED));
        durationField.setConverter(new StringConverter<>() {
            @Override
            public String toString(String duration) {
                if (ContractDefaults.DURATION_INDEFINITE.equals(duration)) {
                    return I18n.get("duration.indefinite");
                }
                return ContractDefaults.DURATION_FIXED.equals(duration) ? I18n.get("duration.fixed") : duration;
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        configureDatePickers();
        parcelBox.getChildren().add(parcelEditor);
        parcelEditor.setOnChange(() -> {
            rentField.setText(ContractFormats.money(parcelEditor.totalRent()));
            onValidationChanged.run();
        });
        ibanField.setPromptText("DE00 0000 0000 0000 0000 00");
        ibanField.setTextFormatter(IbanFormatter.textFormatter());
        configureIbanValidation();
        InputFilters.decimal(introRentField);
        configureIntroRentValidation();
        loadAmtsgerichte();
        courtComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldCourt, court) ->
            courtAddressLabel.setText(court == null ? "" : court.anschrift()));
        hasIntroductoryRentCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
            setIntroductoryRentFieldsVisible(selected));
        durationField.valueProperty().addListener((observable, oldValue, duration) ->
            updateLeaseEndDateVisibility(duration));
    }

    ParcelEditor parcelEditor() {
        return parcelEditor;
    }

    public String contractName() {
        return get(contractNameField);
    }

    /** Calls the callback with the current contract name and lessor now and on every edit of either field. */
    public void setOnHeaderChanged(java.util.function.BiConsumer<String, String> callback) {
        Runnable notify = () -> callback.accept(contractNameField.getText(), textField.getText());
        contractNameField.textProperty().addListener((observable, oldValue, newValue) -> notify.run());
        textField.textProperty().addListener((observable, oldValue, newValue) -> notify.run());
        notify.run();
    }

    public void setOnValidationChanged(Runnable callback) {
        this.onValidationChanged = Objects.requireNonNull(callback);
    }

    void setContractName(String name) {
        contractNameField.setText(name);
    }

    public boolean hasValidationErrors() {
        return ibanInvalid || introRentInvalid || parcelEditor.hasErrors();
    }

    public FormState snapshot() {
        List<String> textValues = textControls().stream().map(TextInputControl::getText).toList();
        List<DatePickerState> dates = datePickers().stream()
                .map(picker -> new DatePickerState(picker.getValue(), picker.getEditor().getText()))
                .toList();
        return new FormState(textValues, dates,
                courtComboBox.getSelectionModel().getSelectedIndex(),
                durationField.getSelectionModel().getSelectedIndex(),
                hasIntroductoryRentCheckBox.isSelected(),
                parcelEditor.drafts());
    }

    public void restore(FormState state) {
        List<TextInputControl> textControls = textControls();
        if (state.textValues().size() != textControls.size()
                || state.dates().size() != datePickers().size()) {
            throw new IllegalArgumentException("Form state does not match the current contract form");
        }
        for (int index = 0; index < textControls.size(); index++) {
            textControls.get(index).setText(state.textValues().get(index));
        }
        List<DatePicker> datePickers = datePickers();
        for (int index = 0; index < datePickers.size(); index++) {
            DatePicker picker = datePickers.get(index);
            DatePickerState date = state.dates().get(index);
            picker.setValue(date.value());
            picker.getEditor().setText(date.editorText());
        }
        parcelEditor.setDrafts(state.parcels());
        courtComboBox.getSelectionModel().select(state.courtIndex());
        durationField.getSelectionModel().select(state.durationIndex());
        hasIntroductoryRentCheckBox.setSelected(state.hasIntroductoryRent());
        updateIbanValidation();
        setIntroductoryRentFieldsVisible(state.hasIntroductoryRent());
        updateLeaseEndDateVisibility(durationField.getValue());
    }

    private List<TextInputControl> textControls() {
        return List.of(contractNameField, textField, lessorAddressField, lessorLocationField, tenantField,
                tenantAddressField, tenantLocationField, noticeField,
                paragraphFourArea, paragraphFiveArea, descriptionArea,
                introRentField, introductoryRentWordsField, rentWordsField, rentDueIntervalField,
                rentDueDayField, recipientField, bankField, ibanField, bicField);
    }

    private List<DatePicker> datePickers() {
        return List.of(lessorDateField, tenantDateField, gardeningYearField, startDateField,
                introductoryRentFromField, firstRentDateField, leaseEndDateField,
                introductoryRentUntilField, rentIncreaseDateField);
    }

    public record FormState(List<String> textValues, List<DatePickerState> dates, int courtIndex,
                            int durationIndex, boolean hasIntroductoryRent,
                            List<ParcelEditor.Draft> parcels) {
        public FormState {
            textValues = List.copyOf(textValues);
            dates = List.copyOf(dates);
            parcels = List.copyOf(parcels);
        }
    }

    public record DatePickerState(LocalDate value, String editorText) {
    }

    public LeaseContract read() {
        boolean intro = hasIntroductoryRentCheckBox.isSelected();
        return LeaseContract.builder()
                .name(get(contractNameField))
                .lessor(get(textField))
                .lessorAddress(get(lessorAddressField))
                .lessorLocation(get(lessorLocationField))
                .lessorDate(lessorDateField.getValue())
                .tenant(get(tenantField))
                .tenantAddress(get(tenantAddressField))
                .tenantLocation(get(tenantLocationField))
                .tenantDate(tenantDateField.getValue())
                .parcels(parcelEditor.read())
                .description(get(descriptionArea))
                .leaseDuration(durationField.getValue() == null ? "" : durationField.getValue())
                .startDate(startDateField.getValue())
                .leaseEndDate(leaseEndDateField.getValue())
                .hasIntroductoryRent(intro)
                .introductoryRent(intro ? decimal(introRentField) : null)
                .introductoryRentFrom(intro ? introductoryRentFromField.getValue() : null)
                .introductoryRentUntil(intro ? introductoryRentUntilField.getValue() : null)
                .introductoryRentWords(get(introductoryRentWordsField))
                .rentIncreaseDate(intro ? rentIncreaseDateField.getValue() : null)
                .rentWords(get(rentWordsField))
                .rentDueInterval(get(rentDueIntervalField))
                .rentDueDay(get(rentDueDayField))
                .firstRentDate(firstRentDateField.getValue())
                .rentRecipient(get(recipientField))
                .bankName(get(bankField))
                .iban(validatedIban())
                .bic(get(bicField))
                .court(selectedCourtAddress())
                .terminationNotice(get(noticeField))
                .gardeningYearEnd(gardeningYearField.getValue() == null ? null
                        : MonthDay.from(gardeningYearField.getValue()))
                .paragraphFour(get(paragraphFourArea))
                .paragraphFive(get(paragraphFiveArea))
                .build();
    }

    private BigDecimal decimal(TextInputControl control) {
        try {
            return ContractFormats.parseDecimal(control.getText());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(I18n.get("error.number") + control.getText(), exception);
        }
    }

    private String validatedIban() {
        String iban = get(ibanField);
        updateIbanValidation();
        if (iban.isEmpty()) {
            return "";
        }
        if (!IbanFormatter.isValid(iban)) {
            throw new IllegalArgumentException(I18n.get("error.iban") + iban);
        }
        return IbanFormatter.format(IbanFormatter.normalize(iban));
    }

    public void fill(LeaseContract contract) {
        if (contract == null) {
            return;
        }

        contractNameField.setText(contract.name());
        textField.setText(contract.lessor());
        lessorAddressField.setText(contract.lessorAddress());
        lessorLocationField.setText(contract.lessorLocation());
        lessorDateField.setValue(contract.lessorDate());
        tenantField.setText(contract.tenant());
        tenantAddressField.setText(contract.tenantAddress());
        tenantLocationField.setText(contract.tenantLocation());
        tenantDateField.setValue(contract.tenantDate());
        parcelEditor.setParcels(contract.parcels());
        selectCourtForContract(contract.court());
        noticeField.setText(contract.terminationNotice());
        gardeningYearField.setValue(contract.gardeningYearEnd() == null ? null : contract.gardeningYearEnd().atYear(2000));
        paragraphFourArea.setText(contract.paragraphFour());
        paragraphFiveArea.setText(contract.paragraphFive());
        descriptionArea.setText(contract.description());
        durationField.setValue(contract.leaseDuration());
        hasIntroductoryRentCheckBox.setSelected(contract.hasIntroductoryRent());
        setIntroductoryRentFieldsVisible(contract.hasIntroductoryRent());
        updateLeaseEndDateVisibility(contract.leaseDuration());
        startDateField.setValue(contract.startDate());
        leaseEndDateField.setValue(contract.leaseEndDate());
        introRentField.setText(ContractFormats.money(contract.introductoryRent()));
        introductoryRentFromField.setValue(contract.introductoryRentFrom());
        introductoryRentUntilField.setValue(contract.introductoryRentUntil());
        introductoryRentWordsField.setText(contract.introductoryRentWords());
        rentWordsField.setText(contract.rentWords());
        rentIncreaseDateField.setValue(contract.rentIncreaseDate());
        rentDueIntervalField.setText(contract.rentDueInterval());
        rentDueDayField.setText(contract.rentDueDay());
        firstRentDateField.setValue(contract.firstRentDate());
        recipientField.setText(contract.rentRecipient());
        bankField.setText(contract.bankName());
        ibanField.setText(contract.iban());
        updateIbanValidation();
        bicField.setText(contract.bic());
    }

    private void loadAmtsgerichte() {
        try {
            courtComboBox.setItems(FXCollections.observableArrayList(new AmtsgerichtRepository().load()));
        } catch (IOException exception) {
            throw new IllegalStateException(I18n.get("court.load.error"), exception);
        }
    }

    private void selectCourtForContract(String courtValue) {
        String location = courtValue.replaceFirst("(?i)^Amtsgericht\\s+", "").trim();
        Amtsgericht match = courtComboBox.getItems().stream()
                .filter(court -> court.ort().equalsIgnoreCase(location)
                        || court.name().equalsIgnoreCase(courtValue)
                        || courtValue.startsWith(court.name() + ","))
                .findFirst()
            .orElse(null);
        courtComboBox.getSelectionModel().select(match);
    }

    private void configureDatePickers() {
        configureDatePicker(lessorDateField, false);
        configureDatePicker(tenantDateField, false);
        configureDatePicker(startDateField, false);
        configureDatePicker(leaseEndDateField, false);
        configureDatePicker(introductoryRentFromField, false);
        configureDatePicker(introductoryRentUntilField, false);
        configureDatePicker(rentIncreaseDateField, false);
        configureDatePicker(firstRentDateField, false);
        configureDatePicker(gardeningYearField, true);
    }

    private void configureDatePicker(DatePicker picker, boolean annualDate) {
        picker.setEditable(false);
        picker.setPromptText(I18n.get(annualDate ? "date.prompt.annual" : "date.prompt"));
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                if (date == null) {
                    return "";
                }
                return annualDate ? ContractFormats.monthDay(MonthDay.from(date)) : ContractFormats.date(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (annualDate) {
                    MonthDay monthDay = ContractFormats.parseMonthDay(text);
                    return monthDay == null ? null : monthDay.atYear(2000);
                }
                return ContractFormats.parseDate(text);
            }
        });
    }

    private void setIntroductoryRentFieldsVisible(boolean visible) {
        introductoryRentFields.setVisible(visible);
        introductoryRentFields.setManaged(visible);
    }

    private void updateLeaseEndDateVisibility(String duration) {
        boolean fixedTerm = ContractDefaults.DURATION_FIXED.equals(duration);
        leaseEndDateRow.setVisible(fixedTerm);
        leaseEndDateRow.setManaged(fixedTerm);
    }

    private void configureIbanValidation() {
        ibanField.textProperty().addListener((observable, oldValue, newValue) -> updateIbanValidation());
        updateIbanValidation();
    }

    private void configureIntroRentValidation() {
        introRentField.textProperty().addListener((observable, oldValue, newValue) -> updateIntroRentValidation());
        hasIntroductoryRentCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                updateIntroRentValidation());
        updateIntroRentValidation();
    }

    private void updateIntroRentValidation() {
        boolean invalid = false;
        if (hasIntroductoryRentCheckBox.isSelected()) {
            try {
                ContractFormats.parseDecimal(introRentField.getText());
            } catch (NumberFormatException exception) {
                invalid = true;
            }
        }
        introRentInvalid = invalid;
        introRentErrorLabel.setVisible(invalid);
        introRentErrorLabel.setManaged(invalid);
        onValidationChanged.run();
    }

    private void updateIbanValidation() {
        String iban = get(ibanField);
        boolean invalid = !iban.isEmpty() && !IbanFormatter.isValid(iban);
        ibanInvalid = invalid;
        ibanErrorLabel.setVisible(invalid);
        ibanErrorLabel.setManaged(invalid);
        onValidationChanged.run();
    }

    private String get(TextInputControl control) {
        return control.getText() == null ? "" : control.getText().trim();
    }

    private String selectedCourtAddress() {
        Amtsgericht court = courtComboBox.getValue();
        return court == null ? "" : court.name() + ", " + court.anschrift();
    }
}
