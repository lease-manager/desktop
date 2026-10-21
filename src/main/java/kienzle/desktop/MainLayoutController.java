package kienzle.desktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import javafx.util.StringConverter;

public class MainLayoutController {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final DateTimeFormatter DISPLAY_ANNUAL_DATE = DateTimeFormatter.ofPattern("dd.MM.");

    @FXML
    private ListView<LeaseContract> contractList;

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblVerpaechterTxt;

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
    private TextField parcelField;

    @FXML
    private TextField districtField;

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
    private TextField sizeField;

    @FXML
    private TextField typeField;

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

        private static final String DEFAULT_PARAGRAPH_FOUR = "Sollte der/die Verpächter(in) die verpachteten Gründe ganz oder teilweise selbst benötigen oder an einen Dritten veräußern, der über die Pachtgründe anderweitig verfügen will, so ist der/die Verpächter(in) berechtigt, das Pachtverhältnis sofort zu lösen.\n"
            + "Eine sofortige Kündigung ist außerdem bei einem Verstoß gegen diesen Pachtvertrag möglich.";

    private final ObservableList<LeaseContract> contracts = FXCollections.observableArrayList(
            new LeaseContract(
                    "PV-001",
                    "Pachtvertrag Nordfeld",
                    "Müller GmbH",
                    "Berlin",
                    "01.01.2025",
                    "Hans Weber",
                    "Berlin",
                    "01.01.2025",
                    "123/4",
                    "Gemarkung Ost",
                    "48000",
                    "Ackerland",
                    "Erbpachtfläche für landwirtschaftliche Nutzung",
                    "2.350,00 €",
                    "Unbestimmte Dauer",
                    "01.01.2025",
                    "1.900,00",
                    true,
                    "01.01.2027",
                    "01.01.2025",
                    "Müller GmbH",
                    "Raiffeisenbank Ost",
                    "DE12 3456 7890 1234 5678 90",
                    "GENODEF1RFS",
                    "Amtsgericht Berlin",
                    "6 Monate",
                    "31. Oktober",
                    DEFAULT_PARAGRAPH_FOUR,
                    defaultParagraphFive("6 Monate", "31. Oktober"),
                    "Musterstraße 1, 10115 Berlin",
                    "Beispielweg 2, 10117 Berlin",
                    "",
                    "01.01.2025",
                    "31.12.2026",
                    "jährlich",
                    "3. Werktag",
                    "eintausendneunhundert Euro",
                    "zweitausenddreihundertfünfzig Euro"
            ),
            new LeaseContract(
                    "PV-002",
                    "Pachtvertrag Westacker",
                    "Schmidt Agrar",
                    "Hamburg",
                    "15.04.2024",
                    "Erik Schulte",
                    "Hamburg",
                    "15.04.2024",
                    "87/12",
                    "Gemarkung West",
                    "32000",
                    "Grünland",
                    "Weidefläche mit extensiver Nutzung",
                    "1.680,00 €",
                    "Bestimmte Dauer",
                    "15.04.2024",
                    "1.500,00",
                    true,
                    "15.04.2029",
                    "15.04.2024",
                    "Schmidt Agrar",
                    "DE98 7654 3210 9876 5432 10",
                    "VOBADEDS",
                    "Amtsgericht Hamburg",
                    "3 Monate",
                    "30. September",
                    DEFAULT_PARAGRAPH_FOUR,
                    defaultParagraphFive("3 Monate", "30. September"),
                    "Beispielstraße 8, 20095 Hamburg",
                    "Feldweg 5, 20097 Hamburg",
                    "15.04.2029",
                    "15.04.2024",
                    "14.04.2025",
                    "monatlich",
                    "5.",
                    "31. März",
                    "eintausendfünfhundert Euro",
                    "eintausendsechshundertachtzig Euro"
            ),
            new LeaseContract(
                    "PV-003",
                    "Pachtvertrag Hofstelle",
                    "Bauer KG",
                    "Leipzig",
                    "01.07.2023",
                    "Sophie Klein",
                    "Leipzig",
                    "01.07.2023",
                    "58/9",
                    "Gemarkung Mitte",
                    "16000",
                    "Gartenland",
                    "Pachtobjekt für Gemüseproduktion",
                    "980,00 €",
                    "Unbestimmte Dauer",
                    "01.07.2023",
                    "900,00",
                    false,
                    "01.07.2025",
                    "01.07.2023",
                    "Bauer KG",
                    "Sparkasse Nord",
                    "DE11 2222 3333 4444 5555 66",
                    "SPOHDEFF",
                    "Amtsgericht Leipzig",
                    "4 Monate",
                    "31. Oktober",
                    DEFAULT_PARAGRAPH_FOUR,
                    defaultParagraphFive("4 Monate", "31. Oktober"),
                    "Musterplatz 3, 04109 Leipzig",
                    "Gartenstraße 12, 04103 Leipzig",
                    "",
                    "01.07.2023",
                    "30.06.2024",
                    "vierteljährlich",
                    "1.",
                    "neunhundert Euro",
                    "neunhundertachtzig Euro"
            )
    );

