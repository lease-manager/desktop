package kienzle.desktop;

import javafx.scene.control.Alert;

/** Centralizes user-facing feedback for the desktop UI. */
public final class UiFeedback {
    public void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    public void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    public boolean confirm(String message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message);
        return confirm.showAndWait()
                .filter(button -> button == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }

    public java.util.Optional<Integer> choose(String title, String message, java.util.List<String> options) {
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(options.get(0), options);
        dialog.setTitle(title);
        dialog.setHeaderText(message);
        return dialog.showAndWait().map(options::indexOf);
    }
}
