package kienzle.customcontrols;

import javafx.application.Platform;
import kienzle.desktop.JavaFxTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabeledTextFieldTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void constructor_setsAndUpdatesHintText() throws Exception {
        LabeledTextField field = onFxThread(() -> new LabeledTextField("Vorname"));

        assertEquals("Vorname", onFxThread(field::getHintText));
        onFxThread(() -> {
            field.setHintText("Nachname");
            return null;
        });
        assertEquals("Nachname", onFxThread(field::getHintText));
        assertEquals("Nachname", onFxThread(field.hintTextProperty()::get));
    }

    private static <T> T onFxThread(ThrowingSupplier<T> supplier) throws Exception {
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
