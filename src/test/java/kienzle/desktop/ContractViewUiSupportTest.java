package kienzle.desktop;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import kienzle.desktop.app.AppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractViewUiSupportTest {
    @TempDir
    Path tempDir;

    @BeforeAll
    static void startToolkit() throws Exception {
        JavaFxTestSupport.startToolkit();
    }

    @AfterEach
    void resetLocale() {
        I18n.setLocale(Locale.GERMAN);
    }

    @Test
    void switchLanguagePersistsConfigurationInTheActiveDataDirectory() throws Exception {
        AppContext context = AppContext.create(tempDir);
        I18n.setLocale(Locale.GERMAN);
        ViewLoader.Loaded loaded = onFxThread(() -> ViewLoader.load(context));
        Scene scene = onFxThread(() -> new Scene(loaded.root()));

        FutureTask<Void> switchLanguage = new FutureTask<>(() -> {
            ContractViewUiSupport.switchLanguage(context, scene, Locale.ENGLISH, -1, StatusFilter.ALL,
                    loaded.controller().snapshotFormState());
            return null;
        });
        Platform.runLater(switchLanguage);
        switchLanguage.get(10, TimeUnit.SECONDS);

        Properties config = new Properties();
        try (var input = Files.newInputStream(tempDir.resolve("config.properties"))) {
            config.load(input);
        }
        assertEquals("en", config.getProperty("language"));
    }

    @Test
    void formStateRoundTripsUnsavedRawValuesAcrossViewRecreation() throws Exception {
        I18n.setLocale(Locale.GERMAN);
        ContractFormController.FormState state = onFxThread(() -> {
            ContractFormController form = loadFormController();
            getField(form, "contractNameField", TextField.class).setText("Unsaved name");
            form.parcelEditor().setDrafts(java.util.List.of(
                    new ParcelEditor.Draft("1/2", "Flur", "not a number", "", "", "10,00"),
                    new ParcelEditor.Draft("1/3", "", "", "", "", "")));
            getField(form, "startDateField", DatePicker.class).getEditor().setText("invalid date text");
            getField(form, "hasIntroductoryRentCheckBox", CheckBox.class).setSelected(true);
            getField(form, "durationField", ComboBox.class).getSelectionModel().select(1);
            return form.snapshot();
        });
        ContractFormController.FormState restored = onFxThread(() -> {
            ContractFormController form = loadFormController();
            form.restore(state);
            return form.snapshot();
        });

        assertEquals(state, restored);
    }

    private static ContractFormController loadFormController() throws Exception {
        FXMLLoader loader = new FXMLLoader(ContractFormController.class.getResource("ContractForm.fxml"),
                I18n.bundle());
        loader.load();
        return loader.getController();
    }

    private static <T> T getField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static <T> T onFxThread(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
