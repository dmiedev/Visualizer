package dev.dmie.visualizer;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * The controller of the main application view.
 */
public class MainController implements Initializable {
    /**
     * Main logger.
     */
    private static final Logger logger = LogManager.getLogger("dev.dmie");

    /**
     * A {@code Label} that contains the name of the current loaded playlist.
     */
    @FXML
    private Label playlistLabel;

    /**
     * A {@code Label} that contains the current time of the current track.
     */
    @FXML
    private Label currentTimeLabel;

    /**
     * A {@code Label} that contains the duration of the current track.
     */
    @FXML
    private Label durationLabel;

    /**
     * A {@code Label} that contains the name of the current track.
     */
    @FXML
    private Label trackLabel;

    /**
     * A {@code Slider} that controls the playback of the current track.
     */
    @FXML
    private Slider playbackSlider;

    /**
     * A {@code Slider} that controls the volume of the current track.
     */
    @FXML
    private Slider volumeSlider;

    /**
     * A {@code Button} that pauses or plays the current track.
     */
    @FXML
    private Button playbackButton;

    /**
     * A {@code Button} that switches to the previous track in the playlist.
     */
    @FXML
    private Button previousTrackButton;

    /**
     * A {@code Button} that switches to the next track in the playlist.
     */
    @FXML
    private Button nextTrackButton;

    /**
     * A {@code Pane} that the visualization is drawn on.
     */
    @FXML
    private Pane spectrumCanvas;


    /**
     * The tracks of the currently loaded playlist.
     */
    private File[] tracks;

    /**
     * The index of the current track in {@link MainController#tracks}.
     */
    private int currentTrackIndex = 0;


    /**
     * The player of the current track.
     */
    private MediaPlayer player;

    /**
     * The visualizer.
     */
    private Visualizer visualizer;


    /**
     * The playback status of the current track.
     */
    private PlaybackStatus playbackStatus = PlaybackStatus.PAUSED;


    /**
     * Called to initialize a controller after its root element has been completely processed.
     * @param url The location used to resolve relative paths for the root object, or {@code null} if the location is
     *            not known.
     *
     * @param resourceBundle The resources used to localize the root object, or {@code null} if the root object was not
     *                       localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playbackSlider.valueProperty().addListener(this::onPlaybackSliderValueChanging);
    }

    /**
     * Reacts to the value of {@link MainController#playbackSlider} changing.
     * @param observable The {@code ObservableValue} which value changed
     * @param oldValue The old value
     * @param newValue The new value
     */
    private void onPlaybackSliderValueChanging(
            ObservableValue<? extends Number> observable,
            Number oldValue,
            Number newValue) {
        double newPosition = newValue.doubleValue() * player.getCycleDuration().toMillis();
        setTimeLabel(currentTimeLabel, new Duration(newPosition));
    }