    @FXML
    public void initialize() {
        durationField.setItems(FXCollections.observableArrayList("Unbestimmte Dauer", "Bestimmte Dauer"));
        configureDatePickers();
        configureIbanField();
        loadAmtsgerichte();
        courtComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldCourt, court) ->
            courtAddressLabel.setText(court == null ? "" : court.anschrift()));
        hasIntroductoryRentCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
            setIntroductoryRentFieldsVisible(selected));
        durationField.valueProperty().addListener((observable, oldValue, duration) ->
            updateLeaseEndDateVisibility(duration));

        contractList.setItems(contracts);
        contractList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(LeaseContract contract, boolean empty) {
                super.updateItem(contract, empty);
                setText(empty || contract == null ? null : contract.name());
            }
        });

        contractList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectContract(newValue);
            }
        });

        if (!contracts.isEmpty()) {
            contractList.getSelectionModel().selectFirst();
        }
    }

    public List<ContractDetailRow> buildDetailRows(LeaseContract contract) {
        return List.of(
                new ContractDetailRow("Vertragsname", contract.name()),
                new ContractDetailRow("Verpächter", contract.lessor()),
                new ContractDetailRow("Anschrift Verpächter", contract.lessorAddress()),
                new ContractDetailRow("Ort / Datum Verpächter", contract.lessorLocation() + " / " + contract.lessorDate()),
                new ContractDetailRow("Pächter", contract.tenant()),
                new ContractDetailRow("Anschrift Pächter", contract.tenantAddress()),
                new ContractDetailRow("Ort / Datum Pächter", contract.tenantLocation() + " / " + contract.tenantDate()),
                new ContractDetailRow("Flurstück", contract.parcelNumber()),
                new ContractDetailRow("Gemarkung", contract.district()),
                new ContractDetailRow("Größe m²", contract.size()),
                new ContractDetailRow("Wirtschaftsart", contract.type()),
                new ContractDetailRow("Regulärer Pachtzins in Euro", contract.rent()),
                new ContractDetailRow("Pachtverhältnis", contract.leaseDuration()),
                new ContractDetailRow("Beginn Pachtverhältnis", contract.startDate()),
                new ContractDetailRow("Ende Pachtverhältnis", contract.leaseEndDate()),
                new ContractDetailRow("Günstiger Pachtzins vorhanden", contract.hasIntroductoryRent() ? "Ja" : "Nein"),
                new ContractDetailRow("Einführungspachtzins in Euro", contract.introductoryRent()),
                new ContractDetailRow("Einführungspachtzins von", contract.introductoryRentFrom()),
                new ContractDetailRow("Einführungspachtzins bis", contract.introductoryRentUntil()),
                new ContractDetailRow("Einführungspachtzins in Worten", contract.introductoryRentWords()),
                new ContractDetailRow("Regulärer Pachtzins ab", contract.rentIncreaseDate()),
                new ContractDetailRow("Regulärer Pachtzins in Worten", contract.rentWords()),
                new ContractDetailRow("Pachtzins fällig alle", contract.rentDueInterval()),
                new ContractDetailRow("Fälligkeitstermin", contract.rentDueDay()),
                new ContractDetailRow("Erste Zahlung am", contract.firstRentDate()),
                new ContractDetailRow("Zahlungsempfänger", contract.rentRecipient()),
                new ContractDetailRow("Geldinstitut", contract.bankName()),
                new ContractDetailRow("IBAN", contract.iban()),
                new ContractDetailRow("BIC", contract.bic()),
                new ContractDetailRow("Amtsgericht", contract.court()),
                new ContractDetailRow("Kündigungsfrist", contract.terminationNotice()),
                new ContractDetailRow("Ende Gartenjahr", contract.gardeningYearEnd()),
                new ContractDetailRow("§ 4", contract.paragraphFour()),
                new ContractDetailRow("§ 5", contract.paragraphFive()),
                new ContractDetailRow("Beschreibung", contract.description())
        );
    }

    public void selectContract(LeaseContract contract) {
        if (contract == null) {
            return;
        }

        lblTitle.setText(contract.name());
        lblVerpaechterTxt.setText("Verpächter: " + contract.lessor());
        contractNameField.setText(contract.name());
        textField.setText(contract.lessor());
        lessorAddressField.setText(contract.lessorAddress());
        lessorLocationField.setText(contract.lessorLocation());
        setDate(lessorDateField, contract.lessorDate(), false);
        tenantField.setText(contract.tenant());
        tenantAddressField.setText(contract.tenantAddress());
        tenantLocationField.setText(contract.tenantLocation());
        setDate(tenantDateField, contract.tenantDate(), false);
        parcelField.setText(contract.parcelNumber());
        districtField.setText(contract.district());
        selectCourtForContract(contract.court());
        noticeField.setText(contract.terminationNotice());
        setDate(gardeningYearField, contract.gardeningYearEnd(), true);
        paragraphFourArea.setText(contract.paragraphFour());
        paragraphFiveArea.setText(contract.paragraphFive());
        descriptionArea.setText(contract.description());
        sizeField.setText(contract.size());
        typeField.setText(contract.type());
        rentField.setText(contract.rent());
        durationField.setValue(contract.leaseDuration());
        hasIntroductoryRentCheckBox.setSelected(contract.hasIntroductoryRent());
        setIntroductoryRentFieldsVisible(contract.hasIntroductoryRent());
        updateLeaseEndDateVisibility(contract.leaseDuration());
        setDate(startDateField, contract.startDate(), false);
        setDate(leaseEndDateField, contract.leaseEndDate(), false);
        introRentField.setText(contract.introductoryRent());
        setDate(introductoryRentFromField, contract.introductoryRentFrom(), false);
        setDate(introductoryRentUntilField, contract.introductoryRentUntil(), false);
        introductoryRentWordsField.setText(contract.introductoryRentWords());
        rentWordsField.setText(contract.rentWords());
        setDate(rentIncreaseDateField, contract.rentIncreaseDate(), false);
        rentDueIntervalField.setText(contract.rentDueInterval());
        rentDueDayField.setText(contract.rentDueDay());
        setDate(firstRentDateField, contract.firstRentDate(), false);
        recipientField.setText(contract.rentRecipient());
        bankField.setText(contract.bankName());
        ibanField.setText(contract.iban());
        bicField.setText(contract.bic());
    }

    private void loadAmtsgerichte() {
        try {
            courtComboBox.setItems(FXCollections.observableArrayList(new AmtsgerichtRepository().load()));
        } catch (IOException exception) {
            throw new IllegalStateException("Die Amtsgericht-Liste konnte nicht geladen werden.", exception);
        }
    }

    private void selectCourtForContract(String courtValue) {
        String location = courtValue.replaceFirst("(?i)^Amtsgericht\\s+", "").trim();
        Amtsgericht match = courtComboBox.getItems().stream()
                .filter(court -> court.ort().equalsIgnoreCase(location)
                        || court.name().equalsIgnoreCase(courtValue))
                .findFirst()
            .orElse(null);
        courtComboBox.getSelectionModel().select(match);
    }

    private void configureIbanField() {
        ibanField.setPromptText("DE00 0000 0000 0000 0000 00");
        ibanField.setTextFormatter(new TextFormatter<String>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            String original = change.getControlText();
            int caretPosition = Math.min(change.getRangeStart(), original.length());
            String rawBeforeCaret = original.substring(0, caretPosition) + change.getText();
            int rawCaretPosition = rawBeforeCaret.replaceAll("[^A-Za-z0-9]", "").length();
            String rawValue = change.getControlNewText().replaceAll("[^A-Za-z0-9]", "")
                    .toUpperCase(java.util.Locale.ROOT);
            if (rawValue.length() > 34) {
                rawValue = rawValue.substring(0, 34);
            }
            String formattedValue = formatIban(rawValue);
            int spacesBeforeCaret = rawCaretPosition == 0 ? 0 : (rawCaretPosition - 1) / 4;
            int formattedCaret = Math.min(formattedValue.length(), rawCaretPosition + spacesBeforeCaret);
            change.setRange(0, original.length());
            change.setText(formattedValue);
            change.setCaretPosition(formattedCaret);
            change.setAnchor(formattedCaret);
            return change;
        }));
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
        picker.setPromptText(annualDate ? "TT.MM." : "TT.MM.JJJJ");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                if (date == null) {
                    return "";
                }
                return date.format(annualDate ? DISPLAY_ANNUAL_DATE : DISPLAY_DATE);
            }

            @Override
            public LocalDate fromString(String text) {
                return parseDate(text, annualDate);
            }
        });
    }

    private void setDate(DatePicker picker, String value, boolean annualDate) {
        picker.setValue(parseDate(value, annualDate));
    }

    private String getDate(DatePicker picker, boolean annualDate) {
        LocalDate date = picker.getValue();
        if (date == null) {
            return "";
        }
        return date.format(annualDate ? DISPLAY_ANNUAL_DATE : DISPLAY_DATE);
    }

    private LocalDate parseDate(String value, boolean annualDate) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (annualDate) {
            DateTimeFormatter[] annualFormats = {
                    DateTimeFormatter.ofPattern("dd.MM.uuuu"),
                    DateTimeFormatter.ofPattern("d.M.uuuu"),
                    DateTimeFormatter.ofPattern("d. MMMM yyyy", java.util.Locale.GERMAN)
            };
            String withReferenceYear = trimmed.matches(".*\\d{4}$") ? trimmed : trimmed + " 2000";
            for (DateTimeFormatter formatter : annualFormats) {
                try {
                    return LocalDate.parse(withReferenceYear, formatter).withYear(2000);
                } catch (DateTimeParseException ignored) {
                    // Try the next supported input format.
                }
            }
            return null;
        }
        DateTimeFormatter[] formats = {
                DISPLAY_DATE,
                DateTimeFormatter.ofPattern("d.M.uuuu"),
                DateTimeFormatter.ISO_LOCAL_DATE
        };
        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported input format.
            }
        }
        return null;
    }

    private static String defaultParagraphFive(String terminationNotice, String gardeningYearEnd) {
        return "1. Für den Flächeninhalt und den Ertrag des Grundstücks übernimmt der/die Verpächter(in) keine Gewähr.\n"
                + "2. Für die Erhaltung von Grenzanlagen und -zeichen hat der/die Pächter(in) zu sorgen. "
                + "Grunddienstbarkeiten und beschränkt persönliche Dienstbarkeiten, mit denen das verpachtete "
                + "Grundstück belastet ist, muss der/die Pächter(in) dulden.\n"
                + "3. Während der Pachtdauer ist das Grundstück ordentlich zu bewirtschaften. Schäden durch "
                + "Witterung, Naturereignisse oder Wild hat der/die Pächter(in) zu tragen und zu bezahlen. "
                + "Laufender Unterhalt und gewöhnliche Ausbesserungen, insbesondere an Wegen und Gräben, obliegen "
                + "dem/der Pächter(in).\n"
                + "4. Auf dem Grundstück vorhandene Bäume und Hecken müssen unbeschädigt erhalten bleiben.\n"
                + "5. Eine Änderung der Kulturart des Grundstücks ist ohne Genehmigung des/der Verpächter(s/in) "
                + "nicht gestattet.\n"
                + "6. Der/Die Pächter(in) darf die Nutzung des Grundstücks nur mit schriftlicher Erlaubnis des/der "
                + "Verpächter(s/in) einem Dritten überlassen oder unterverpachten.\n"
                + "7. Die auf dem Grundstück ruhenden Lasten trägt der/die Verpächter(in).\n"
                + "8. Verändern sich während der Pachtzeit die Lebenshaltungskosten, können die Parteien eine "
                + "Anpassung des Pachtzinses nach billigem Ermessen vereinbaren.\n"
                + "9. Eine Kündigung des Pachtvertrags muss " + terminationNotice
                + " vor Ende des Gartenjahres (" + gardeningYearEnd + ") erfolgen.";
    }

    private String formatIban(String rawValue) {
        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < rawValue.length(); index++) {
            if (index > 0 && index % 4 == 0) {
                formatted.append(' ');
            }
            formatted.append(rawValue.charAt(index));
        }
        return formatted.toString();
    }

    private void setIntroductoryRentFieldsVisible(boolean visible) {
        introductoryRentFields.setVisible(visible);
        introductoryRentFields.setManaged(visible);
    }

    private void updateLeaseEndDateVisibility(String duration) {
        boolean fixedTerm = "Bestimmte Dauer".equals(duration);
        leaseEndDateRow.setVisible(fixedTerm);
        leaseEndDateRow.setManaged(fixedTerm);
    }

    @FXML
    private void showPreview() {
        try {
            Map<String, String> values = Map.ofEntries(
                    Map.entry("lessor", get(textField)),
                    Map.entry("lessorAddress", get(lessorAddressField)),
                    Map.entry("tenant", get(tenantField)),
                    Map.entry("tenantAddress", get(tenantAddressField)),
                    Map.entry("parcelNumber", get(parcelField)),
                    Map.entry("district", get(districtField)),
                    Map.entry("size", get(sizeField)),
                    Map.entry("type", get(typeField)),
                    Map.entry("description", get(descriptionArea)),
                    Map.entry("rent", get(rentField)),
                    Map.entry("totalRent", get(rentField)),
                    Map.entry("leaseDuration", durationField.getValue() == null ? "" : durationField.getValue()),
                    Map.entry("startDate", getDate(startDateField, false)),
                    Map.entry("introductoryRentFrom", hasIntroductoryRentCheckBox.isSelected() ? getDate(introductoryRentFromField, false) : ""),
                    Map.entry("introductoryRentUntil", hasIntroductoryRentCheckBox.isSelected() ? getDate(introductoryRentUntilField, false) : ""),
                    Map.entry("hasIntroductoryRent", Boolean.toString(hasIntroductoryRentCheckBox.isSelected())),
                    Map.entry("introductoryRent", hasIntroductoryRentCheckBox.isSelected() ? get(introRentField) : ""),
                    Map.entry("introductoryRentWords", hasIntroductoryRentCheckBox.isSelected() ? get(introductoryRentWordsField) : ""),
                    Map.entry("rentWords", get(rentWordsField)),
                    Map.entry("rentIncreaseDate", hasIntroductoryRentCheckBox.isSelected() ? getDate(rentIncreaseDateField, false) : ""),
                    Map.entry("rentDueDay", get(rentDueDayField)),
                    Map.entry("rentDueInterval", get(rentDueIntervalField)),
                    Map.entry("firstRentDate", getDate(firstRentDateField, false)),
                    Map.entry("rentRecipient", get(recipientField)),
                    Map.entry("bankName", get(bankField)),
                    Map.entry("iban", get(ibanField)),
                    Map.entry("bic", get(bicField)),
                    Map.entry("court", selectedCourtAddress()),
                    Map.entry("terminationNotice", terminationMonths(get(noticeField))),
                    Map.entry("paragraphFour", get(paragraphFourArea)),
                    Map.entry("paragraphFive", get(paragraphFiveArea)),
                    Map.entry("gardeningYearEnd", getDate(gardeningYearField, true)),
                    Map.entry("leaseEndDate", getDate(leaseEndDateField, false)),
                    Map.entry("lessorSignatureDate", signatureDate(get(lessorLocationField), getDate(lessorDateField, false))),
                    Map.entry("tenantSignatureDate", signatureDate(get(tenantLocationField), getDate(tenantDateField, false)))
            );

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
            previewStage.setTitle("Vertragsvorschau - " + get(contractNameField));
            previewStage.initModality(Modality.WINDOW_MODAL);
            previewStage.initOwner(contractList.getScene().getWindow());
            previewStage.setScene(new Scene(scrollPane, 900.0, 850.0));
            previewStage.show();
        } catch (IOException exception) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Die Vertragsvorschau konnte nicht erstellt werden: " + exception.getMessage()).showAndWait();
        }
    }

    private String get(TextInputControl control) {
        return control.getText() == null ? "" : control.getText().trim();
    }

    private String selectedCourtAddress() {
        Amtsgericht court = courtComboBox.getValue();
        return court == null ? "" : court.name() + ", " + court.anschrift();
    }

    private String signatureDate(String location, String date) {
        if (location.isBlank()) {
            return date;
        }
        return date.isBlank() ? location : location + ", den " + date;
    }

    private String terminationMonths(String notice) {
        String months = notice.replaceAll("(?i)\\s*Monate?", "").trim();
        return switch (months) {
            case "1" -> "einen";
            case "2" -> "zwei";
            case "3" -> "drei";
            case "4" -> "vier";
            case "5" -> "fünf";
            case "6" -> "sechs";
            case "7" -> "sieben";
            case "8" -> "acht";
            case "9" -> "neun";
            case "10" -> "zehn";
            case "11" -> "elf";
            case "12" -> "zwölf";
            default -> notice;
        };
    }

}
