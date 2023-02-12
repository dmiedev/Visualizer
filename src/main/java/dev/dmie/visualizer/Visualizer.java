package dev.dmie.visualizer;

import javafx.scene.layout.Pane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;

/**
 * A simple music visualizer with spectrum bars and dots in the background.
 */
public class Visualizer implements AudioSpectrumListener {
    /**
     * A canvas that visualization is drawn on.
     */
    private final Pane canvas;

    /**
     * Bars that represent magnitude of each band of the current track.
     */
    private Rectangle[] bars;

    /**
     * The maximum height of a bar.
     */
    private final double maxBarHeight;


    /**
     * Dots that change their radius on each beat of the current track.
     */
    private Circle[][] dots;

    /**
     * The minimum radius of a dot.
     */
    private final double minDotRadius;

    /**
     * The maximum radius of a dot.
     */
    private final double maxDotRadius;

    /**
     * Whether the dot animation is going forward.
     */
    private boolean dotsAnimationForward = true;


    /**
     * The spectrum threshold.
     */
    private final int spectrumThreshold;

    /**
     * The maximum allowed magnitude.
     */
    private final float maxMagnitude;


    /**
     * The magnitude buffer used for smooth visualization.
     */
    private final float[] magnitudeBuffer;

    /**
     * The magnitude average across all bands from the previous call of {@link Visualizer#spectrumDataUpdate}.
     */
    private float priorMagnitudeAverage = 0.0f;

    /**
     * The timestamp when the {@link Visualizer#updateDotsRadius} was last called.
     */
    private double lastDotTimestamp = 0.0;


    /**
     * Creates a new {@link Visualizer} instance.
     * @param canvas A canvas to draw visualization on
     * @param bandNum The number of bands
     * @param spectrumThreshold The spectrum threshold
     * @param maxMagnitude The maximum allowed magnitude
     */
    public Visualizer(Pane canvas, int bandNum, int spectrumThreshold, float maxMagnitude) {
        this(canvas, canvas.getHeight(), 3, 6, bandNum, spectrumThreshold, maxMagnitude);
    }


    /**
     * Creates a new {@link Visualizer} instance.
     * @param canvas A canvas to draw visualization on
     * @param maxBarHeight The maximum bar height
     * @param minDotRadius The minimum dot radius
     * @param maxDotRadius The maximum dot radius
     * @param bandNum The number of bands
     * @param spectrumThreshold The spectrum threshold
     * @param maxMagnitude The maximum allowed magnitude
     */
    public Visualizer(
            Pane canvas,
            double maxBarHeight,
            double minDotRadius,
            double maxDotRadius,
            int bandNum,
            int spectrumThreshold,
            float maxMagnitude) {
        this.canvas = canvas;
        this.maxBarHeight = maxBarHeight;
        this.minDotRadius = minDotRadius;
        this.maxDotRadius = maxDotRadius;
        this.spectrumThreshold = spectrumThreshold;
        this.maxMagnitude = maxMagnitude;

        magnitudeBuffer = new float[bandNum];
        Arrays.fill(magnitudeBuffer, spectrumThreshold);
    }

    /**
     * Clears internal data to prepare for the next track, but does not reset the visualization itself.
     */
    public void clear() {
        priorMagnitudeAverage = 0.0f;
        lastDotTimestamp = 0.0;
    }

    /**
     * Sets up the visualization.
     */
    public void setUp() {
        if (!canvas.getChildren().isEmpty()) {
            return;
        }
        setUpDots();
        setUpBars();
    }

    /**
     * Fills the {@link Visualizer#bars} array and adds its elements to the {@link Visualizer#canvas}.
     */
    private void setUpBars() {
        final int barCount = magnitudeBuffer.length;
        bars = new Rectangle[barCount];

        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        final double barPadding = 10.0;
        final double bottomPadding = 5.0;
        double barWidth = (canvasWidth - (barCount + 1) * barPadding) / barCount;

        for (int i = 0; i < barCount; i++) {
            Color color = Color.hsb((double) i / barCount * 30, 1, 1);
            Rectangle bar = new Rectangle(barWidth, 0, color);
            bar.setArcHeight(10);
            bar.setArcWidth(20);
            bar.relocate(barPadding + (barWidth + barPadding) * i, canvasHeight - bottomPadding);
            bars[i] = bar;
        }
        canvas.getChildren().addAll(bars);
    }

