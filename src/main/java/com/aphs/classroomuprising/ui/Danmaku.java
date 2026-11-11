package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Mr. Reward's questions as bullets in the opening exam, drawn over the scene: each is a slanted
 * question strip (its number and the start of its prompt), red for his. At the start they spring
 * from his chest and spread along a slight arc in front of him; each round the next one is driven
 * at the player like a battering ram (it draws back, then flies straight); an answer goes back
 * the same way as the player's own strip, in the theme colour. Presentation only: which question
 * comes next and what an answer did come from the core.
 */
final class Danmaku extends Pane {
    private static final double ARC_RADIUS = 235, TOP_DEG = 226, BOTTOM_DEG = 134;
    private static final Color CARET = Color.web("#FFD23F"), CYAN = Color.web("#5CE1E6"), RED = Color.web("#FF4D6D");

    private record Bullet(Group node, Timeline bob) {}

    private final Map<String, Bullet> bullets = new LinkedHashMap<>();

    Danmaku() {
        setMouseTransparent(true);
        setPickOnBounds(false);
    }

    /** Questions still waiting to be fired, in firing order. */
    List<String> waiting() { return List.copyOf(bullets.keySet()); }

    /**
     * The questions burst out of {@code origin} and spread to an arc in front of him, the first
     * to be fired at the top. Each strip shows its number ("Q1") and a short line of its prompt.
     */
    Animation fanOut(List<String> ids, List<String> numbers, List<String> lines, Point2D origin) {
        ParallelTransition all = new ParallelTransition();
        for (int i = 0; i < ids.size(); i++) {
            Group node = strip(numbers.get(i), lines.get(i), "bullet-reward", RED);
            node.setLayoutX(origin.getX());
            node.setLayoutY(origin.getY());
            node.setScaleX(0.2);
            node.setScaleY(0.2);
            node.setOpacity(0);
            getChildren().add(node);
            Point2D slot = slot(i, ids.size(), origin);
            Timeline out = new Timeline(
                    new KeyFrame(Duration.millis(520),
                            new KeyValue(node.layoutXProperty(), slot.getX(), MotionTokens.OUT),
                            new KeyValue(node.layoutYProperty(), slot.getY(), MotionTokens.OUT),
                            new KeyValue(node.scaleXProperty(), 1, MotionTokens.OUT),
                            new KeyValue(node.scaleYProperty(), 1, MotionTokens.OUT),
                            new KeyValue(node.rotateProperty(), 0, MotionTokens.OUT)),
                    new KeyFrame(Duration.millis(160), new KeyValue(node.opacityProperty(), 1)));
            node.setRotate(-24);
            out.setDelay(Duration.millis(i * 55));
            // Hovering: each strip drifts a little on its own beat, like a bullet held in the air.
            Timeline bob = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(node.translateXProperty(), -3)),
                    new KeyFrame(Duration.millis(900 + i * 37), new KeyValue(node.translateXProperty(), 3, Interpolator.EASE_BOTH)));
            bob.setAutoReverse(true);
            bob.setCycleCount(Animation.INDEFINITE);
            out.setOnFinished(e -> bob.play());
            bullets.put(ids.get(i), new Bullet(node, bob));
            all.getChildren().add(out);
        }
        return all;
    }

    /**
     * Fires one question at {@code target} along a curve, leaving a trail; when it lands it
     * bursts and {@code onImpact} runs. An unknown id lands at once.
     */
    Animation fire(String id, Point2D target, Runnable onImpact) {
        Bullet bullet = bullets.remove(id);
        if (bullet == null) {
            PauseTransition now = new PauseTransition(Duration.ONE);
            now.setOnFinished(e -> onImpact.run());
            return now;
        }
        bullet.bob().stop();
        Group node = bullet.node();
        node.setTranslateX(0);
        node.toFront();
        return ram(node, target, RED, 1, () -> {
            getChildren().remove(node);
            burst(target, RED, CARET);
            onImpact.run();
        });
    }

    /**
     * The player's answer flying back at Mr. Reward: the question returns to its strip, in the
     * theme colour, and is driven at him like a battering ram. A right answer lands ({@code hits})
     * and bursts; a wrong one stops short of him and breaks. {@code onDone} gets whether it landed.
     */
    Animation counter(Point2D from, Point2D target, String number, String line, boolean hits, Consumer<Boolean> onDone) {
        Group node = strip(number, line, "bullet-player", CARET);
        node.setLayoutX(from.getX());
        node.setLayoutY(from.getY());
        node.setScaleX(0.1);
        node.setScaleY(0.1);
        getChildren().add(node);
        // It unfolds from the point the paper folded into.
        Timeline unfold = new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(node.scaleXProperty(), 1, MotionTokens.OUT),
                new KeyValue(node.scaleYProperty(), 1, MotionTokens.OUT)));
        Point2D end = hits ? target : from.add(target.subtract(from).multiply(0.7));
        Animation drive = ram(node, end, CARET, 1, () -> {
            if (hits) {
                getChildren().remove(node);
                burst(target, CARET, Color.WHITE);
            } else {
                // It stops short of him and breaks apart.
                Timeline fall = new Timeline(new KeyFrame(Duration.millis(320),
                        new KeyValue(node.opacityProperty(), 0),
                        new KeyValue(node.rotateProperty(), node.getRotate() + 38, javafx.animation.Interpolator.EASE_IN),
                        new KeyValue(node.layoutYProperty(), end.getY() + 90, javafx.animation.Interpolator.EASE_IN)));
                fall.setOnFinished(f -> getChildren().remove(node));
                fall.play();
            }
            onDone.accept(hits);
        });
        unfold.setOnFinished(e -> drive.play());
        return unfold;
    }

    /**
     * A battering ram: the strip turns to face its target, draws back away from it, holds for
     * a beat, then drives at it in a straight line, faster and faster, leaving sparks.
     */
    private Animation ram(Group node, Point2D target, Color trail, double scale, Runnable onImpact) {
        Point2D start = new Point2D(node.getLayoutX(), node.getLayoutY());
        Point2D way = target.subtract(start);
        double length = Math.max(1, way.magnitude());
        Point2D back = start.subtract(way.multiply(82 / length));
        // The strip's left end leads when it travels left, its right end when it travels right.
        double angle = Math.toDegrees(Math.atan2(way.getY(), way.getX()));
        double facing = way.getX() < 0 ? angle - 180 : angle;
        Timeline drawBack = new Timeline(new KeyFrame(Duration.millis(300),
                new KeyValue(node.layoutXProperty(), back.getX(), MotionTokens.OUT),
                new KeyValue(node.layoutYProperty(), back.getY(), MotionTokens.OUT),
                new KeyValue(node.rotateProperty(), facing, MotionTokens.OUT),
                new KeyValue(node.scaleXProperty(), scale * 1.12, MotionTokens.OUT),
                new KeyValue(node.scaleYProperty(), scale * 0.88, MotionTokens.OUT)));
        PauseTransition hold = new PauseTransition(Duration.millis(90));
        Animation drive = flight(node, back, target, Duration.millis(260), trail);
        Timeline stretch = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(node.scaleXProperty(), scale * 1.25, javafx.animation.Interpolator.EASE_IN),
                new KeyValue(node.scaleYProperty(), scale * 0.8, javafx.animation.Interpolator.EASE_IN)));
        ParallelTransition charge = new ParallelTransition(drive, stretch);
        charge.setOnFinished(e -> onImpact.run());
        return new javafx.animation.SequentialTransition(drawBack, hold, charge);
    }

    /** The bullets still waiting fade away (the exam is over). */
    void clear() {
        for (Bullet bullet : bullets.values()) {
            bullet.bob().stop();
            FadeTransition fade = new FadeTransition(Duration.millis(400), bullet.node());
            fade.setToValue(0);
            fade.setOnFinished(e -> getChildren().remove(bullet.node()));
            fade.play();
        }
        bullets.clear();
    }

    /** Where the {@code i}-th of {@code n} bullets hovers: an arc on his left, first at the top. */
    static Point2D slot(int i, int n, Point2D origin) {
        double deg = n <= 1 ? 180 : TOP_DEG + (BOTTOM_DEG - TOP_DEG) * i / (n - 1.0);
        double rad = Math.toRadians(deg);
        return new Point2D(origin.getX() + ARC_RADIUS * Math.cos(rad), origin.getY() + ARC_RADIUS * Math.sin(rad));
    }

    // ---- drawing ------------------------------------------------------------------------

    /**
     * A question as a slanted strip: its number on a caret tag, then the start of its prompt.
     * The group's origin is the strip's centre, so it can be placed and flown by its middle.
     */
    private static Group strip(String number, String line, String side, Color glow) {
        javafx.scene.control.Label tag = new javafx.scene.control.Label(number);
        tag.getStyleClass().addAll("display", "bullet-number");
        Slant numberTag = new Slant(tag);
        numberTag.getStyleClass().add("bullet-number-tag");
        javafx.scene.control.Label text = new javafx.scene.control.Label(line);
        text.getStyleClass().addAll("display", "bullet-text");
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, numberTag, text);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Slant strip = new Slant(row);
        strip.getStyleClass().addAll("bullet-strip", side);
        strip.layoutXProperty().bind(strip.widthProperty().divide(-2));
        strip.layoutYProperty().bind(strip.heightProperty().divide(-2));
        Group node = new Group(strip);
        node.setEffect(new DropShadow(16, glow));
        // Drawn once into a bitmap, glow and all; moving, scaling and turning it then reuse that.
        node.setCache(true);
        node.setCacheHint(javafx.scene.CacheHint.SPEED);
        return node;
    }

    /** Moves a node in a straight line, faster and faster, dropping fading sparks behind it. */
    private Animation flight(Node node, Point2D a, Point2D c, Duration time, Color trail) {
        return new Transition() {
            private double lastSpark = -1;
            {
                setCycleDuration(time);
                setInterpolator(javafx.animation.Interpolator.SPLINE(0.55, 0, 0.9, 0.6));
            }

            @Override
            protected void interpolate(double t) {
                double x = a.getX() + (c.getX() - a.getX()) * t;
                double y = a.getY() + (c.getY() - a.getY()) * t;
                node.setLayoutX(x);
                node.setLayoutY(y);
                if (t - lastSpark >= 0.07) {
                    lastSpark = t;
                    spark(x, y, trail);
                }
            }
        };
    }

    private void spark(double x, double y, Color color) {
        Circle dot = new Circle(x, y, 7, color.deriveColor(0, 1, 1, 0.55));
        dot.setMouseTransparent(true);
        getChildren().add(0, dot);
        Timeline fade = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(dot.opacityProperty(), 0), new KeyValue(dot.radiusProperty(), 2)));
        fade.setOnFinished(e -> getChildren().remove(dot));
        fade.play();
    }

    /** Two rings bursting outward where something lands. */
    private void burst(Point2D at, Color first, Color second) {
        for (int i = 0; i < 2; i++) {
            Circle ring = new Circle(at.getX(), at.getY(), 8);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(i == 0 ? first : second);
            ring.setStrokeWidth(i == 0 ? 6 : 3);
            getChildren().add(ring);
            Timeline grow = new Timeline(new KeyFrame(Duration.millis(340 + i * 90),
                    new KeyValue(ring.radiusProperty(), 70 + i * 30, MotionTokens.OUT),
                    new KeyValue(ring.opacityProperty(), 0, Interpolator.EASE_IN)));
            grow.setOnFinished(e -> getChildren().remove(ring));
            grow.play();
        }
    }
}
