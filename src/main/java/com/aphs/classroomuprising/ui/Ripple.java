package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * A panel opening out of a point (or closing into one) as a ripple: two flat-coloured
 * silhouettes of the panel run ahead of it in wide rings, cyan outside, caret inside, and the
 * panel follows them. Closing runs the other way: the panel turns into its coloured silhouettes,
 * which shrink into the point.
 */
final class Ripple {
    /** How wide each coloured ring is. */
    private static final double BAND = 110;
    private static final Color OUTER = Color.web("#5CE1E6"), INNER = Color.web("#FFD23F");

    private Ripple() {}

    /** Opens {@code panel} from {@code at} (in the coordinates of the panel's parent). */
    static Animation open(Region panel, Point2D at) {
        return run(panel, at, true, Duration.millis(760));
    }

    /** Closes {@code panel} into {@code at}, quicker than it opened; the panel is hidden afterwards. */
    static Animation close(Region panel, Point2D at) {
        return run(panel, at, false, Duration.millis(360));
    }

    /**
     * The live panel is revealed (or hidden) by a growing circle; two pre-coloured copies of its
     * shape lie under it, clipped by larger circles, so they show as rings ahead of its edge.
     * The colours are made once per ripple (no per-frame effects), and the panel itself is never
     * swapped for a picture, so nothing jumps when the ripple ends.
     */
    private static Animation run(Region panel, Point2D at, boolean opening, Duration time) {
        Pane parent = (Pane) panel.getParent();
        panel.setVisible(true);
        panel.setClip(null);
        parent.applyCss();
        parent.layout();
        SnapshotParameters transparent = new SnapshotParameters();
        transparent.setFill(Color.TRANSPARENT);
        Image picture = panel.snapshot(transparent, null);
        ImageView outer = still(tint(picture, OUTER), panel);
        ImageView inner = still(tint(picture, INNER), panel);
        int index = parent.getChildren().indexOf(panel);
        parent.getChildren().addAll(index, java.util.List.of(outer, inner));

        double cx = at.getX() - panel.getLayoutX(), cy = at.getY() - panel.getLayoutY();
        double w = panel.getWidth(), h = panel.getHeight();
        double reach = Math.max(Math.max(Math.hypot(cx, cy), Math.hypot(w - cx, cy)),
                Math.max(Math.hypot(cx, h - cy), Math.hypot(w - cx, h - cy))) + 2 * BAND;
        Circle faceClip = new Circle(cx, cy, 0), innerClip = new Circle(cx, cy, 0), outerClip = new Circle(cx, cy, 0);
        panel.setClip(faceClip);
        inner.setClip(innerClip);
        outer.setClip(outerClip);
        DoubleProperty front = new SimpleDoubleProperty();
        front.addListener((obs, was, now) -> {
            double r = now.doubleValue();
            outerClip.setRadius(Math.max(0, r));
            innerClip.setRadius(Math.max(0, r - BAND));
            faceClip.setRadius(Math.max(0, r - 2 * BAND));
        });
        front.set(opening ? 0 : reach);
        Timeline wave = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(front, opening ? 0 : reach)),
                new KeyFrame(time, new KeyValue(front, opening ? reach : 0, opening ? MotionTokens.OUT : Interpolator.EASE_IN)));
        // Cleaned up when it stops, through a status listener: callers may set their own onFinished.
        wave.statusProperty().addListener((obs, was, now) -> {
            if (now != Animation.Status.STOPPED) return;
            parent.getChildren().removeAll(outer, inner);
            panel.setClip(null);
            if (!opening) panel.setVisible(false);
        });
        return wave;
    }

    /** The picture's shape in one flat colour (its transparency kept), made once per ripple. */
    static Image tint(Image picture, Color color) {
        int w = (int) picture.getWidth(), h = (int) picture.getHeight();
        int[] pixels = new int[w * h];
        javafx.scene.image.WritablePixelFormat<java.nio.IntBuffer> argb = javafx.scene.image.PixelFormat.getIntArgbInstance();
        picture.getPixelReader().getPixels(0, 0, w, h, argb, pixels, 0, w);
        int rgb = ((int) Math.round(color.getRed() * 255) << 16) | ((int) Math.round(color.getGreen() * 255) << 8)
                | (int) Math.round(color.getBlue() * 255);
        for (int i = 0; i < pixels.length; i++) pixels[i] = (pixels[i] & 0xFF000000) | rgb;
        javafx.scene.image.WritableImage out = new javafx.scene.image.WritableImage(w, h);
        out.getPixelWriter().setPixels(0, 0, w, h, argb, pixels, 0, w);
        return out;
    }

    private static ImageView still(Image picture, Region panel) {
        ImageView view = new ImageView(picture);
        view.setManaged(false);
        view.setMouseTransparent(true);
        view.relocate(panel.getLayoutX(), panel.getLayoutY());
        return view;
    }
}
