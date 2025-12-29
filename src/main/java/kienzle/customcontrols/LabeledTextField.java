package kienzle.customcontrols;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class LabeledTextField extends StackPane {
    @FXML private TextField textField;
    @FXML private Label hintLabel;

    public LabeledTextField(String hintText) {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("LabeledTextField.fxml")
        );
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load LabeledTextField.fxml. Ensure the file exists in the correct package.",
                    e
            );
        }

        // Configure the hint label
        hintLabel.setText(hintText);
        StackPane.setAlignment(hintLabel, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(hintLabel, new javafx.geometry.Insets(2, 0, 0, 5));
    }

    public TextField getTextField() {
        return textField;
    }
}
