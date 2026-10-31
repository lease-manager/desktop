package kienzle.desktop;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class JavaFxTestSupport {
    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean(false);

    private JavaFxTestSupport() {
    }

    public static void startToolkit() throws Exception {
        if (TOOLKIT_STARTED.compareAndSet(false, true)) {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start in time");
        }
    }
}
