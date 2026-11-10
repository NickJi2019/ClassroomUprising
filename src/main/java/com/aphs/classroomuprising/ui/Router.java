package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.ui.scene.FrameSequence;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.Optional;

/**
 * Owns the canvas layers (screen, toasts, menu overlay, transitions) and every screen change.
 *
 * <ul>
 *   <li>Screen change: the old screen's UI steps out; the new screen opens out of the old one
 *       through a growing slanted mask that starts where the player made their choice; a tinted,
 *       enlarged copy of the new screen runs just ahead of the edge; the new UI steps in.</li>
 *   <li>If the artists deliver a frame sequence named "screen", it plays instead.</li>
 *   <li>Time change within a day: the clock ({@link ClockShift}).</li>
 *   <li>Esc menu: opens and closes through a stepped mask (or the "menu-open" sequence).</li>
 * </ul>
 * Input is blocked while any of these runs, so nothing reaches a screen being replaced.
 */
final class Router {
    private static final double W = ClassroomApp.WIDTH, H = ClassroomApp.HEIGHT;
    /** The Esc menu opens out of its "MENU" title. */
    private static final Point2D MENU_ORIGIN = new Point2D(150, 95);

    private final StackPane canvas;
    private final StackPane screenLayer = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private final Pane transitionLayer = new Pane();
    private final Toasts toasts = new Toasts();
    private final TipLayer tips = new TipLayer();
    /** A tip asked for during a transition; it appears once the transition has finished. */
    private TutorialTips.Tip pendingTip;
    private AppContext context;
    private Screen current;
    private PauseMenu pause;
    private boolean transitioning;
    private Point2D origin;

