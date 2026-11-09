package com.aphs.classroomuprising.ui;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/**
 * Owns the canvas layers (screen, menu overlay, toasts, transition wipe) and switches screens
 * with the slanted wipe. Input is blocked while a wipe is running, so a click cannot reach a
 * screen that is being replaced.
 */
final class Router {
    private final StackPane screenLayer = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private final Pane wipeLayer = new Pane();
    private final Toasts toasts = new Toasts();
    private AppContext context;
    private Screen current;
    private PauseMenu pause;
    private boolean transitioning;

    Router(StackPane canvas, Scene scene) {
        overlayLayer.setPickOnBounds(false);
        wipeLayer.setMouseTransparent(true);
        StackPane toastLayer = new StackPane(toasts);
        toastLayer.setMouseTransparent(true);
        StackPane.setAlignment(toasts, Pos.TOP_RIGHT);
        StackPane.setMargin(toasts, new Insets(92, 32, 0, 0));
        // Toasts sit under the menu overlay, so the ESC menu is never covered by feedback.
        canvas.getChildren().addAll(screenLayer, toastLayer, overlayLayer, wipeLayer);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::filterKeys);
    }

    void bind(AppContext context) { this.context = context; }

    Toasts toasts() { return toasts; }

    /** Shows a screen. With a caption, the wipe band carries that text (e.g. the next time of day). */
    void show(Screen next, String caption) {
        closePause();
        next.root();
        if (current == null) {
            swap(next);
            return;
        }
        wipe(caption, () -> swap(next));
    }

    private void swap(Screen next) {
        if (current != null) current.onHidden();
        screenLayer.getChildren().setAll(next.root());
        current = next;
        next.onShown();
        next.focus();
    }

    private void wipe(String caption, Runnable midpoint) {
        transitioning = true;
        screenLayer.setMouseTransparent(true);
        Polygon trail = new Polygon(180, 0, 1760, 0, 1580, 720, 0, 720);
        trail.getStyleClass().add("wipe-trail");
        trail.setTranslateX(-90);
        Polygon band = new Polygon(180, 0, 1700, 0, 1520, 720, 0, 720);
        band.getStyleClass().add("wipe-band");
        Group wipe = new Group(trail, band);
        if (caption != null) {
            Label text = new Label(caption);
            text.getStyleClass().addAll("display", "wipe-caption");
            text.relocate(560, 300);
            wipe.getChildren().add(text);
        }
        wipe.setTranslateX(-1900);
        wipeLayer.getChildren().add(wipe);

        TranslateTransition in = new TranslateTransition(Duration.millis(240), wipe);
        in.setToX(-170);
        in.setInterpolator(Interpolator.SPLINE(0.6, 0, 0.4, 1));
        in.setOnFinished(e -> midpoint.run());
        TranslateTransition out = new TranslateTransition(Duration.millis(300), wipe);
        out.setToX(1500);
        out.setInterpolator(Interpolator.SPLINE(0.6, 0, 0.4, 1));
        SequentialTransition all = new SequentialTransition(in, new PauseTransition(Duration.millis(caption == null ? 90 : 380)), out);
        all.setOnFinished(e -> {
            wipeLayer.getChildren().remove(wipe);
            screenLayer.setMouseTransparent(false);
            transitioning = false;
        });
        all.play();
    }

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

    private void openPause() {
        pause = new PauseMenu(context, this::closePause);
        overlayLayer.getChildren().setAll(pause.root());
        pause.onShown();
        pause.focus();
    }

    void closePause() {
        if (pause == null) return;
        overlayLayer.getChildren().clear();
        pause = null;
        if (current != null) current.focus();
    }
}
