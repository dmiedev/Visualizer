package dev.dmie.visualizer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The music visualizer application.
 */
public class Application extends javafx.application.Application {
    /**
     * @see javafx.application.Application#start(Stage)
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 400);
        stage.setResizable(false);
        stage.setTitle("Music Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The entrypoint of the program.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        launch();
    }
}