    Router(StackPane canvas, Scene scene) {
        this.canvas = canvas;
        overlayLayer.setPickOnBounds(false);
        transitionLayer.setMouseTransparent(true);
        StackPane toastLayer = new StackPane(toasts);
        toastLayer.setMouseTransparent(true);
        StackPane.setAlignment(toasts, Pos.TOP_RIGHT);
        StackPane.setMargin(toasts, new Insets(92, 32, 0, 0));
        // Toasts and tips sit under the menu overlay, so the Esc menu is never covered by them.
        canvas.getChildren().addAll(screenLayer, tips, toastLayer, overlayLayer, transitionLayer);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::filterKeys);
        scene.addEventHandler(RevealOriginEvent.ACTIVATED, e -> origin = canvas.sceneToLocal(e.sceneX(), e.sceneY()));
    }

    void bind(AppContext context) { this.context = context; }

    Toasts toasts() { return toasts; }

    /** Shows a tutorial tip now, or after the transition that is running. */
    void tip(TutorialTips.Tip tip) {
        if (transitioning) pendingTip = tip;
        else tips.show(tip);
    }

    /** Shows a screen; a caption (e.g. the next time of day) is shown during the change. */
    void show(Screen next, String caption) {
        closePauseNow();
        dropTips();
        next.root();
        if (current == null) {
            swap(next);
            return;
        }
        Optional<FrameSequence> drawn = sequence("screen");
        if (drawn.isPresent()) playSequence(next, drawn.get());
        else reveal(next, caption);
    }

    /** Shows a screen with the clock: hands sweep from one time of day to the next. */
    void showTimeChange(Screen next, DayPeriod from, DayPeriod to) {
        closePauseNow();
        dropTips();
        next.root();
        if (current == null) {
            swap(next);
            return;
        }
        // The scene behind the clock is re-graded to the new time of day while the hands turn.
        SceneView world = current.root().lookup(".scene-view") instanceof SceneView scene ? scene : null;
        ClockShift clock = new ClockShift(from, to, () -> swap(next),
                context == null ? null : context.assets(), world);
        begin();
        transitionLayer.getChildren().add(clock);
        Animation animation = clock.animation();
        animation.setOnFinished(e -> {
            transitionLayer.getChildren().remove(clock);
            end();
        });
        animation.play();
    }

    // ---- screen change: mask reveal -----------------------------------------------------

    private void reveal(Screen next, String caption) {
        Screen previous = current;
        Parent oldRoot = previous.root();
        Parent newRoot = next.root();
        Point2D from = takeOrigin();
        begin();

        Animation exit = exit(oldRoot);
        screenLayer.getChildren().add(newRoot);
        current = next;
        next.onShown();
        next.focus();
        newRoot.applyCss();
        newRoot.layout();
        Image picture = newRoot.snapshot(canvasArea(), null);

        Polygon mask = new Polygon();
        newRoot.setClip(mask);
        ImageView echoView = new ImageView(picture);
        echoView.getTransforms().add(new Scale(1.12, 1.12, from.getX(), from.getY()));
        // A cool, colourless ghost of the next screen: tinting colour directly would shift hues (yellow to green).
        echoView.setEffect(new javafx.scene.effect.ColorAdjust(0, -1, 0.05, 0.1));
        Rectangle tint = new Rectangle(W, H);
        tint.getStyleClass().add("reveal-echo-tint");
        tint.setBlendMode(BlendMode.MULTIPLY);
        Pane echo = new Pane(echoView, tint);
        echo.setMinSize(W, H);
        echo.setPrefSize(W, H);
        echo.setMaxSize(W, H);
        echo.setMouseTransparent(true);
        Polygon echoMask = new Polygon();
        echo.setClip(echoMask);
        screenLayer.getChildren().add(screenLayer.getChildren().indexOf(newRoot), echo);

        Polygon edge = new Polygon();
        edge.getStyleClass().add("reveal-edge");
        transitionLayer.getChildren().add(edge);

        DoubleProperty inner = new SimpleDoubleProperty(0);
        DoubleProperty outer = new SimpleDoubleProperty(0);
        inner.addListener((obs, a, b) -> {
            Double[] points = boxed(RevealMask.polygon(from.getX(), from.getY(), b.doubleValue(), 8));
            mask.getPoints().setAll(points);
            edge.getPoints().setAll(points);
        });
        outer.addListener((obs, a, b) -> echoMask.getPoints().setAll(
                boxed(RevealMask.polygon(from.getX(), from.getY(), b.doubleValue(), 14))));
        Timeline open = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(inner, 0), new KeyValue(outer, 0)),
                new KeyFrame(Duration.millis(70), new KeyValue(inner, 0)),
                new KeyFrame(Duration.millis(140), new KeyValue(outer, 0)),
                new KeyFrame(Duration.millis(420), new KeyValue(outer, 1, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(540), new KeyValue(inner, 1, MotionTokens.REVEAL)));

        ParallelTransition all = new ParallelTransition(exit, open);
        Node captionNode = caption == null ? null : caption(caption);
        if (captionNode != null) all.getChildren().add(captionAnimation(captionNode));
        enter(newRoot, 170);
        all.setOnFinished(e -> {
            newRoot.setClip(null);
            screenLayer.getChildren().removeAll(oldRoot, echo);
            transitionLayer.getChildren().removeAll(edge);
            if (captionNode != null) transitionLayer.getChildren().remove(captionNode);
            previous.onHidden();
            reset(oldRoot);
            end();
        });
        all.play();
    }

    /** The old screen's UI (not its art) slides out and fades, one layer after another. */
    private static Animation exit(Parent root) {
        ParallelTransition all = new ParallelTransition();
        int i = 0;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof SceneView) continue;
            TranslateTransition move = new TranslateTransition(Duration.millis(150), child);
            move.setByX(-40);
            move.setInterpolator(MotionTokens.IN);
            FadeTransition fade = new FadeTransition(Duration.millis(150), child);
            fade.setToValue(0);
            ParallelTransition one = new ParallelTransition(move, fade);
            one.setDelay(Duration.millis(i++ * 25));
            all.getChildren().add(one);
        }
        return all;
    }

    /** The new screen's UI layers step in after the mask has started opening. */
    private static void enter(Parent root, double delay) {
        int i = 0;
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof SceneView) continue;
            Motion.slideIn(child, delay + i++ * 50);
        }
    }

    private static void reset(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            child.setTranslateX(0);
            child.setOpacity(1);
        }
    }

    private Node caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("display", "reveal-caption-label");
        Slant tag = new Slant(label);
        tag.getStyleClass().addAll("tag-caret", "reveal-caption");
        tag.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane layer = new StackPane(tag);
        layer.setPrefSize(W, H);
        layer.setMouseTransparent(true);
        layer.setOpacity(0);
        transitionLayer.getChildren().add(layer);
        return layer;
    }

    private static Animation captionAnimation(Node layer) {
        return new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(layer.opacityProperty(), 0), new KeyValue(layer.translateXProperty(), 40)),
                new KeyFrame(Duration.millis(140), new KeyValue(layer.opacityProperty(), 1, MotionTokens.OUT),
                        new KeyValue(layer.translateXProperty(), 0, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(380), new KeyValue(layer.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(540), new KeyValue(layer.opacityProperty(), 0, MotionTokens.IN),
                        new KeyValue(layer.translateXProperty(), -30, MotionTokens.IN)));
    }

    // ---- screen change: artist frame sequence -------------------------------------------

    private void playSequence(Screen next, FrameSequence frames) {
        begin();
        origin = null;
        ImageView view = new ImageView();
        view.setMouseTransparent(true);
        transitionLayer.getChildren().add(view);
        Animation animation = frames.play(view, W, H, () -> swap(next));
        animation.setOnFinished(e -> {
            transitionLayer.getChildren().remove(view);
            end();
        });
        animation.play();
    }

    private Optional<FrameSequence> sequence(String name) {
        return context == null || context.assets() == null ? Optional.empty() : context.assets().sequence(name);
    }

    private void swap(Screen next) {
        if (current != null) current.onHidden();
        screenLayer.getChildren().setAll(next.root());
        current = next;
        next.onShown();
        next.focus();
    }

    // ---- Esc menu -----------------------------------------------------------------------

    private void openPause() {
        Image backdrop = screenLayer.snapshot(canvasArea(), null);
        pause = new PauseMenu(context, this::closePause, backdrop);
        Parent root = pause.root();
        overlayLayer.getChildren().setAll(root);
        pause.onShown();
        pause.focus();
        begin();
        Optional<FrameSequence> drawn = sequence("menu-open");
        if (drawn.isPresent()) {
            root.setVisible(false);
            ImageView view = new ImageView();
            transitionLayer.getChildren().add(view);
            Animation animation = drawn.get().play(view, W, H, () -> root.setVisible(true));
            animation.setOnFinished(e -> {
                transitionLayer.getChildren().remove(view);
                end();
            });
            animation.play();
            return;
        }
        Animation open = maskAnimation(root, 0, 1, Duration.millis(320), 12);
        open.setOnFinished(e -> {
            root.setClip(null);
            end();
        });
        open.play();
    }

    /** Closes the Esc menu through the same stepped mask, in reverse. */
    void closePause() {
        if (pause == null || transitioning) return;
        Parent root = pause.root();
        begin();
        Animation close = maskAnimation(root, 1, 0, Duration.millis(180), 8);
        close.setOnFinished(e -> {
            closePauseNow();
            end();
        });
        close.play();
    }

    private void closePauseNow() {
        if (pause == null) return;
        overlayLayer.getChildren().clear();
        pause.onHidden();
        pause = null;
        if (current != null) current.focus();
    }

    /** A mask growing or shrinking from the menu title in a few hard steps, like drawn frames. */
    private static Animation maskAnimation(Parent root, double from, double to, Duration duration, int steps) {
        Polygon mask = new Polygon();
        root.setClip(mask);
        DoubleProperty progress = new SimpleDoubleProperty(-1);
        progress.addListener((obs, a, b) -> mask.getPoints().setAll(
                boxed(RevealMask.polygon(MENU_ORIGIN.getX(), MENU_ORIGIN.getY(), b.doubleValue(), 10))));
        progress.set(from);
        return new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress, from)),
                new KeyFrame(duration, new KeyValue(progress, to, MotionTokens.steps(steps))));
    }

    // ---- input --------------------------------------------------------------------------

    private void filterKeys(KeyEvent event) {
        if (transitioning) {
            event.consume();
            return;
        }
        if (event.getCode() != KeyCode.ESCAPE) return;
        if (pause != null) closePause();
        else if (current != null && current.allowsPause() && context.hasSession()) openPause();
        else return;
        event.consume();
    }

    private void begin() {
        transitioning = true;
        screenLayer.setMouseTransparent(true);
        overlayLayer.setMouseTransparent(true);
    }

    private void end() {
        transitioning = false;
        screenLayer.setMouseTransparent(false);
        overlayLayer.setMouseTransparent(false);
        if (pendingTip != null) {
            tips.show(pendingTip);
            pendingTip = null;
        }
    }

    /** A tip belongs to the screen it was shown on. */
    private void dropTips() {
        pendingTip = null;
        tips.clear();
    }

    private Point2D takeOrigin() {
        Point2D at = origin == null ? new Point2D(W / 2, H / 2) : origin;
        origin = null;
        return new Point2D(Math.max(0, Math.min(W, at.getX())), Math.max(0, Math.min(H, at.getY())));
    }

    /**
     * Snapshot settings for exactly the visible 1280x720 canvas. Screens contain large
     * typography that overhangs the canvas, so a plain snapshot would be wider than the screen.
     */
    private static SnapshotParameters canvasArea() {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setViewport(new javafx.geometry.Rectangle2D(0, 0, W, H));
        return parameters;
    }

    private static Double[] boxed(double[] values) {
        Double[] out = new Double[values.length];
        for (int i = 0; i < values.length; i++) out[i] = values[i];
        return out;
    }
}
