module kienzle.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires org.apache.commons.logging;
    requires com.google.gson;
    requires com.google.zxing;

    // Automatic module name for ControlsFX 11.2.1
    requires org.controlsfx.controls;
    // Module name for FormsFX
    requires com.dlsc.formsfx;
    //requires javafx.swt;

    opens kienzle.desktop to javafx.fxml, com.google.gson;
    exports kienzle.desktop;
    exports kienzle.desktop.contract;
    exports kienzle.desktop.pdf;
    exports kienzle.desktop.app;
    opens kienzle.desktop.app to com.google.gson;
    opens kienzle.desktop.contract to com.google.gson;
    exports kienzle.customcontrols;
    opens kienzle.customcontrols to javafx.fxml;
}
