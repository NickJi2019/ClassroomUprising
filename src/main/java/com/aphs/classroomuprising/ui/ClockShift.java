package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Time-of-day transition: a dial whose hands sweep forward from the old period's time to the
 * new one while the digital time counts up and the new period name slides in. The screen
 * behind is swapped while the overlay fully covers it.
 */
final class ClockShift extends Pane {
    private static final double CX = 420, CY = 360, RADIUS = 160;
    private static final Duration FADE_IN = Duration.millis(170);
    private static final Duration SWEEP_END = Duration.millis(1050);
    private static final Duration SWAP = Duration.millis(1080);
    private static final Duration HOLD_END = Duration.millis(1300);
    private static final Duration FADE_OUT_END = Duration.millis(1520);

    private final Timeline animation;

    ClockShift(DayPeriod from, DayPeriod to, Runnable swapScreen) {
        setPrefSize(ClassroomApp.WIDTH, ClassroomApp.HEIGHT);
        setMouseTransparent(true);
        getStyleClass().add("clock-shift");

        Rectangle dim = new Rectangle(ClassroomApp.WIDTH, ClassroomApp.HEIGHT);
        dim.getStyleClass().add("clock-dim");
        Polygon band = new Polygon(0, 196, 1280, 196, 1280, 524, 82, 524);
        band.getStyleClass().add("clock-band");
        Line edge = new Line(0, 196, 1280, 196);
        edge.getStyleClass().add("clock-band-edge");
        getChildren().addAll(dim, band, edge);

        Circle face = new Circle(CX, CY, RADIUS);
        face.getStyleClass().add("clock-face");
        getChildren().add(face);
        for (int hour = 0; hour < 12; hour++) {
            boolean major = hour % 3 == 0;
            double length = major ? 26 : 13;
            Polygon tick = new Polygon(CX - 3, CY - RADIUS + 10, CX + 5, CY - RADIUS + 10,
                    CX + 3, CY - RADIUS + 10 + length, CX - 5, CY - RADIUS + 10 + length);
            tick.getStyleClass().add(major ? "clock-tick-major" : "clock-tick");
            tick.getTransforms().add(new Rotate(hour * 30, CX, CY));
            getChildren().add(tick);
        }

        Polygon minuteHand = new Polygon(CX - 2, CY + 14, CX + 2, CY + 14, CX + 3, CY - RADIUS + 26, CX - 1, CY - RADIUS + 26);
        minuteHand.getStyleClass().add("clock-hand-minute");
        Rotate minuteTurn = new Rotate(0, CX, CY);
        minuteHand.getTransforms().add(minuteTurn);
        Polygon hourHand = new Polygon(CX - 7, CY + 12, CX + 7, CY + 12, CX + 10, CY - RADIUS * 0.55, CX - 4, CY - RADIUS * 0.55);
        hourHand.getStyleClass().add("clock-hand-hour");
        Rotate hourTurn = new Rotate(0, CX, CY);
        hourHand.getTransforms().add(hourTurn);
        Circle hub = new Circle(CX, CY, 11);
        hub.getStyleClass().add("clock-hub");
        getChildren().addAll(minuteHand, hourHand, hub);

        Label fromName = new Label(ClockTimes.caption(from));
        fromName.getStyleClass().addAll("display", "clock-from");
        fromName.relocate(660, 236);
        Label toName = new Label(ClockTimes.caption(to));
        toName.getStyleClass().addAll("display", "clock-to");
        toName.relocate(652, 268);
        toName.setOpacity(0);
        toName.setTranslateX(60);
        Label digital = new Label();
        digital.getStyleClass().addAll("mono", "clock-time");
        digital.relocate(664, 404);
        getChildren().addAll(fromName, toName, digital);

        int start = ClockTimes.minutes(from, from);
        int end = start + ClockTimes.elapsed(start, ClockTimes.minutes(to, from));
        DoubleProperty minutes = new SimpleDoubleProperty();
        minutes.addListener((obs, old, now) -> {
            double m = now.doubleValue();
            minuteTurn.setAngle((m % 60) * 6);
            hourTurn.setAngle(((m / 60) % 12) * 30);
            digital.setText(ClockTimes.format(m));
        });
        minutes.set(start);
        hourTurn.setAngle(((start / 60.0) % 12) * 30);
        minuteTurn.setAngle((start % 60) * 6);
        digital.setText(ClockTimes.format(start));

        setOpacity(0);
        Interpolator sweep = Interpolator.SPLINE(0.55, 0, 0.25, 1);
        Interpolator slide = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);
        animation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0), new KeyValue(minutes, start)),
                new KeyFrame(FADE_IN, new KeyValue(opacityProperty(), 1), new KeyValue(minutes, start)),
                new KeyFrame(Duration.millis(560), new KeyValue(fromName.opacityProperty(), 1),
                        new KeyValue(toName.opacityProperty(), 0), new KeyValue(toName.translateXProperty(), 60)),
                new KeyFrame(Duration.millis(820), new KeyValue(fromName.opacityProperty(), 0.35),
                        new KeyValue(toName.opacityProperty(), 1, slide), new KeyValue(toName.translateXProperty(), 0, slide)),
                new KeyFrame(SWEEP_END, new KeyValue(minutes, end, sweep)),
                new KeyFrame(SWAP, e -> swapScreen.run()),
                new KeyFrame(HOLD_END, new KeyValue(opacityProperty(), 1)),
                new KeyFrame(FADE_OUT_END, new KeyValue(opacityProperty(), 0, Interpolator.EASE_IN)));
    }

    Animation animation() { return animation; }
}
