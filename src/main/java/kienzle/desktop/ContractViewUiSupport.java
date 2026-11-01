package kienzle.desktop;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kienzle.desktop.app.AppContext;

import java.io.IOException;
import java.util.Locale;

/** UI support for view reloading and locale switching. */
public final class ContractViewUiSupport {
    private ContractViewUiSupport() {
    }

    public static void switchLanguage(AppContext context, Scene scene, Locale locale, int selectedIndex,
                                      StatusFilter activeFilter, ContractFormController.FormState formState) {
        if (locale.getLanguage().equals(I18n.locale().getLanguage())) {
            return;
        }
        I18n.setLocale(locale);
        try {
            I18n.saveToConfig(context.dataDir().resolve("config.properties"));
        } catch (IOException ignored) {
            // The language still changes for this session if the config cannot be written.
        }
        try {
            ViewLoader.Loaded loaded = ViewLoader.load(context);
            loaded.controller().restoreSelection(Math.max(selectedIndex, -1), activeFilter);
            loaded.controller().restoreFormState(formState);
            Parent root = loaded.root();
            scene.setRoot(root);
            if (scene.getWindow() instanceof Stage stage) {
                stage.setTitle(I18n.get("app.title"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
