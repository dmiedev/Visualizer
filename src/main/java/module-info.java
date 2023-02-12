/**
 * A simple music visualizer program made with JavaFX.
 */
module dev.dmie.visualizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens dev.dmie.visualizer to javafx.fxml;
    exports dev.dmie.visualizer;
}