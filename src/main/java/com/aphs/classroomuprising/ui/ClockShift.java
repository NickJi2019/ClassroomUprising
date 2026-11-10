package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.ui.scene.AssetStore;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Time-of-day transition. A translucent overlay with a dial: the hands sweep forward (the
 * minute hand leaving a smear when fast), then tick through the last three minutes; a 24-hour
 * ring shows the day's periods with the destination lit and a marker travelling along it; the
 * scene behind is re-graded to the new time of day while this happens. The overlay closes for
 * an instant to swap screens, then fades away.
 *
 * <p>The face and hands use artwork from the asset manifest when delivered
 * ({@code ui.clock-face}, {@code ui.clock-hour-hand}, {@code ui.clock-minute-hand}); otherwise
 * they are drawn here.</p>
 */
final class ClockShift extends Pane {
    private static final double CX = 420, CY = 360, RADIUS = 160, RING = RADIUS + 30;
    private static final double FADE_IN = 170;
    private static final double SWEEP_MS = 720;
    private static final double TICK_MS = 110;

    private final ParallelTransition animation;

    ClockShift(DayPeriod from, DayPeriod to, Runnable swapScreen, AssetStore assets, SceneView world) {
        setPrefSize(ClassroomApp.WIDTH, ClassroomApp.HEIGHT);
        setMouseTransparent(true);
        getStyleClass().add("clock-shift");

        Rectangle dim = new Rectangle(ClassroomApp.WIDTH, ClassroomApp.HEIGHT);
        dim.getStyleClass().add("clock-dim");
        Polygon band = new Polygon(0, 150, 1280, 150, 1280, 570, 104, 570);
        band.getStyleClass().add("clock-band");
        Line edge = new Line(0, 150, 1280, 150);
        edge.getStyleClass().add("clock-band-edge");
        getChildren().addAll(dim, band, edge);

        // 24-hour schedule ring: one arc per period, the destination lit.
        Circle track = new Circle(CX, CY, RING);
        track.getStyleClass().add("clock-ring-track");
        getChildren().add(track);
        for (ClockTimes.Segment segment : ClockTimes.daySegments()) {
            double a = ClockTimes.dayRingAngle(segment.startMinutes());
            double sweep = segment.length() / (double) ClockTimes.DAY_MINUTES * 360;
            Arc arc = new Arc(CX, CY, RING, RING, 90 - a - sweep, sweep - 1.2);
            arc.setType(ArcType.OPEN);
            arc.getStyleClass().addAll("clock-ring", "ring-" + segment.period().name().toLowerCase(Locale.ROOT));
            if (segment.period() == to) arc.getStyleClass().add("ring-target");
            getChildren().add(arc);
        }
        Circle marker = new Circle(7);
        marker.getStyleClass().add("clock-ring-marker");

        // Face, smear, hands.
        Optional<Image> faceArt = assets == null ? Optional.empty() : assets.ui("clock-face");
        Optional<Image> hourArt = assets == null ? Optional.empty() : assets.ui("clock-hour-hand");
        Optional<Image> minuteArt = assets == null ? Optional.empty() : assets.ui("clock-minute-hand");
        getChildren().add(faceArt.<Node>map(ClockShift::art).orElseGet(ClockShift::drawnFace));

        List<Rotate> smearTurns = new ArrayList<>();
        List<Node> smear = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Node ghost = minuteArt.<Node>map(ClockShift::art).orElseGet(ClockShift::drawnMinuteHand);
            ghost.getStyleClass().add("clock-hand-smear");
            Rotate turn = new Rotate(0, CX, CY);
            ghost.getTransforms().add(turn);
            ghost.setOpacity(0);
            smearTurns.add(turn);
            smear.add(ghost);
        }
        getChildren().addAll(smear);
        Node minuteHand = minuteArt.<Node>map(ClockShift::art).orElseGet(ClockShift::drawnMinuteHand);
        Rotate minuteTurn = new Rotate(0, CX, CY);
        minuteHand.getTransforms().add(minuteTurn);
        Node hourHand = hourArt.<Node>map(ClockShift::art).orElseGet(ClockShift::drawnHourHand);
        Rotate hourTurn = new Rotate(0, CX, CY);
        hourHand.getTransforms().add(hourTurn);
        Circle hub = new Circle(CX, CY, 11);
        hub.getStyleClass().add("clock-hub");
        getChildren().addAll(minuteHand, hourHand, hub, marker);

        Label fromName = new Label(ClockTimes.caption(from));
        fromName.getStyleClass().addAll("display", "clock-from");
        fromName.relocate(660, 222);
        Label toName = new Label(ClockTimes.caption(to));
        toName.getStyleClass().addAll("display", "clock-to");
        toName.relocate(652, 254);
        toName.setOpacity(0);
        toName.setTranslateX(60);
        Label digital = new Label();
        digital.getStyleClass().addAll("mono", "clock-time");
        digital.relocate(664, 392);
        Label periodLine = new Label();
        periodLine.getStyleClass().addAll("mono", "clock-period-line");
        periodLine.relocate(668, 452);
        periodLine.setText(UiText.period(from) + "  →  " + ClockTimes.caption(to));
        getChildren().addAll(fromName, toName, digital, periodLine);

        // Time drives everything on the dial.
        int start = ClockTimes.minutes(from, from);
        int end = start + ClockTimes.elapsed(start, ClockTimes.minutes(to, from));
        ClockTimes.TickPlan plan = ClockTimes.tickPlan(start, end);
        DoubleProperty minutes = new SimpleDoubleProperty(start);
        DoubleProperty jitter = new SimpleDoubleProperty(0);
        Runnable draw = () -> {
            double m = minutes.get();
            minuteTurn.setAngle((m % 60) * 6 + jitter.get());
            hourTurn.setAngle(((m / 60) % 12) * 30);
            digital.setText(ClockTimes.format(m));
            double ring = Math.toRadians(ClockTimes.dayRingAngle(m));
            marker.setCenterX(CX + RING * Math.sin(ring));
            marker.setCenterY(CY - RING * Math.cos(ring));
        };
        minutes.addListener(o -> draw.run());
        jitter.addListener(o -> draw.run());
        draw.run();

