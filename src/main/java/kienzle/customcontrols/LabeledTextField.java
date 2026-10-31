package kienzle.customcontrols;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class LabeledTextField extends StackPane {

    @FXML private TextField textField;
    @FXML private Label hintLabel;

    // Property holding the hint text
    private final StringProperty hintTextProperty = new SimpleStringProperty("");

    public LabeledTextField() {
        loadFXML();
    }

    // Constructor for manual creation in code (optional)
    public LabeledTextField(String hint) {
        this();
        setHintText(hint);
    }

    private void loadFXML() {
        FXMLLoader loader = new FXMLLoader(requireResource("/kienzle/desktop/LabeledTextField.fxml"));

        // IMPORTANT: tell the loader that this instance is both the root and the controller
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Could not load LabeledTextField.fxml", e);
        }

        // Binding: connects the property to the label
        if (hintLabel != null) {
            hintLabel.textProperty().bind(hintTextProperty);
        } else {
            System.err.println("WARNING: hintLabel is null! The FXML was not injected correctly.");
        }
    }

    private static URL requireResource(String path) {
        URL url = LabeledTextField.class.getResource(path);
        return Objects.requireNonNull(url, () -> "Missing JavaFX resource: " + path);
    }

    // === Getters and setters for FXML ===

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