    /**
     * Fills the {@link Visualizer#dots} array and adds its elements to the {@link Visualizer#canvas}.
     */
    private void setUpDots() {
        final int rowsNumber = 5;
        final int columnsNumber = 13;
        dots = new Circle[rowsNumber][columnsNumber];

        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        final int padding = 20;
        double horizontalMargin =
                (canvasWidth - padding * 2 - minDotRadius * 2 * columnsNumber) / (columnsNumber - 1);
        double verticalMargin =
                (canvasHeight - padding * 2 - minDotRadius * 2 * rowsNumber) / (rowsNumber - 1);
        final Color color = Color.rgb(255, 255, 255, 0.5);

        for (int i = 0; i < rowsNumber; i++) {
            double rowPosition = padding + (minDotRadius * 2 + verticalMargin) * i;
            for (int j = 0; j < columnsNumber; j++) {
                double radius = (maxDotRadius - minDotRadius) / 2 + minDotRadius;
                Circle dot = new Circle(radius, color);
                dot.relocate(padding + (minDotRadius * 2 + horizontalMargin) * j, rowPosition);
                dots[i][j] = dot;
            }
            canvas.getChildren().addAll(dots[i]);
        }
    }

    /**
     * Called when there are new data available for the audio spectrum.
     * @param timestamp Timestamp of the event in seconds
     * @param duration Duration for which the spectrum was computed in seconds.
     * @param magnitudes Array containing the non-positive spectrum magnitude in decibels (dB) for each band.
     *                   The size of the array equals the number of bands and should be considered to be read-only
     * @param phases Array containing the phase in the range [<code>Math.PI</code>,&nbsp;<code>Math.PI</code>] for each
     *               band.
     *               The size of the array equals the number of bands and should be considered to be read-only
     */
    @Override
    public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
        float magnitudeGrowthAverage = 0.0f;
        float magnitudeAverage = 0.0f;

        for (int i = 0; i < magnitudes.length; i++) {
            if (magnitudes[i] >= magnitudeBuffer[i]) {
                magnitudeGrowthAverage += magnitudes[i] - magnitudeBuffer[i];
                magnitudeBuffer[i] = magnitudes[i];
            } else {
                magnitudeBuffer[i] = Math.max(magnitudeBuffer[i] - 3, magnitudes[i]);
            }
            magnitudeAverage += magnitudes[i] - spectrumThreshold;

            updateBarHeight(i);
        }
        magnitudeAverage /= magnitudes.length;
        magnitudeGrowthAverage /= magnitudes.length;

        shiftBarsColor(magnitudeGrowthAverage);
        updateDotsRadius(magnitudeGrowthAverage, timestamp);

        priorMagnitudeAverage = magnitudeAverage;
    }

    /**
     * Updates the height of a bar with the provided {@code index}.
     * @param index The index of the bar in {@link Visualizer#bars}
     */
    private void updateBarHeight(int index) {
        double newHeight = maxBarHeight * Math.min(1, (magnitudeBuffer[index] - spectrumThreshold) / maxMagnitude);
        bars[index].setY(-newHeight);
        bars[index].setHeight(newHeight);
    }

    /**
     * Shifts colors of the {@link Visualizer#bars}.
     * @param magnitudeGrowthAverage The magnitude growth average
     */
    private void shiftBarsColor(double magnitudeGrowthAverage) {
        Color firstBarColor = (Color) bars[0].getFill();
        double shiftPercent = magnitudeGrowthAverage / maxMagnitude;
        double baseColorHue = (firstBarColor.getHue() + (30 * shiftPercent));
        for (int i = 0; i < bars.length; i++) {
            Color color = Color.hsb(baseColorHue + (double) i / bars.length * 30, 1, 1);
            bars[i].setFill(color);
        }
    }

    /**
     * Updates dots' radius.
     * @param magnitudeGrowthAverage The magnitude growth average
     * @param timestamp The current timestamp
     */
    private void updateDotsRadius(double magnitudeGrowthAverage, double timestamp) {
        double percent = magnitudeGrowthAverage / priorMagnitudeAverage;
        if (percent < 0.15 || (timestamp - lastDotTimestamp) < 0.125) {
            return;
        }
        for (int i = 0; i < dots.length; i++) {
            for (int j = 0; j < dots[0].length; j++) {
                double radius = dotsAnimationForward ^ ((i + j) % 2 == 0) ? minDotRadius : maxDotRadius;
                dots[i][j].setRadius(radius);
            }
        }
        dotsAnimationForward = !dotsAnimationForward;
        lastDotTimestamp = timestamp;
    }
}
