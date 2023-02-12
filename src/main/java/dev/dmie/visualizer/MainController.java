package dev.dmie.visualizer;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * The controller of the main application view.
 */
public class MainController implements Initializable {
    @FXML
    private Label playlistLabel;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private Label trackLabel;
    @FXML
    private Slider playbackSlider;
    @FXML
    private Slider volumeSlider;
    @FXML
    private Button playbackButton;
    @FXML
    private Button previousTrackButton;
    @FXML
    private Button nextTrackButton;
    @FXML
    private Pane spectrumCanvas;

    /**
     * The
     */
    private File[] tracks;
    private int currentTrackIndex = 0;

    private MediaPlayer player;
    private AudioSpectrumCanvasListener spectrumListener;
    private PlaybackStatus playbackStatus = PlaybackStatus.PAUSED;

    private boolean spectrumCanvasIsPrepared = false;

    private Rectangle[] spectrumBars;
    private final int SPECTRUM_BARS_COUNT = 25;

    private Circle[][] spectrumDots;
    private final double SPECTRUM_DOT_MIN_RADIUS = 3;
    private final double SPECTRUM_DOT_MAX_RADIUS = 6;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playbackSlider.valueProperty().addListener(this::onPlaybackSliderValueChanging);
    }

    private void onPlaybackSliderValueChanging(
            ObservableValue<? extends Number> observable,
            Number oldValue,
            Number newValue) {
        double millis = newValue.doubleValue() * player.getCycleDuration().toMillis();
        Duration newDuration = new Duration(millis);
        setTimeLabel(currentTimeLabel, newDuration);
    }

    private File showDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a Playlist");
        Stage stage = (Stage) trackLabel.getScene().getWindow();
        return directoryChooser.showDialog(stage);
    }

    private void showTrackFailureAlert(String trackName) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed to load track");
        alert.setHeaderText("Failed to load track");
        alert.setContentText("An error occurred while trying to load the track \"" + trackName + "\".");
        alert.showAndWait();
    }

    private void showPlaylistFailureAlert(String playlistName) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed to load playlist");
        alert.setHeaderText("Failed to load playlist");
        alert.setContentText("An error occurred while trying to load the playlist \"" + playlistName + "\".");
        alert.showAndWait();
    }

    @FXML
    protected void onSelectPlaylistButtonClicked() {
        File playlist = showDirectoryChooser();
        if (playlist == null) {
            return;
        }
        loadPlaylist(playlist);
    }

    private void loadPlaylist(File playlist) {
        File[] loadedTracks;
        try {
            loadedTracks = playlist.listFiles(
                    (file) -> file.isFile() && file.getName().matches(".+\\.(aac|m4a|mp3|pcm|wav|aiff)"));
        } catch (SecurityException exception) {
            showPlaylistFailureAlert(playlist.getName());
            return;
        }
        if (loadedTracks == null) {
            showPlaylistFailureAlert(playlist.getName());
            return;
        }
        this.tracks = loadedTracks;
        currentTrackIndex = 0;
        playlistLabel.setText(playlist.getName());
        if (Arrays.asList(loadedTracks).isEmpty()) {
            if (player != null) {
                stop();
            }
            trackLabel.setText("No Tracks Found");
            setControlsDisable(true);
            return;
        }
        Arrays.sort(loadedTracks);
        prepareSpectrumCanvas();
        loadCurrentTrack();
    }

    private void prepareSpectrumCanvas() {
        if (spectrumCanvasIsPrepared) {
            return;
        }
        setUpSpectrumDots();
        setUpSpectrumBars();
        spectrumCanvasIsPrepared = true;
    }

    private void setTrackLabelText() {
        String trackName = tracks[currentTrackIndex].getName();
        String trackLabelText = String.format("[ %d / %d ]   %s", currentTrackIndex + 1, tracks.length, trackName);
        trackLabel.setText(trackLabelText);
    }

    private void loadCurrentTrack() {
        if (player != null) {
            stop();
        }
        setControlsDisable(true);
        setTrackLabelText();

        if (!tracks[currentTrackIndex].exists()) {
            showTrackFailureAlert(tracks[currentTrackIndex].getName());
            enableTrackButtons();
            return;
        }
        try {
            Media media = new Media(tracks[currentTrackIndex].toURI().toString());
            player = new MediaPlayer(media);
        } catch (MediaException exception) {
            showTrackFailureAlert(tracks[currentTrackIndex].getName());
            enableTrackButtons();
            return;
        }

        setUpPlayer();
    }

    private void setUpPlayer() {
        player.setOnReady(this::onPlayerReady);
        player.setOnEndOfMedia(this::onPlayerTrackEnd);
        player.currentTimeProperty().addListener(this::onPlayerCurrentTimeChange);
        player.volumeProperty().bind(volumeSlider.valueProperty());

        player.setAudioSpectrumInterval(1 / 40.0);
        player.setAudioSpectrumThreshold(-80);
        player.setAudioSpectrumNumBands(SPECTRUM_BARS_COUNT);

        if (spectrumListener == null) {
            createSpectrumListener();
        } else {
            spectrumListener.clear();
        }
        player.setAudioSpectrumListener(spectrumListener);
    }

    private void createSpectrumListener() {
        spectrumListener = new AudioSpectrumCanvasListener(
                spectrumBars,
                spectrumCanvas.getHeight(),
                spectrumDots,
                SPECTRUM_DOT_MIN_RADIUS,
                SPECTRUM_DOT_MAX_RADIUS,
                player.getAudioSpectrumThreshold(),
                80.0);
    }

    private void onPlayerCurrentTimeChange(
            ObservableValue<? extends Duration> observable,
            Duration oldValue,
            Duration newValue) {
        double percent = newValue.toMillis() / player.getCycleDuration().toMillis();
        playbackSlider.setValue(percent);
    }

    private void setControlsDisable(boolean disabled) {
        playbackButton.setDisable(disabled);
        volumeSlider.setDisable(disabled);
        playbackSlider.setDisable(disabled);
        previousTrackButton.setDisable(disabled);
        nextTrackButton.setDisable(disabled);
    }

    private void enableTrackButtons() {
        previousTrackButton.setDisable(false);
        nextTrackButton.setDisable(false);
    }

    private void onPlayerReady() {
        setControlsDisable(false);
        volumeSlider.setValue(player.getVolume());
        setTimeLabel(durationLabel, player.getCycleDuration());
    }

    private void setTimeLabel(Label label, Duration duration) {
        int minutes = Double.valueOf(duration.toMinutes()).intValue();
        int seconds = Double.valueOf(duration.toSeconds() % 60).intValue();
        String durationText = String.format("%d:%02d", minutes, seconds);
        label.setText(durationText);
    }

    private void onPlayerTrackEnd() {
        if (++currentTrackIndex >= tracks.length) {
            currentTrackIndex = 0;
        }
        loadCurrentTrack();
        if (currentTrackIndex != 0) {
            play();
        }
    }

    private void stop() {
        player.seek(Duration.ZERO);
        currentTimeLabel.setText("0:00");
        durationLabel.setText("0:00");
        playbackSlider.setValue(0.0);
        pause();
    }

    @FXML
    protected void onPlaybackButtonPressed() {
        if (playbackStatus == PlaybackStatus.PLAYING) {
            pause();
        } else {
            play();
        }
    }

    @FXML
    protected void onPlaybackSliderMousePressed() {
        player.pause();
    }

    @FXML
    protected void onPlaybackSliderMouseReleased() {
        Duration position = player.getCycleDuration().multiply(playbackSlider.getValue());
        player.seek(position);
        if (playbackStatus == PlaybackStatus.PLAYING) {
            play();
        }
    }

    private void play() {
        player.play();
        playbackButton.setText("❚ ❚");
        playbackStatus = PlaybackStatus.PLAYING;
    }

    private void pause() {
        player.pause();
        playbackButton.setText("▶️");
        playbackStatus = PlaybackStatus.PAUSED;
    }

    private void setUpSpectrumBars() {
        spectrumBars = new Rectangle[SPECTRUM_BARS_COUNT];

        double canvasWidth = spectrumCanvas.getWidth();
        double canvasHeight = spectrumCanvas.getHeight();
        final double barPadding = 10.0;
        final double bottomPadding = 5.0;
        double barWidth = (canvasWidth - (SPECTRUM_BARS_COUNT + 1) * barPadding) / SPECTRUM_BARS_COUNT;

        for (int i = 0; i < SPECTRUM_BARS_COUNT; i++) {
            Color color = Color.hsb((double) i / SPECTRUM_BARS_COUNT * 30, 1, 1);
            Rectangle bar = new Rectangle(barWidth, 0, color);
            bar.setArcHeight(10);
            bar.setArcWidth(20);
            bar.relocate(barPadding + (barWidth + barPadding) * i, canvasHeight - bottomPadding);
            spectrumBars[i] = bar;
        }
        spectrumCanvas.getChildren().addAll(spectrumBars);
    }

    private void setUpSpectrumDots() {
        final int rowsNumber = 5;
        final int columnsNumber = 13;
        spectrumDots = new Circle[rowsNumber][columnsNumber];

        double canvasWidth = spectrumCanvas.getWidth();
        double canvasHeight = spectrumCanvas.getHeight();
        final int padding = 20;
        double horizontalMargin =
                (canvasWidth - padding * 2 - SPECTRUM_DOT_MIN_RADIUS * 2 * columnsNumber) / (columnsNumber - 1);
        double verticalMargin =
                (canvasHeight - padding * 2 - SPECTRUM_DOT_MIN_RADIUS * 2 * rowsNumber) / (rowsNumber - 1);
        final Color color = Color.rgb(255, 255, 255, 0.5);

        for (int i = 0; i < rowsNumber; i++) {
            double rowPosition = padding + (SPECTRUM_DOT_MIN_RADIUS * 2 + verticalMargin) * i;
            for (int j = 0; j < columnsNumber; j++) {
                double radius = (SPECTRUM_DOT_MAX_RADIUS - SPECTRUM_DOT_MIN_RADIUS) / 2 + SPECTRUM_DOT_MIN_RADIUS;
                Circle dot = new Circle(radius, color);
                dot.relocate(padding + (SPECTRUM_DOT_MIN_RADIUS * 2 + horizontalMargin) * j, rowPosition);
                spectrumDots[i][j] = dot;
            }
            spectrumCanvas.getChildren().addAll(spectrumDots[i]);
        }
    }

    @FXML
    protected void onNextTrackButtonClicked() {
        if (++currentTrackIndex >= tracks.length) {
            currentTrackIndex = 0;
        }
        loadCurrentTrack();
        play();
    }

    @FXML
    protected void onPreviousTrackButtonClicked() {
        if (--currentTrackIndex < 0) {
            currentTrackIndex = tracks.length - 1;
        }
        loadCurrentTrack();
        play();
    }
}