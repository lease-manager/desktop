module kienzle.desktop {
    requires javafx.controls;
    requires javafx.fxml;

    // Automatic module name for ControlsFX 11.2.1
    requires org.controlsfx.controls;
    // Module name for FormsFX
    requires com.dlsc.formsfx;
    //requires javafx.swt;

    opens kienzle.desktop to javafx.fxml;
    exports kienzle.desktop;
    exports kienzle.customcontrols;
    opens kienzle.customcontrols to javafx.fxml;
}
