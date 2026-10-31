package kienzle.desktop;

import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlagIconsTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void renderGermanAndUkFlags() throws Exception {
        Node germany = onFxThread(FlagIcons::germany);
        Node uk = onFxThread(FlagIcons::unitedKingdom);

        assertNotNull(germany);
        assertNotNull(uk);
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
