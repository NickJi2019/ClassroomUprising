package com.aphs.classroomuprising.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/**
 * SAN as a slanted bar in ten segments, with no number: the fill is the share of a full bar
 * ({@link Stats#sanFraction}). Changes slide to the new length. Below a quarter it turns red,
 * as a warning to the eye only. When SAN is very low ({@link #setDistressed}) the bar twitches:
 * it jitters, flickers and now and then jerks sideways, at uneven moments.
 */
final class SanBar extends Pane {
    private static final PseudoClass LOW = PseudoClass.getPseudoClass("low");
    private static final double SKEW = 7;

    private final double width;
    private final double height;
    private final Polygon fill = new Polygon();
    private final DoubleProperty fraction = new SimpleDoubleProperty(0);
    private Timeline moving;
    private boolean placed;
    private double target;
    private final java.util.Random nerves = new java.util.Random();
    private long nextTwitch;
    private final javafx.animation.AnimationTimer twitch = new javafx.animation.AnimationTimer() {
        @Override
        public void handle(long now) { twitch(now); }
    };
    private boolean distressed;

    SanBar(double width, double height) {
        this.width = width;
        this.height = height;
        getStyleClass().add("san-bar");
        Polygon track = new Polygon();
        track.getStyleClass().add("san-track");
        track.getPoints().setAll(points(1));
        fill.getStyleClass().add("san-fill");
        getChildren().addAll(track, fill);
        for (int i = 1; i < 10; i++) {
            double x = (width - SKEW) * i / 10;
            Line tick = new Line(x + SKEW, 0, x, height);
            tick.getStyleClass().add("san-tick");
            getChildren().add(tick);
        }
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        fraction.addListener((obs, was, now) -> draw(now.doubleValue()));
        draw(0);
    }

    /** Shows this share of a full bar (0 to 1); the first value appears at once, later ones slide. */
    void set(double share) {
        target = Math.max(0, Math.min(1, share));
        setAccessibleText("SAN " + Math.round(target * 100) + "%");
        if (moving != null) moving.stop();
        if (!placed) {
            placed = true;
            fraction.set(target);
            return;
        }
        moving = new Timeline(new KeyFrame(Duration.millis(420), new KeyValue(fraction, target, MotionTokens.OUT)));
        moving.play();
    }

    /** Very low SAN: the bar twitches until it is called with false. */
    void setDistressed(boolean on) {
        if (on == distressed) return;
        distressed = on;
        if (on) {
            nextTwitch = 0;
            twitch.start();
        } else {
            twitch.stop();
            setTranslateX(0);
            setTranslateY(0);
            setOpacity(1);
            setSkewless();
        }
    }

    boolean distressed() { return distressed; }

    private void twitch(long now) {
        if (now < nextTwitch) return;
        boolean jerk = nerves.nextInt(9) == 0;   // now and then a bigger spasm
        double reach = jerk ? 7 : 1.6;
        setTranslateX((nerves.nextDouble() * 2 - 1) * reach);
        setTranslateY((nerves.nextDouble() * 2 - 1) * (jerk ? 3 : 1));
        setOpacity(nerves.nextInt(5) == 0 ? 0.35 + nerves.nextDouble() * 0.3 : 1);
        getTransforms().setAll(new javafx.scene.transform.Shear(jerk ? (nerves.nextDouble() - 0.5) * 0.5 : 0, 0));
        nextTwitch = now + (jerk ? 90 : 40 + nerves.nextInt(110)) * 1_000_000L;
    }

    private void setSkewless() { getTransforms().clear(); }

    /** The share shown once any slide has finished. */
    double share() { return target; }

    private void draw(double share) {
        fill.getPoints().setAll(points(share));
        fill.setVisible(share > 0);
        pseudoClassStateChanged(LOW, share < 0.25);
    }

    private Double[] points(double share) {
        double w = (width - SKEW) * share;
        return new Double[] {SKEW, 0.0, SKEW + w, 0.0, w, height, 0.0, height};
    }
}
