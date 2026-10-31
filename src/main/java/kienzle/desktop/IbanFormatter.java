package kienzle.desktop;

import javafx.scene.control.TextFormatter;

import java.util.Locale;

/** Groups an IBAN in blocks of four characters while typing. */
public final class IbanFormatter {
    private static final int MAX_LENGTH = 34;

    private IbanFormatter() {
    }

    public static String format(String rawValue) {
        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < rawValue.length(); index++) {
            if (index > 0 && index % 4 == 0) {
                formatted.append(' ');
            }
            formatted.append(rawValue.charAt(index));
        }
        return formatted.toString();
    }

    public static String normalize(String text) {
        String raw = text.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return raw.length() > MAX_LENGTH ? raw.substring(0, MAX_LENGTH) : raw;
    }

    public static TextFormatter<String> textFormatter() {
        return new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            String original = change.getControlText();
            int caretPosition = Math.min(change.getRangeStart(), original.length());
            String rawBeforeCaret = original.substring(0, caretPosition) + change.getText();
            int rawCaretPosition = rawBeforeCaret.replaceAll("[^A-Za-z0-9]", "").length();
            String formattedValue = format(normalize(change.getControlNewText()));
            int spacesBeforeCaret = rawCaretPosition == 0 ? 0 : (rawCaretPosition - 1) / 4;
            int formattedCaret = Math.min(formattedValue.length(), rawCaretPosition + spacesBeforeCaret);
            change.setRange(0, original.length());
            change.setText(formattedValue);
            change.setCaretPosition(formattedCaret);
            change.setAnchor(formattedCaret);
            return change;
        });
    }
}
