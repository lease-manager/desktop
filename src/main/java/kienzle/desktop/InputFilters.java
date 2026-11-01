package kienzle.desktop;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

import java.util.regex.Pattern;

/** Blocks characters that can never be valid while typing or pasting. */
final class InputFilters {
    private static final Pattern PARCEL_NUMBER = Pattern.compile("\\d*(/\\d*)?");
    private static final Pattern DECIMAL = Pattern.compile("[0-9.,]*");

    private InputFilters() {
    }

    static void parcelNumber(TextInputControl control) {
        restrict(control, PARCEL_NUMBER);
    }

    static void decimal(TextInputControl control) {
        restrict(control, DECIMAL);
    }

    // Installed after the initial text is set, so existing (legacy) values are never dropped.
    private static void restrict(TextInputControl control, Pattern allowed) {
        control.setTextFormatter(new TextFormatter<String>(change ->
                allowed.matcher(change.getControlNewText()).matches() ? change : null));
    }
}
