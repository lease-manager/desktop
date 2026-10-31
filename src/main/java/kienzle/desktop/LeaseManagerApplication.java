package kienzle.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kienzle.desktop.app.AppContext;

import java.io.IOException;

public class LeaseManagerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        I18n.loadFromConfig(I18n.defaultConfigPath());
        Scene scene = new Scene(ViewLoader.load(AppContext.createDefault()).root(), 1100, 700);
        stage.setTitle(I18n.get("app.title"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