    /**
     * Shows a {@code DirectoryChooser} to select a playlist directory.
     * @return The selected playlist
     */
    private File showPlaylistChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a Playlist");
        Stage stage = (Stage) trackLabel.getScene().getWindow();
        return directoryChooser.showDialog(stage);
    }

    /**
     * Shows an {@code Alert} with a message that the program failed to load a track.
     * @param trackName The name of the track
     */
    private void showTrackFailureAlert(String trackName) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed to load track");
        alert.setHeaderText("Failed to load track");
        alert.setContentText("An error occurred while trying to load the track \"" + trackName + "\".");
        alert.showAndWait();
    }

    /**
     * Shows an {@code Alert} with a message that the program failed to load a playlist.
     * @param playlistName The name of the playlist.
     */
    private void showPlaylistFailureAlert(String playlistName) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Failed to load playlist");
        alert.setHeaderText("Failed to load playlist");
        alert.setContentText("An error occurred while trying to load the playlist \"" + playlistName + "\".");
        alert.showAndWait();
    }

    /**
     * Reacts to the click on the "Select Playlist" button.
     */
    @FXML
    protected void onSelectPlaylistButtonClicked() {
        File playlist = showPlaylistChooser();
        if (playlist == null) {
            return;
        }
        loadPlaylist(playlist);
    }

    /**
     * Loads tracks of a playlist.
     * @param playlist The playlist
     */
    private void loadPlaylist(File playlist) {
        File[] loadedTracks;
        try {
            loadedTracks = playlist.listFiles(
                    (file) -> file.isFile() && file.getName().matches(".+\\.(aac|m4a|mp3|pcm|wav|aiff)"));
        } catch (SecurityException exception) {
            logger.error("Access to the playlist was denied");
            showPlaylistFailureAlert(playlist.getName());
            return;
        }
        if (loadedTracks == null) {
            logger.error("An I/O error occurred while loading playlist");
            showPlaylistFailureAlert(playlist.getName());
            return;
        }
        tracks = loadedTracks;
        currentTrackIndex = 0;
        playlistLabel.setText(playlist.getName());
        if (Arrays.asList(loadedTracks).isEmpty()) {
            logger.info("The loaded playlist is empty");
            if (player != null) {
                stop();
            }
            trackLabel.setText("No Tracks Found");
            setControlsDisable(true);
            return;
        }
        Arrays.sort(loadedTracks);
        loadCurrentTrack();
    }

    /**
     * Sets the text of the {@link MainController#trackLabel} based on the current track.
     */
    private void setTrackLabelText() {
        String trackName = tracks[currentTrackIndex].getName();
        String trackLabelText = String.format("[ %d / %d ]   %s", currentTrackIndex + 1, tracks.length, trackName);
        trackLabel.setText(trackLabelText);
    }

    /**
     * Loads the current track.
     */
    private void loadCurrentTrack() {
        logger.info("Loading current track");
        if (player != null) {
            stop();
        }
        setControlsDisable(true);
        setTrackLabelText();

        File currentTrack = tracks[currentTrackIndex];

        if (!currentTrack.exists()) {
            logger.error("Trying to play non-existing track");
            showTrackFailureAlert(currentTrack.getName());
            enableTrackButtons();
            return;
        }
        if (currentTrack.getName().endsWith(".mp3")) {
            logger.warn("Loading an mp3 file, bad length computation is possible");
        }
        try {
            Media media = new Media(currentTrack.toURI().toString());
            player = new MediaPlayer(media);
        } catch (MediaException exception) {
            logger.error("Failed to create MediaPlayer");
            showTrackFailureAlert(currentTrack.getName());
            enableTrackButtons();
            return;
        }

        setUpPlayer();
    }

    /**
     * Sets up the {@link MainController#player}.
     */
    private void setUpPlayer() {
        logger.info("Setting up player");
        player.setOnReady(this::onPlayerReady);
        player.setOnEndOfMedia(this::onPlayerTrackEnd);
        player.currentTimeProperty().addListener(this::onPlayerCurrentTimeChange);
        player.volumeProperty().bind(volumeSlider.valueProperty());

        player.setAudioSpectrumInterval(1 / 40.0);
        player.setAudioSpectrumThreshold(-80);
        player.setAudioSpectrumNumBands(25);

        if (visualizer == null) {
            logger.info("Creating a new visualizer");
            visualizer = new Visualizer(
                    spectrumCanvas,
                    player.getAudioSpectrumNumBands(),
                    player.getAudioSpectrumThreshold(),
                    80.0f);
            visualizer.setUp();
        } else {
            logger.info("Clearing existing visualizer");
            visualizer.clear();
        }
        player.setAudioSpectrumListener(visualizer);
    }

    /**
     * Reacts to the change of {@link MainController#player}'s {@code currentTime}.
     * @param observable The {@code ObservableValue} which value changed
     * @param oldValue The old value
     * @param newValue The new value
     */
    private void onPlayerCurrentTimeChange(
            ObservableValue<? extends Duration> observable,
            Duration oldValue,
            Duration newValue) {
        double percent = newValue.toMillis() / player.getCycleDuration().toMillis();
        playbackSlider.setValue(percent);
    }

    /**
     * Enables or disables the controls of this controller.
     * @param disabled Whether the controls should be disabled
     */
    private void setControlsDisable(boolean disabled) {
        playbackButton.setDisable(disabled);
        volumeSlider.setDisable(disabled);
        playbackSlider.setDisable(disabled);
        previousTrackButton.setDisable(disabled);
        nextTrackButton.setDisable(disabled);
    }

    /**
     * Enables {@link MainController#previousTrackButton} and {@link MainController#nextTrackButton}.
     */
    private void enableTrackButtons() {
        previousTrackButton.setDisable(false);
        nextTrackButton.setDisable(false);
    }

    /**
     * Called when the {@link MainController#player} is ready.
     */
    private void onPlayerReady() {
        setControlsDisable(false);
        volumeSlider.setValue(player.getVolume());
        setTimeLabel(durationLabel, player.getCycleDuration());
    }

    /**
     * Sets the text of a time label based on the provided {@code duration}.
     * @param label The time label.
     * @param duration The duration
     */
    private void setTimeLabel(Label label, Duration duration) {
        int minutes = Double.valueOf(duration.toMinutes()).intValue();
        int seconds = Double.valueOf(duration.toSeconds() % 60).intValue();
        String durationText = String.format("%d:%02d", minutes, seconds);
        label.setText(durationText);
    }

    /**
     * Called when the {@link MainController#player} finishes playing the current track.
     */
    private void onPlayerTrackEnd() {
        if (++currentTrackIndex >= tracks.length) {
            currentTrackIndex = 0;
        }
        loadCurrentTrack();
        if (currentTrackIndex != 0) {
            play();
        }
    }

    /**
     * Stops the playback.
     */
    private void stop() {
        logger.debug("Setting all values to zero");
        player.seek(Duration.ZERO);
        currentTimeLabel.setText("0:00");
        durationLabel.setText("0:00");
        playbackSlider.setValue(0.0);
        pause();
    }

    /**
     * Called when the {@link MainController#playbackButton} is pressed.
     */
    @FXML
    protected void onPlaybackButtonPressed() {
        logger.info("Playback button pressed");
        if (playbackStatus == PlaybackStatus.PLAYING) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Called when the {@link MainController#playbackSlider} is pressed.
     */
    @FXML
    protected void onPlaybackSliderMousePressed() {
        logger.debug("Pausing while slider is pressed");
        player.pause();
    }

    /**
     * Called when the {@link MainController#playbackSlider} is released.
     */
    @FXML
    protected void onPlaybackSliderMouseReleased() {
        Duration position = player.getCycleDuration().multiply(playbackSlider.getValue());
        logger.debug("Playback slider released at position {}", position.toString());
        player.seek(position);
        if (playbackStatus == PlaybackStatus.PLAYING) {
            play();
        }
    }

    /**
     * Resumes or starts the playback of the current track.
     */
    private void play() {
        logger.info("Starting playback");
        player.play();
        playbackButton.setText("❚ ❚");
        playbackStatus = PlaybackStatus.PLAYING;
    }

    /**
     * Pauses the playback of current track.
     */
    private void pause() {
        logger.info("Pausing playback");
        player.pause();
        playbackButton.setText("▶️");
        playbackStatus = PlaybackStatus.PAUSED;
    }

    /**
     * Called when the {@link MainController#nextTrackButton} is clicked.
     */
    @FXML
    protected void onNextTrackButtonClicked() {
        logger.info("Next button pressed");
        if (++currentTrackIndex >= tracks.length) {
            logger.debug("Moving to the start of the playlist");
            currentTrackIndex = 0;
        }
        loadCurrentTrack();
        play();
    }

    /**
     * Called when the {@link MainController#previousTrackButton} is clicked.
     */
    @FXML
    protected void onPreviousTrackButtonClicked() {
        logger.info("Previous button pressed");
        if (--currentTrackIndex < 0) {
            logger.debug("Moving to the end of the playlist");
            currentTrackIndex = tracks.length - 1;
        }
        loadCurrentTrack();
        play();
    }
}