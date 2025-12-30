package kienzle.customcontrols;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import java.io.IOException;

public class LabeledTextField extends StackPane {

    @FXML private TextField textField;
    @FXML private Label hintLabel;

    // Die Property für den Text
    private final StringProperty hintTextProperty = new SimpleStringProperty("");

    public LabeledTextField() {
        loadFXML();
    }

    // Konstruktor für manuelle Erstellung im Code (optional)
    public LabeledTextField(String hint) {
        this();
        setHintText(hint);
    }

    private void loadFXML() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LabeledTextField.fxml"));

        // WICHTIG: Wir sagen dem Loader: "Ich bin selbst die Wurzel und der Controller"
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Konnte LabeledTextField.fxml nicht laden", e);
        }

        // Binding: Verbindet Variable mit Label
        if (hintLabel != null) {
            hintLabel.textProperty().bind(hintTextProperty);
        } else {
            System.err.println("ACHTUNG: hintLabel ist null! FXML wurde nicht korrekt injiziert.");
        }
    }

    // === Getter und Setter für FXML ===

    public String getHintText() {
        return hintTextProperty.get();
    }

    public void setHintText(String value) {
        this.hintTextProperty.set(value);
    }

    public StringProperty hintTextProperty() {
        return hintTextProperty;
    }
}