        // Smear: ghosts of the minute hand at recent angles, visible only while it moves fast.
        Deque<Double> history = new ArrayDeque<>();
        AnimationTimer trail = new AnimationTimer() {
            @Override
            public void handle(long now) {
                history.addFirst(minuteTurn.getAngle());
                while (history.size() > 8) history.removeLast();
                Double[] past = history.toArray(Double[]::new);
                for (int i = 0; i < smear.size(); i++) {
                    double angle = past[Math.min(past.length - 1, 2 * (i + 1))];
                    smearTurns.get(i).setAngle(angle);
                    double behind = Math.abs(Math.floorMod((long) Math.round(minuteTurn.getAngle() - angle) + 540, 360) - 180);
                    smear.get(i).setOpacity((0.42 - i * 0.12) * Math.min(1, behind / 24));
                }
            }
        };

        double sweepEnd = FADE_IN + SWEEP_MS;
        double ticksEnd = sweepEnd + plan.ticks().size() * TICK_MS;
        double coverStart = ticksEnd + 40;
        double swapAt = coverStart + 110;
        double holdEnd = swapAt + 170;
        double fadeEnd = holdEnd + 230;

        setOpacity(0);
        Interpolator sweep = Interpolator.SPLINE(0.55, 0, 0.25, 1);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0), new KeyValue(minutes, start),
                        new KeyValue(dim.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(FADE_IN), new KeyValue(opacityProperty(), 1), new KeyValue(minutes, start)),
                new KeyFrame(Duration.millis(560), new KeyValue(fromName.opacityProperty(), 1),
                        new KeyValue(toName.opacityProperty(), 0), new KeyValue(toName.translateXProperty(), 60)),
                new KeyFrame(Duration.millis(820), new KeyValue(fromName.opacityProperty(), 0.35),
                        new KeyValue(toName.opacityProperty(), 1, MotionTokens.OUT),
                        new KeyValue(toName.translateXProperty(), 0, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(sweepEnd), new KeyValue(minutes, plan.sweepTo(), sweep)),
                new KeyFrame(Duration.millis(coverStart), new KeyValue(dim.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(swapAt), e -> swapScreen.run(),
                        new KeyValue(dim.opacityProperty(), 0.96, MotionTokens.IN)),
                new KeyFrame(Duration.millis(holdEnd), new KeyValue(opacityProperty(), 1)),
                new KeyFrame(Duration.millis(fadeEnd), new KeyValue(opacityProperty(), 0, Interpolator.EASE_IN)));
        // Three discrete ticks, each with a tiny overshoot of the minute hand.
        for (int i = 0; i < plan.ticks().size(); i++) {
            double at = sweepEnd + (i + 1) * TICK_MS;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(at), new KeyValue(minutes, plan.ticks().get(i), Interpolator.DISCRETE),
                            new KeyValue(jitter, 1.8, Interpolator.DISCRETE)),
                    new KeyFrame(Duration.millis(at + 70), new KeyValue(jitter, 0, MotionTokens.OUT)));
        }
        timeline.statusProperty().addListener((obs, old, now) -> {
            if (now == Animation.Status.RUNNING) trail.start();
            else trail.stop();
        });

        animation = new ParallelTransition(timeline);
        if (world != null) {
            Animation regrade = world.regrade(to.name().toLowerCase(Locale.ROOT), Duration.millis(SWEEP_MS + 120));
            regrade.setDelay(Duration.millis(FADE_IN));
            animation.getChildren().add(regrade);
        }
    }

    Animation animation() { return animation; }

    private static Node art(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(2 * RADIUS);
        view.setFitHeight(2 * RADIUS);
        view.relocate(CX - RADIUS, CY - RADIUS);
        return view;
    }

    private static Node drawnFace() {
        var face = new Pane();
        face.setMouseTransparent(true);
        Circle disc = new Circle(CX, CY, RADIUS);
        disc.getStyleClass().add("clock-face");
        face.getChildren().add(disc);
        for (int tick = 0; tick < 60; tick++) {
            boolean hour = tick % 5 == 0, major = tick % 15 == 0;
            double length = major ? 26 : hour ? 14 : 6;
            double width = major ? 4 : hour ? 3 : 1.4;
            Polygon mark = new Polygon(CX - width, CY - RADIUS + 10, CX + width + 2, CY - RADIUS + 10,
                    CX + width, CY - RADIUS + 10 + length, CX - width - 2, CY - RADIUS + 10 + length);
            mark.getStyleClass().add(major ? "clock-tick-major" : hour ? "clock-tick" : "clock-tick-minor");
            mark.getTransforms().add(new Rotate(tick * 6, CX, CY));
            face.getChildren().add(mark);
        }
        return face;
    }

    private static Node drawnMinuteHand() {
        Polygon hand = new Polygon(CX - 2, CY + 16, CX + 2, CY + 16, CX + 3, CY - RADIUS + 24, CX - 1, CY - RADIUS + 24);
        hand.getStyleClass().add("clock-hand-minute");
        return hand;
    }

    private static Node drawnHourHand() {
        Polygon hand = new Polygon(CX - 7, CY + 12, CX + 7, CY + 12, CX + 10, CY - RADIUS * 0.55, CX - 4, CY - RADIUS * 0.55);
        hand.getStyleClass().add("clock-hand-hour");
        return hand;
    }
}
