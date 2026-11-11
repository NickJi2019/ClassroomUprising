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
 * cracks into shards around the point of impact, flashes, shakes, then the shards fall away,
 * inner ones first, revealing what lies behind them (the full-screen defeat card). Presentation
 * only.
 */
final class Shatter {
    private static final double W = ClassroomApp.WIDTH, H = ClassroomApp.HEIGHT;
    private static final int RAYS = 13;
    /** Ring radii around the impact; the last reaches past every corner. */
    private static final double[] RINGS = {0, 85, 230, 450, 1700};

    private Shatter() {}

    /**
     * @param layer   a full-canvas pane on top of everything (what lies behind and the shards go here)
     * @param screen  what breaks (the screen's root)
     * @param at      the point of impact
     * @param behind  what the falling shards reveal (full-canvas); it stays when they are gone
     * @param fallen  runs when the shards are gone
     * @return the crack and the fall
     */
    static Animation play(Pane layer, Node screen, Point2D at, Node behind, Runnable fallen) {
        SnapshotParameters area = new SnapshotParameters();
        area.setViewport(new Rectangle2D(0, 0, W, H));
        Image picture = screen.snapshot(area, null);
        Random random = new Random(11);

        // While it cracks the screen is still one piece: its picture with the crack lines on it
        // (cheap to draw). The shards replace it when they start to fall.
        javafx.scene.image.ImageView whole = new javafx.scene.image.ImageView(picture);
        Group lines = new Group();
        Group cracked = new Group(whole, lines);
        cracked.setCache(true);
        cracked.setCacheHint(javafx.scene.CacheHint.SPEED);
        Group shards = new Group();
        shards.setVisible(false);
        Rectangle flash = new Rectangle(W, H, Color.WHITE);
        flash.setOpacity(0);
        layer.getChildren().addAll(behind, shards, cracked, flash);

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
                // The piece of glass, trimmed to the screen (the outer ring reaches far past it).
                List<Point2D> piece = onScreen(List.of(grid[k][i], grid[k][j], grid[k + 1][j], grid[k + 1][i]));
                if (piece.size() < 3) continue;
                Polygon shard = new Polygon();
                for (Point2D p : piece) shard.getPoints().addAll(p.getX(), p.getY());
                shard.setFill(glass);
                shard.setStroke(Color.rgb(255, 255, 255, 0.85));
                shard.setStrokeWidth(1.4);
                shards.getChildren().add(shard);
                Polygon edge = new Polygon();
                edge.getPoints().setAll(shard.getPoints());
                edge.setFill(null);
                edge.setStroke(Color.rgb(255, 255, 255, 0.85));
                edge.setStrokeWidth(1.4);
                lines.getChildren().add(edge);
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
            jolt.getKeyFrames().add(new KeyFrame(Duration.millis(40 * (i + 1)), new KeyValue(cracked.translateXProperty(), xs[i])));
        ParallelTransition breaking = new ParallelTransition(crack, jolt);
        PauseTransition hold = new PauseTransition(Duration.millis(260));
        // The pieces come apart: the picture gives way to the shards, which fall.
        hold.setOnFinished(e -> {
            layer.getChildren().remove(cracked);
            shards.setVisible(true);
        });
        ParallelTransition apart = new ParallelTransition();
        apart.getChildren().addAll(falls);
        apart.setOnFinished(e -> {
            layer.getChildren().removeAll(shards, flash);
            fallen.run();
        });
        return new SequentialTransition(breaking, hold, apart);
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
        // Drawn once into a bitmap as it starts to move (so the bitmaps are made over a few
        // frames, not all in one); falling and turning then just move it.
        fall.getKeyFrames().add(new KeyFrame(Duration.ZERO, e -> {
            shard.setCacheHint(javafx.scene.CacheHint.SPEED);
            shard.setCache(true);
        }));
        return fall;
    }

    /** A polygon trimmed to the screen (Sutherland-Hodgman against its four edges). */
    static List<Point2D> onScreen(List<Point2D> polygon) {
        List<Point2D> out = polygon;
        double[][] edges = {{0, 0}, {1, W}, {2, 0}, {3, H}};   // x >= 0, x <= W, y >= 0, y <= H
        for (double[] edge : edges) {
            int side = (int) edge[0];
            double limit = edge[1];
            List<Point2D> in = out;
            out = new ArrayList<>();
            for (int n = 0; n < in.size(); n++) {
                Point2D a = in.get(n), b = in.get((n + 1) % in.size());
                boolean aIn = inside(a, side, limit), bIn = inside(b, side, limit);
                if (aIn) out.add(a);
                if (aIn != bIn) out.add(cross(a, b, side, limit));
            }
            if (out.isEmpty()) return out;
        }
        return out;
    }

    private static boolean inside(Point2D p, int side, double limit) {
        return switch (side) {
            case 0 -> p.getX() >= limit;
            case 1 -> p.getX() <= limit;
            case 2 -> p.getY() >= limit;
            default -> p.getY() <= limit;
        };
    }

    private static Point2D cross(Point2D a, Point2D b, int side, double limit) {
        boolean vertical = side <= 1;
        double t = vertical ? (limit - a.getX()) / (b.getX() - a.getX()) : (limit - a.getY()) / (b.getY() - a.getY());
        return a.add(b.subtract(a).multiply(t));
    }
}
