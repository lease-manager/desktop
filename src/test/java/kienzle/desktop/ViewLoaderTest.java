package kienzle.desktop;

import javafx.application.Platform;
import javafx.scene.Node;
import kienzle.desktop.app.AppContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ViewLoaderTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void load_buildsMainViewAndController() throws Exception {
        AppContext context = AppContext.create(Path.of(System.getProperty("java.io.tmpdir"), "lease-view-loader-test"));
        ViewLoader.Loaded loaded = onFxThread(() -> {
            try {
                return ViewLoader.load(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertNotNull(loaded.root());
        assertNotNull(loaded.controller());
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
