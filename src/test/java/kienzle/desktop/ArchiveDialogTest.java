package kienzle.desktop;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import kienzle.desktop.app.AppContext;
import kienzle.desktop.contract.ContractEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveDialogTest {
    @TempDir
    Path tempDir;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @Test
    void refreshEntries_populatesArchivedContractsAndKeepsRestoreDisabledWithoutSelection() throws Exception {
        AppContext context = AppContext.create(tempDir);
        ContractEntry first = context.workflow().save(null, LeaseContract.builder().name("Alpha").build());
        ContractEntry second = context.workflow().save(null, LeaseContract.builder().name("Beta").build());
        context.workflow().archive(first);
        context.workflow().archive(second);

        ArchiveDialog dialog = onFxThread(() -> new ArchiveDialog(context.workflow(), new Stage(), restored -> { }));
        onFxThread(() -> {
            invoke(dialog, "refreshEntries");
            return null;
        });

        ListView<?> list = (ListView<?>) getField(dialog, "list");
        Button restoreButton = (Button) getField(dialog, "restoreButton");

        assertEquals(2, list.getItems().size());
        assertTrue(restoreButton.isDisable());
    }

    @Test
    void restoreSelected_withoutSelection_doesNothing() throws Exception {
        AppContext context = AppContext.create(tempDir);
        ContractEntry entry = context.workflow().save(null, LeaseContract.builder().name("Only").build());
        context.workflow().archive(entry);

        ArchiveDialog dialog = onFxThread(() -> new ArchiveDialog(context.workflow(), new Stage(), restored -> { }));
        onFxThread(() -> {
            invoke(dialog, "refreshEntries");
            invoke(dialog, "restoreSelected");
            return null;
        });

        assertEquals(1, context.workflow().listArchived().size());
        assertEquals(0, context.workflow().list().size());
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
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
