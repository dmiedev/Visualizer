/**
 * A simple music visualizer program made with JavaFX.
 */
module dev.dmie.visualizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires org.apache.logging.log4j;

    opens dev.dmie.visualizer to javafx.fxml;
    exports dev.dmie.visualizer;
}