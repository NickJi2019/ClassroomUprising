package com.aphs.classroomuprising.ui.scene;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Background art, scaled to cover the canvas with a margin, slowly drifting (so the scene is
 * never a still), and colour-graded by a {@link Grade}. A new image crossfades over the old.
 * The drift amplitude is a parameter so a foreground plane can move further (parallax).
 */
final class BackgroundLayer extends Pane {
    static final double W = 1280, H = 720;
    private final double margin;
    private final double driftScale;
    private Group current;
    private ColorAdjust adjust;
    private Rectangle tint;
    private final Timeline drift;

    BackgroundLayer(double margin, double driftScale) {
        this.margin = margin;
        this.driftScale = driftScale;
        setPrefSize(W, H);
        setMinSize(W, H);
        setMaxSize(W, H);
        setMouseTransparent(true);
        double dx = 14 * driftScale, dy = 8 * driftScale;
        drift = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(translateXProperty(), -dx), new KeyValue(translateYProperty(), dy),
                        new KeyValue(scaleXProperty(), 1.0), new KeyValue(scaleYProperty(), 1.0)),
                new KeyFrame(Duration.seconds(26), new KeyValue(translateXProperty(), dx, Interpolator.EASE_BOTH),
                        new KeyValue(translateYProperty(), -dy, Interpolator.EASE_BOTH),
                        new KeyValue(scaleXProperty(), 1 + 0.02 * driftScale, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1 + 0.02 * driftScale, Interpolator.EASE_BOTH)));
        drift.setAutoReverse(true);
        drift.setCycleCount(Animation.INDEFINITE);
    }

    Animation drift() { return drift; }

    /** Shows an image with a grade; crossfades when something is already showing. */
    void show(Image image, Grade grade, boolean crossfade) {
        ImageView view = new ImageView(image);
        double scale = Math.max((W + 2 * margin) / image.getWidth(), (H + 2 * margin) / image.getHeight());
        view.setFitWidth(image.getWidth() * scale);
        view.setFitHeight(image.getHeight() * scale);
        view.setSmooth(true);
        view.relocate((W - view.getFitWidth()) / 2, (H - view.getFitHeight()) / 2);
        ColorAdjust colour = new ColorAdjust();
        view.setEffect(colour);
        Rectangle layerTint = new Rectangle(-margin, -margin, W + 2 * margin, H + 2 * margin);
        Group group = new Group(view, layerTint);
        apply(colour, layerTint, grade);

        Group previous = current;
        current = group;
        adjust = colour;
        tint = layerTint;
        getChildren().add(group);
        if (previous != null && crossfade) {
            group.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(420), group);
            fade.setToValue(1);
            fade.setOnFinished(e -> getChildren().remove(previous));
            fade.play();
        } else if (previous != null) {
            getChildren().remove(previous);
        }
    }

    /** Re-grades the current image, animated over {@code duration} (zero applies at once). */
    Animation regrade(Grade grade, Duration duration) {
        tint.setBlendMode(grade.mode());
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            // A timeline whose only key frame is at time zero never applies it, so set the values directly.
            apply(adjust, tint, grade);
            return new Timeline();
        }
        return new Timeline(new KeyFrame(duration,
                new KeyValue(tint.fillProperty(), grade.tint(), Interpolator.EASE_BOTH),
                new KeyValue(tint.opacityProperty(), grade.tintOpacity(), Interpolator.EASE_BOTH),
                new KeyValue(adjust.saturationProperty(), grade.saturation(), Interpolator.EASE_BOTH),
                new KeyValue(adjust.brightnessProperty(), grade.brightness(), Interpolator.EASE_BOTH)));
    }

    private static void apply(ColorAdjust colour, Rectangle layerTint, Grade grade) {
        colour.setSaturation(grade.saturation());
        colour.setBrightness(grade.brightness());
        layerTint.setFill(grade.tint());
        layerTint.setOpacity(grade.tintOpacity());
        layerTint.setBlendMode(grade.mode());
    }
}
