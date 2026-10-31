package kienzle.desktop;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import kienzle.desktop.app.AppContext;

import java.io.IOException;

/** Loads the main view and injects the application context into its controller. */
public final class ViewLoader {
    private ViewLoader() {
    }

    public static Loaded load(AppContext context) throws IOException {
        FXMLLoader loader = new FXMLLoader(ViewLoader.class.getResource("ContractView.fxml"), I18n.bundle());
        loader.setControllerFactory(type -> type == MainLayoutController.class
                ? new MainLayoutController(context) : newInstance(type));
        Parent root = loader.load();
        return new Loaded(root, loader.getController());
    }

    private static Object newInstance(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Loaded(Parent root, MainLayoutController controller) {
    }
}
