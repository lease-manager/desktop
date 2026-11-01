package kienzle.desktop;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Editable list of the parcels of a contract: one row per parcel, always at least one row. */
public final class ParcelEditor extends VBox {
    /** Raw text of one row, so partially typed or invalid input survives a view reload. */
    public record Draft(String number, String district, String size, String type, String designation, String rent) {
        static Draft empty() {
            return new Draft("", "", "", "", "", "");
        }

        boolean isBlank() {
            return number.isBlank() && district.isBlank() && size.isBlank() && type.isBlank()
                    && designation.isBlank() && rent.isBlank();
        }
    }

    private static final double[] COLUMN_PERCENT = {14, 16, 11, 16, 23, 14};
    private static final String[] COLUMN_KEYS = {
            "parcel", "district", "detail.size", "detail.land.use", "parcel.designation", "parcel.rent"};

    private final GridPane grid = new GridPane();
    private final List<Row> rows = new ArrayList<>();
    private final Label errorLabel = new Label(I18n.get("parcel.error"));
    private Runnable onChange = () -> {
    };

    public ParcelEditor() {
        setSpacing(8);
        grid.setHgap(6);
        grid.setVgap(6);
        for (double percent : COLUMN_PERCENT) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percent);
            grid.getColumnConstraints().add(column);
        }
        ColumnConstraints removeColumn = new ColumnConstraints();
        removeColumn.setMinWidth(34);
        grid.getColumnConstraints().add(removeColumn);

        Button addButton = new Button(I18n.get("parcel.add"));
        addButton.setId("addParcelButton");
        addButton.setOnAction(event -> {
            addRow(Draft.empty());
            rebuild();
            changed();
        });
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        getChildren().addAll(grid, errorLabel, addButton);
        addRow(Draft.empty());
        rebuild();
    }

    private void changed() {
        markErrors();
        onChange.run();
    }

    public void setOnChange(Runnable callback) {
        this.onChange = callback;
        changed();
    }

    public List<Draft> drafts() {
        return rows.stream().map(Row::draft).toList();
    }

    public void setDrafts(List<Draft> drafts) {
        rows.clear();
        drafts.forEach(this::addRow);
        if (rows.isEmpty()) {
            addRow(Draft.empty());
        }
        rebuild();
        changed();
    }

    public void setParcels(List<Parcel> parcels) {
        setDrafts(parcels.stream().map(ParcelEditor::toDraft).toList());
    }

    /** Parcels as entered; completely empty rows are ignored. */
    public List<Parcel> read() {
        List<Parcel> parcels = new ArrayList<>();
        for (Draft draft : drafts()) {
            if (!draft.isBlank()) {
                parcels.add(new Parcel(draft.number().trim(), draft.district().trim(), decimal(draft.size()),
                        draft.type().trim(), draft.designation().trim(), decimal(draft.rent())));
            }
        }
        return parcels;
    }

    /** Sum of all rents that are valid numbers; invalid entries are reported by {@link #read()}. */
    public BigDecimal totalRent() {
        BigDecimal total = null;
        for (Draft draft : drafts()) {
            try {
                BigDecimal rent = ContractFormats.parseDecimal(draft.rent());
                if (rent != null) {
                    total = total == null ? rent : total.add(rent);
                }
            } catch (NumberFormatException ignored) {
                // Shown as an error when the contract is read.
            }
        }
        return total;
    }

    /** True if a filled row lacks a parcel number or a valid rent, or has an invalid size. */
    public boolean hasErrors() {
        return rows.stream().anyMatch(row -> rowInvalid(row.draft()));
    }

    private static boolean rowInvalid(Draft draft) {
        if (draft.isBlank()) {
            return false;
        }
        return !validParcelNumber(draft.number()) || !validNumber(draft.rent(), true) || !validNumber(draft.size(), false);
    }

    /** Exactly one parcel number such as "1080" or "1080/1". */
    static boolean validParcelNumber(String text) {
        return text.trim().matches("\\d+(/\\d+)?");
    }

    private static boolean validNumber(String text, boolean required) {
        try {
            return ContractFormats.parseDecimal(text) != null || !required;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void markErrors() {
        for (Row row : rows) {
            Draft draft = row.draft();
            boolean blank = draft.isBlank();
            row.mark(0, !blank && !validParcelNumber(draft.number()));
            row.mark(2, !blank && !validNumber(draft.size(), false));
            row.mark(5, !blank && !validNumber(draft.rent(), true));
        }
        errorLabel.setVisible(hasErrors());
        errorLabel.setManaged(hasErrors());
    }

    int rowCount() {
        return rows.size();
    }

    private static Draft toDraft(Parcel parcel) {
        return new Draft(parcel.number(), parcel.district(), ContractFormats.number(parcel.size()),
                parcel.type(), parcel.designation(), ContractFormats.money(parcel.rent()));
    }

    private static BigDecimal decimal(String text) {
        try {
            return ContractFormats.parseDecimal(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(I18n.get("error.number") + text, exception);
        }
    }

    private void addRow(Draft draft) {
        Row row = new Row(draft);
        row.remove.setOnAction(event -> {
            if (rows.size() > 1) {
                rows.remove(row);
                rebuild();
                changed();
            }
        });
        row.fields.forEach(field -> field.textProperty().addListener((observable, oldValue, newValue) ->
                onChange.run()));
        rows.add(row);
    }

    private void rebuild() {
        grid.getChildren().clear();
        for (int column = 0; column < COLUMN_KEYS.length; column++) {
            Label header = new Label(I18n.get(COLUMN_KEYS[column]));
            header.setStyle("-fx-font-weight: bold;");
            grid.add(header, column, 0);
        }
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            for (int column = 0; column < row.fields.size(); column++) {
                grid.add(row.fields.get(column), column, index + 1);
            }
            row.remove.setDisable(rows.size() == 1);
            grid.add(row.remove, COLUMN_KEYS.length, index + 1);
        }
        setPadding(Insets.EMPTY);
    }

    private static final class Row {
        private final List<TextField> fields = new ArrayList<>();
        private final Button remove = new Button("✕");

        Row(Draft draft) {
            for (String value : List.of(draft.number(), draft.district(), draft.size(), draft.type(),
                    draft.designation(), draft.rent())) {
                TextField field = new TextField(value);
                field.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(field, Priority.ALWAYS);
                fields.add(field);
            }
            remove.setTooltip(new javafx.scene.control.Tooltip(I18n.get("parcel.remove")));
        }

        void mark(int column, boolean error) {
            fields.get(column).setStyle(error ? "-fx-border-color: #c0392b;" : "");
        }

        Draft draft() {
            return new Draft(fields.get(0).getText(), fields.get(1).getText(), fields.get(2).getText(),
                    fields.get(3).getText(), fields.get(4).getText(), fields.get(5).getText());
        }
    }
}
