package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * A call-out banner for combat moments (Dasher ready, overtime, all clear): a slanted band
 * snaps across the screen in a few drawn-looking steps, a lagging accent shard follows, the
 * words punch in from the right, hold, and the whole thing is thrown off to the right.
 * It only announces a state the backend reported; it never changes anything.
 */
final class CutIn {
    private static final double W = ClassroomApp.WIDTH;
    private static final double BAND_H = 136;
    private static final double SLOPE = 0.35;

    private CutIn() {}

    /** Plays one banner centred at {@code y} on {@code layer}; tone is "caret", "cyan" or "danger". */
    static Animation play(Pane layer, double y, String text, String sub, String tone) {
        Polygon band = band(-120, W + 240, BAND_H);
        band.getStyleClass().addAll("cutin-band", "tone-" + tone);
        Polygon shard = band(-120, W + 240, BAND_H * 0.42);
        shard.getStyleClass().addAll("cutin-shard", "tone-" + tone);
        shard.setTranslateY(BAND_H * 0.62);

        Label word = new Label(text);
        word.getStyleClass().addAll("display", "cutin-word", "tone-" + tone);
        VBox words = new VBox(-6, word);
        if (sub != null) {
            Label subLabel = new Label(sub);
            subLabel.getStyleClass().addAll("display", "cutin-sub", "tone-" + tone);
            words.getChildren().add(subLabel);
        }
        words.setLayoutX(W * 0.30);
        words.setLayoutY(12);
        words.setRotate(-4);

        Group banner = new Group(shard, band, words);
        banner.setLayoutY(y - BAND_H / 2);
        banner.setRotate(-4);
        banner.setMouseTransparent(true);
        layer.getChildren().add(banner);

        Scale bandGrow = new Scale(0, 1, -120, 0);
        band.getTransforms().add(bandGrow);
        Scale shardGrow = new Scale(0, 1, -120, 0);
        shard.getTransforms().add(shardGrow);
        words.setTranslateX(260);
        words.setOpacity(0);

        // In: the band opens in hard steps, the shard lags, the words overshoot and settle.
        Timeline in = new Timeline(
                new KeyFrame(Duration.millis(150), new KeyValue(bandGrow.xProperty(), 1, MotionTokens.steps(5))),
                new KeyFrame(Duration.millis(70), new KeyValue(shardGrow.xProperty(), 0)),
                new KeyFrame(Duration.millis(250), new KeyValue(shardGrow.xProperty(), 1, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(90), new KeyValue(words.translateXProperty(), 260),
                        new KeyValue(words.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(220), new KeyValue(words.translateXProperty(), -18, MotionTokens.OUT),
                        new KeyValue(words.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(320), new KeyValue(words.translateXProperty(), 0, MotionTokens.IN_OUT)));

        // Out: everything is thrown to the right and the words drift left as they fade.
        Timeline out = new Timeline(
                new KeyFrame(Duration.millis(200),
                        new KeyValue(band.translateXProperty(), W + 200, MotionTokens.IN),
                        new KeyValue(shard.translateXProperty(), W + 340, MotionTokens.IN),
                        new KeyValue(words.translateXProperty(), -70, MotionTokens.IN)));
        FadeTransition fade = new FadeTransition(Duration.millis(200), words);
        fade.setToValue(0);

        SequentialTransition all = new SequentialTransition(in, new PauseTransition(Duration.millis(620)),
                new ParallelTransition(out, fade));
        all.setOnFinished(e -> layer.getChildren().remove(banner));
        all.play();
        return all;
    }

    /** A parallelogram from x0 to x1, slanted like the rest of the interface. */
    private static Polygon band(double x0, double x1, double h) {
        double lean = h * SLOPE;
        return new Polygon(x0 + lean, 0, x1 + lean, 0, x1, h, x0, h);
    }
}
