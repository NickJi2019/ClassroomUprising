package com.aphs.classroomuprising.ui;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Hold to fast-forward, for the whole window. Holding Enter, Space or the left mouse button for
 * {@link #HOLD} makes {@link #active()} true until it is released; whatever can play faster
 * (dialogue, screen transitions, the clock) follows it. A tap still does what a tap does.
 *
 * <p>Safety: while a key is held, its keyboard auto-repeat never reaches the game, so holding
 * a key cannot press a menu that appears meanwhile; and when a held mouse button actually
 * fast-forwarded something, the click it ends in is swallowed for the same reason. Typing in a
 * text field is left alone.</p>
 */
final class FastForward {
    static final Duration HOLD = Duration.millis(380);
    /** How much faster transitions play while held. */
    static final double RATE = 3;

    private final ReadOnlyBooleanWrapper active = new ReadOnlyBooleanWrapper(false);
    private final PauseTransition holdTimer = new PauseTransition(HOLD);
    private boolean keyDown;
    private boolean mouseDown;
    private boolean used;
    private boolean swallowClick;

    FastForward(Scene scene) {
        holdTimer.setOnFinished(e -> { if (keyDown || mouseDown) active.set(true); });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!advanceKey(e.getCode()) || typing(e.getTarget())) return;
            if (keyDown) {
                e.consume();   // auto-repeat of the held key
                return;
            }
            keyDown = true;
            used = false;
            holdTimer.playFromStart();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (!advanceKey(e.getCode())) return;
            keyDown = false;
            settle();
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            swallowClick = false;
            if (e.getButton() != MouseButton.PRIMARY || typing(e.getTarget())) return;
            mouseDown = true;
            used = false;
            holdTimer.playFromStart();
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() != MouseButton.PRIMARY || !mouseDown) return;
            mouseDown = false;
            swallowClick = active.get() && used;
            settle();
        });
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (swallowClick && e.getButton() == MouseButton.PRIMARY) {
                swallowClick = false;
                e.consume();
            }
        });
        // A key released while the window was not focused never reports it: forget the hold.
        scene.windowProperty().addListener((obs, old, window) -> {
            if (window != null) window.focusedProperty().addListener((o, was, now) -> { if (!now) release(); });
        });
    }

    /** True while Enter, Space or the mouse button has been held long enough. */
    ReadOnlyBooleanProperty active() { return active.getReadOnlyProperty(); }

    /** Something was actually fast-forwarded during this hold (so the click it ends in must not count). */
    void markUsed() { if (active.get()) used = true; }

    /** Forgets any hold (the window lost focus). */
    void release() {
        keyDown = false;
        mouseDown = false;
        swallowClick = false;
        settle();
    }

    private void settle() {
        if (keyDown || mouseDown) return;
        holdTimer.stop();
        active.set(false);
    }

    static boolean advanceKey(KeyCode code) { return code == KeyCode.ENTER || code == KeyCode.SPACE; }

    /** Inside a text field or code editor, Enter, Space and dragging belong to the text. */
    private static boolean typing(EventTarget target) {
        for (Node node = target instanceof Node n ? n : null; node != null; node = node.getParent())
            if (node instanceof TextInputControl) return true;
        return false;
    }
}
