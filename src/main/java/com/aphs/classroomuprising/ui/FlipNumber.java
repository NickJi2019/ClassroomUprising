package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * A split-flap number: on {@link #flipTo} the top half of the old value folds down over a
 * hinge and the bottom half of the new value unfolds from it.
 */
public class FlipNumber extends Pane {
    private double cellWidth = 230;
    private double cellHeight = 220;
    private String text = "";

    public FlipNumber() {
        getStyleClass().add("flip-number");
        applySize();
    }

    public double getCellWidth() { return cellWidth; }

    public void setCellWidth(double value) { cellWidth = value; applySize(); }

    public double getCellHeight() { return cellHeight; }

    public void setCellHeight(double value) { cellHeight = value; applySize(); }

    public void setText(String value) {
        text = value;
        getChildren().setAll(face(value, null), hinge());
    }

    /** Animates from the current text to {@code next}; the text is {@code next} afterwards. */
    public Animation flipTo(String next) {
        double middle = cellHeight / 2;
        Node newTop = face(next, true);
        Node oldBottom = face(text, false);
        Node oldTop = face(text, true);
        Node newBottom = face(next, false);
        Scale fold = new Scale(1, 1, 0, middle);
        oldTop.getTransforms().add(fold);
        oldTop.getStyleClass().add("flip-moving");
        Scale unfold = new Scale(1, 0, 0, middle);
        newBottom.getTransforms().add(unfold);
        newBottom.getStyleClass().add("flip-moving");

        // Light: a flap darkens as it turns away from the viewer, and casts a shadow below the hinge.
        ColorAdjust foldShade = new ColorAdjust();
        foldShade.brightnessProperty().bind(fold.yProperty().subtract(1).multiply(0.45));
        oldTop.setEffect(foldShade);
        ColorAdjust unfoldShade = new ColorAdjust();
        unfoldShade.brightnessProperty().bind(unfold.yProperty().subtract(1).multiply(0.45));
        newBottom.setEffect(unfoldShade);
        Rectangle shadow = new Rectangle(0, middle, cellWidth, middle);
        shadow.getStyleClass().add("flip-shadow");
        shadow.opacityProperty().bind(fold.yProperty().negate().add(1)
                .multiply(unfold.yProperty().negate().add(1)).multiply(0.55));
        getChildren().setAll(newTop, oldBottom, shadow, oldTop, newBottom, hinge());

        Timeline flip = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(fold.yProperty(), 1), new KeyValue(unfold.yProperty(), 0)),
                new KeyFrame(Duration.millis(230), new KeyValue(fold.yProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(unfold.yProperty(), 0)),
                new KeyFrame(Duration.millis(450), new KeyValue(unfold.yProperty(), 1, Interpolator.SPLINE(0.3, 0.6, 0.4, 1.0))),
                // Landing: the flap slaps down and bounces once.
                new KeyFrame(Duration.millis(510), new KeyValue(unfold.yProperty(), 0.92, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(590), new KeyValue(unfold.yProperty(), 1, Interpolator.EASE_IN)));
        flip.setOnFinished(e -> setText(next));
        return flip;
    }

    /** One copy of the digits; {@code top} null shows the whole face, otherwise one half. */
    private Label face(String value, Boolean top) {
        Label digits = new Label(value);
        digits.getStyleClass().addAll("display", "flip-digits");
        digits.setAlignment(Pos.CENTER);
        digits.setMinSize(cellWidth, cellHeight);
        digits.setPrefSize(cellWidth, cellHeight);
        digits.setMaxSize(cellWidth, cellHeight);
        if (top != null) {
            double middle = cellHeight / 2;
            digits.setClip(new Rectangle(0, top ? 0 : middle, cellWidth, middle));
        }
        return digits;
    }

    private Rectangle hinge() {
        Rectangle hinge = new Rectangle(0, cellHeight / 2 - 1.5, cellWidth, 3);
        hinge.getStyleClass().add("flip-hinge");
        return hinge;
    }

    private void applySize() {
        setMinSize(cellWidth, cellHeight);
        setPrefSize(cellWidth, cellHeight);
        setMaxSize(cellWidth, cellHeight);
    }
}
