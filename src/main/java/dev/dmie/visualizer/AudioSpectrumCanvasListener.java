package dev.dmie.visualizer;

import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.Arrays;

public class AudioSpectrumCanvasListener implements AudioSpectrumListener {
    private final Rectangle[] bars;
    private final double maxBarHeight;

    private final Circle[][] dots;
    private final double minDotRadius;
    private final double maxDotRadius;
    private boolean dotsAnimationForward = true;

    private final int spectrumThreshold;
    private final float maxMagnitude;

    private final float[] magnitudeBuffer;
    private float priorMagnitudeAverage = 0.0f;

    private double lastDotTimestamp = 0.0;


    public AudioSpectrumCanvasListener(
            Rectangle[] bars,
            double maxBarHeight,
            Circle[][] dots,
            double minDotRadius,
            double maxDotRadius,
            int spectrumThreshold,
            float maxMagnitude) {
        this.bars = bars;
        this.maxBarHeight = maxBarHeight;
        this.dots = dots;
        this.minDotRadius = minDotRadius;
        this.maxDotRadius = maxDotRadius;
        this.spectrumThreshold = spectrumThreshold;
        this.maxMagnitude = maxMagnitude;

        magnitudeBuffer = new float[bars.length];
        Arrays.fill(magnitudeBuffer, spectrumThreshold);
    }

    public void clear() {
        priorMagnitudeAverage = 0.0f;
        lastDotTimestamp = 0.0;
    }

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

    private void updateBarHeight(int index) {
        double newHeight = maxBarHeight * Math.min(1, (magnitudeBuffer[index] - spectrumThreshold) / maxMagnitude);
        bars[index].setY(-newHeight);
        bars[index].setHeight(newHeight);
    }

    private void shiftBarsColor(double magnitudeGrowthAverage) {
        Color firstBarColor = (Color) bars[0].getFill();
        double shiftPercent = magnitudeGrowthAverage / maxMagnitude;
        double baseColorHue = (firstBarColor.getHue() + (30 * shiftPercent));
        for (int i = 0; i < bars.length; i++) {
            Color color = Color.hsb(baseColorHue + (double) i / bars.length * 30, 1, 1);
            bars[i].setFill(color);
        }
    }

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
