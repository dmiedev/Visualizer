module dev.dmie.visualizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens dev.dmie.visualizer to javafx.fxml;
    exports dev.dmie.visualizer;
}