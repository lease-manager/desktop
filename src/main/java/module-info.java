module kienzle.desktop {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens kienzle.desktop to javafx.fxml;
    exports kienzle.desktop;
}