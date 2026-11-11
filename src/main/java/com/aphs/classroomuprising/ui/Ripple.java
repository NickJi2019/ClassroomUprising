package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * A panel opening out of a point (or closing into one) as a ripple: two flat-coloured
 * silhouettes of the panel (an opaque rectangle) run ahead of it in wide rings, cyan outside,
 * caret inside, and the panel follows them. Closing runs the other way: the panel turns into its coloured silhouettes,
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
     * The live panel is revealed (or hidden) by a growing circle; two flat-coloured discs lie
     * under it, larger than its circle and clipped to the panel's rectangle, so they show as rings
     * ahead of its edge: the panel's two-tone silhouette (the panel is an opaque rectangle). Discs
     * and a rectangle clip are drawn by the graphics card; nothing is snapshot or recoloured, and
     * the panel itself is never swapped for a picture, so nothing jumps when the ripple ends.
     */
    private static Animation run(Region panel, Point2D at, boolean opening, Duration time) {
        Pane parent = (Pane) panel.getParent();
        panel.setVisible(true);
        panel.setClip(null);
        parent.applyCss();
        parent.layout();
        double w = panel.getWidth(), h = panel.getHeight();
        double cx = at.getX() - panel.getLayoutX(), cy = at.getY() - panel.getLayoutY();
        Circle outer = new Circle(cx, cy, 0, OUTER), inner = new Circle(cx, cy, 0, INNER);
        javafx.scene.Group rings = new javafx.scene.Group(outer, inner);
        rings.setClip(new javafx.scene.shape.Rectangle(0, 0, w, h));
        rings.setOpacity(0.92);
        rings.setManaged(false);
        rings.setMouseTransparent(true);
        // Placed by its origin (relocate would place a group by its content's bounds, which start at the impact).
        rings.setLayoutX(panel.getLayoutX());
        rings.setLayoutY(panel.getLayoutY());
        int index = parent.getChildren().indexOf(panel);
        parent.getChildren().add(index, rings);

        double reach = Math.max(Math.max(Math.hypot(cx, cy), Math.hypot(w - cx, cy)),
                Math.max(Math.hypot(cx, h - cy), Math.hypot(w - cx, h - cy))) + 2 * BAND;
        Circle faceClip = new Circle(cx, cy, 0);
        panel.setClip(faceClip);
        DoubleProperty front = new SimpleDoubleProperty();
        front.addListener((obs, was, now) -> {
            double r = now.doubleValue();
            outer.setRadius(Math.max(0, r));
            inner.setRadius(Math.max(0, r - BAND));
            faceClip.setRadius(Math.max(0, r - 2 * BAND));
        });
        front.set(opening ? 0 : reach);
        Timeline wave = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(front, opening ? 0 : reach)),
                new KeyFrame(time, new KeyValue(front, opening ? reach : 0, opening ? MotionTokens.OUT : Interpolator.EASE_IN)));
        // Cleaned up when it stops, through a status listener: callers may set their own onFinished.
        wave.statusProperty().addListener((obs, was, now) -> {
            if (now != Animation.Status.STOPPED) return;
            parent.getChildren().remove(rings);
            panel.setClip(null);
            if (!opening) panel.setVisible(false);
        });
        return wave;
    }
}
