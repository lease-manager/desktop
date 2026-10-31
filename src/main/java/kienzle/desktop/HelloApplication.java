package kienzle.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        I18n.loadFromConfig(I18n.defaultConfigPath());
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("ContractView.fxml"),
                I18n.bundle());
        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);
        stage.setTitle(I18n.get("app.title"));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}