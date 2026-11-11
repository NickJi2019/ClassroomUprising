package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The screen breaking like glass when the player is defeated: a picture of the whole screen
 * cracks into shards around the point of impact, flashes, shakes, then the shards fall away into
 * darkness, inner ones first. Presentation only.
 */
final class Shatter {
    private static final double W = ClassroomApp.WIDTH, H = ClassroomApp.HEIGHT;
    private static final int RAYS = 13;
    /** Ring radii around the impact; the last reaches past every corner. */
    private static final double[] RINGS = {0, 85, 230, 450, 1700};

    private Shatter() {}

    /**
     * @param layer   a full-canvas pane on top of everything (the shards and the dark go here)
     * @param screen  what breaks (the screen's root)
     * @param at      the point of impact
     * @param fallen  runs when the shards are gone, while the screen is still dark behind them
     * @return the crack and the fall; when it finishes the dark fades and is removed
     */
    static Animation play(Pane layer, Node screen, Point2D at, Runnable fallen) {
        SnapshotParameters area = new SnapshotParameters();
        area.setViewport(new Rectangle2D(0, 0, W, H));
        Image picture = screen.snapshot(area, null);
        Random random = new Random(11);

        Rectangle dark = new Rectangle(W, H, Color.web("#05060F"));
        Group shards = new Group();
        Rectangle flash = new Rectangle(W, H, Color.WHITE);
        flash.setOpacity(0);
        layer.getChildren().addAll(dark, shards, flash);

        // The crack pattern: rays out of the impact, crossed by jagged rings.
        double[] angles = new double[RAYS];
        for (int i = 0; i < RAYS; i++) angles[i] = (i + random.nextDouble() * 0.7) * 2 * Math.PI / RAYS;
        Point2D[][] grid = new Point2D[RINGS.length][RAYS];
        for (int k = 0; k < RINGS.length; k++)
            for (int i = 0; i < RAYS; i++) {
                double r = RINGS[k] * (k == 0 || k == RINGS.length - 1 ? 1 : 0.75 + random.nextDouble() * 0.5);
                grid[k][i] = at.add(Math.cos(angles[i]) * r, Math.sin(angles[i]) * r);
            }
        ImagePattern glass = new ImagePattern(picture, 0, 0, W, H, false);
        List<Animation> falls = new ArrayList<>();
        for (int k = 0; k < RINGS.length - 1; k++)
            for (int i = 0; i < RAYS; i++) {
                int j = (i + 1) % RAYS;
                Polygon shard = new Polygon();
                for (Point2D p : List.of(grid[k][i], grid[k][j], grid[k + 1][j], grid[k + 1][i]))
                    shard.getPoints().addAll(p.getX(), p.getY());
                shard.setFill(glass);
                shard.setStroke(Color.rgb(255, 255, 255, 0.85));
                shard.setStrokeWidth(1.4);
                shards.getChildren().add(shard);
                falls.add(fall(shard, at, k, random));
            }

        Sfx.SHATTER.play(layer);
        // The crack: a white flash and a jolt, the pieces still in place.
        Timeline crack = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(flash.opacityProperty(), 0.7)),
                new KeyFrame(Duration.millis(220), new KeyValue(flash.opacityProperty(), 0, Interpolator.EASE_OUT)));
        Timeline jolt = new Timeline();
        double[] xs = {-10, 8, -6, 4, -2, 0};
        for (int i = 0; i < xs.length; i++)
            jolt.getKeyFrames().add(new KeyFrame(Duration.millis(40 * (i + 1)), new KeyValue(shards.translateXProperty(), xs[i])));
        ParallelTransition cracked = new ParallelTransition(crack, jolt);
        ParallelTransition apart = new ParallelTransition();
        apart.getChildren().addAll(falls);
        apart.setOnFinished(e -> {
            layer.getChildren().removeAll(shards, flash);
            fallen.run();
        });
        Timeline lift = new Timeline(new KeyFrame(Duration.millis(520), new KeyValue(dark.opacityProperty(), 0, Interpolator.EASE_IN)));
        lift.setOnFinished(e -> layer.getChildren().remove(dark));
        return new SequentialTransition(cracked, new PauseTransition(Duration.millis(260)), apart, lift);
    }

    /** One shard falls: away from the impact, then down, turning and fading; inner ones go first. */
    private static Animation fall(Polygon shard, Point2D at, int ring, Random random) {
        double cx = 0, cy = 0;
        int n = shard.getPoints().size() / 2;
        for (int p = 0; p < n; p++) {
            cx += shard.getPoints().get(2 * p);
            cy += shard.getPoints().get(2 * p + 1);
        }
        Point2D centre = new Point2D(cx / n, cy / n);
        Point2D away = centre.subtract(at);
        away = away.magnitude() < 1 ? new Point2D(0, 1) : away.normalize();
        double push = 50 + random.nextDouble() * 120;
        double drop = 760 + random.nextDouble() * 360;
        double time = 950 + random.nextDouble() * 450;
        double spin = (random.nextBoolean() ? 1 : -1) * (25 + random.nextDouble() * 110);
        Interpolator gravity = Interpolator.SPLINE(0.5, 0, 0.9, 0.7);
        Timeline fall = new Timeline(
                new KeyFrame(Duration.millis(time),
                        new KeyValue(shard.translateXProperty(), away.getX() * push, Interpolator.EASE_OUT),
                        new KeyValue(shard.translateYProperty(), away.getY() * push + drop, gravity),
                        new KeyValue(shard.rotateProperty(), spin, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(time * 0.6), new KeyValue(shard.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(time), new KeyValue(shard.opacityProperty(), 0)));
        fall.setDelay(Duration.millis(ring * 70 + random.nextDouble() * 90));
        return fall;
    }
}
