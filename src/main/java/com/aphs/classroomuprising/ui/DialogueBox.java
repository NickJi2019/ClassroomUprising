package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Bottom-of-screen dialogue: a name tag, typewriter text and an advance marker.
 * {@link #advance()} first completes the line being typed, then moves to the next line.
 *
 * <p>Enter, Space or the mouse button: a tap advances once; holding it for a moment
 * fast-forwards (each line completes, then the next starts, quickly) until it is released or
 * the script ends. A key still held when the script ends never reaches whatever appears next
 * (such as a choice menu), so fast-forward cannot choose anything. Every shown line goes into a
 * {@link DialogueHistory} for the log; scrolling up over the box asks for the log.</p>
 */
public class DialogueBox extends StackPane {
    private final Label speaker = new Label();
    private final Label text = new Label();
    private final Label more = new Label("▼");
    private final FadeTransition blink = new FadeTransition(Duration.millis(520), more);
    private List<DialogueScript.Line> lines = List.of();
    private int index;
    private Transition typing;
    private Runnable onFinished = () -> {};
    private java.util.function.Consumer<DialogueScript.Line> onLine = line -> {};
    private boolean finished = true;
    /** The full text of the line on screen, used to size the box before and while it types. */
    private String current = "";
    private final DialogueHistory history = new DialogueHistory();
    /** How long Enter/Space/the mouse must be held before fast-forward starts. */
    private static final Duration HOLD = Duration.millis(380);
    /** One fast-forward step: complete the line on screen, or start the next one. */
    private static final Duration FAST_STEP = Duration.millis(70);
    private final PauseTransition holdTimer = new PauseTransition(HOLD);
    private final Timeline fastTimer = new Timeline(new KeyFrame(FAST_STEP, e -> fastStep()));
    private final Slant fastTag;
    private boolean held;
    private BooleanSupplier advanceAllowed = () -> true;
    private Runnable onHistory = () -> {};

    public DialogueBox() {
        getStyleClass().add("dialogue-box");
        speaker.getStyleClass().addAll("display", "name-tag-label");
        Slant tag = new Slant(speaker);
        tag.getStyleClass().add("name-tag");
        tag.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        StackPane.setAlignment(tag, Pos.TOP_LEFT);
        tag.setTranslateY(-48);
        tag.setTranslateX(-16);

        text.getStyleClass().add("dialogue-text");
        text.setWrapText(true);
        StackPane.setAlignment(text, Pos.TOP_LEFT);

        more.getStyleClass().add("dialogue-more");
        StackPane.setAlignment(more, Pos.BOTTOM_RIGHT);
        more.setVisible(false);
        blink.setFromValue(1);
        blink.setToValue(0.2);
        blink.setAutoReverse(true);
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();

        Label fastLabel = new Label("FAST");
        fastLabel.getStyleClass().addAll("display", "dialogue-fast-label");
        HBox fastMark = new HBox(4, arrow(), arrow(), fastLabel);
        fastMark.setAlignment(Pos.CENTER_LEFT);
        fastTag = new Slant(fastMark);
        fastTag.getStyleClass().add("dialogue-fast");
        fastTag.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        StackPane.setAlignment(fastTag, Pos.TOP_RIGHT);
        fastTag.setTranslateY(-40);
        fastTag.setVisible(false);
        fastTag.setMouseTransparent(true);

        holdTimer.setOnFinished(e -> startFast());
        fastTimer.setCycleCount(Animation.INDEFINITE);

        getChildren().addAll(text, tag, more, fastTag);
        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) press();
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) release();
        });
        addEventHandler(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() > 0) onHistory.run();
        });
        widthProperty().addListener((obs, a, b) -> fit());
    }

    private static Polygon arrow() {
        Polygon arrow = new Polygon(0, 0, 9, 6, 0, 12);
        arrow.getStyleClass().add("dialogue-fast-arrow");
        return arrow;
    }

    /**
     * Lets Enter and Space on {@code root} drive this box: a tap advances, holding fast-forwards.
     * While the key is held, further presses of it (the keyboard's auto-repeat) are swallowed, so a
     * key held past the end of the script cannot activate the menu that appears.
     */
    void attachKeys(Node root) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (advanceKey(e.getCode()) && held) e.consume();
        });
        root.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (advanceKey(e.getCode())) release();
        });
        root.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (advanceKey(e.getCode()) && !finished && advanceAllowed.getAsBoolean()) {
                press();
                e.consume();
            }
        });
    }

    private static boolean advanceKey(KeyCode code) { return code == KeyCode.ENTER || code == KeyCode.SPACE; }

    /** When this is false (a slide shown large, the log open) neither taps nor fast-forward advance. */
    void setAdvanceAllowed(BooleanSupplier allowed) { advanceAllowed = allowed; }

    /** Called when the player asks for the log by scrolling up over the box. */
    void setOnHistory(Runnable action) { onHistory = action; }

    /** Every line shown so far, with the slide that was up. */
    List<DialogueHistory.Entry> history() { return history.entries(); }

    /** Whether Enter/Space/the mouse button is being held on the box. */
    boolean holding() { return held; }

    /** Whether fast-forward is running. */
    boolean fastForwarding() { return fastTimer.getStatus() == Animation.Status.RUNNING; }

    /** The advance key or button went down: advance once now, fast-forward if it stays down. */
    void press() {
        if (held) return;
        held = true;
        if (advanceAllowed.getAsBoolean()) advance();
        holdTimer.playFromStart();
    }

    /** The advance key or button came up: fast-forward stops. */
    void release() {
        held = false;
        holdTimer.stop();
        stopFast();
    }

    private void startFast() {
        if (!held || finished) return;
        fastTag.setVisible(true);
        fastTimer.play();
    }

    private void stopFast() {
        fastTimer.stop();
        fastTag.setVisible(false);
    }

    /** One step of fast-forward (no per-line sound): finish the typing, or go on to the next line. */
    private void fastStep() {
        if (finished) {
            stopFast();
            return;
        }
        if (!advanceAllowed.getAsBoolean()) return;
        if (typing != null && typing.getStatus() == Animation.Status.RUNNING) {
            typing.stop();
            text.setText(lines.get(index).text());
            more.setVisible(true);
        } else {
            next();
            if (finished) stopFast();
        }
    }

    /** Plays the lines in order and runs {@code whenDone} once after the last line is dismissed. */
    public void play(List<DialogueScript.Line> script, Runnable whenDone) {
        lines = List.copyOf(script);
        onFinished = whenDone;
        finished = false;
        index = -1;
        history.beginScript();
        next();
    }

    /** Shows one line with no follow-up action. */
    public void say(String who, String line) {
        say(new DialogueScript.Line(who, line));
    }

    /** Shows one prepared line (speaker art and all) with no follow-up action. */
    void say(DialogueScript.Line line) {
        play(List.of(line), () -> {});
        finished = true;
    }

    public boolean isFinished() { return finished; }


    /** Called as each line starts, e.g. to put its speaker in focus. */
    void setOnLine(java.util.function.Consumer<DialogueScript.Line> listener) { onLine = listener; }

    public void advance() {
        if (typing != null && typing.getStatus() == Animation.Status.RUNNING) {
            typing.stop();
            text.setText(lines.get(index).text());
            more.setVisible(true);
            return;
        }
        if (finished) return;
        Sfx.DIALOGUE.play(this);
        next();
    }

    private void next() {
        index++;
        if (index >= lines.size()) {
            finished = true;
            more.setVisible(false);
            onFinished.run();
            return;
        }
        DialogueScript.Line line = lines.get(index);
        speaker.setText(line.speaker().toUpperCase(Locale.ROOT));
        history.shown(line);
        onLine.accept(line);
        type(line.text());
    }

    private void type(String full) {
        more.setVisible(false);
        current = full;
        fit();
        text.setText("");
        typing = new Transition() {
            {
                setCycleDuration(Duration.millis(Math.max(240, full.length() * 22)));
                setInterpolator(Interpolator.LINEAR);
            }
            @Override
            protected void interpolate(double fraction) {
                text.setText(full.substring(0, (int) Math.round(full.length() * fraction)));
            }
        };
        typing.setOnFinished(e -> more.setVisible(true));
        typing.play();
    }

    /**
     * Grows the box upwards when the whole current line needs more rows than its layout height
     * gives (long authored lines, other fonts); it never shrinks below that height. Measured on
     * the full line, so the box does not jump while the text types.
     */
    private void fit() {
        double width = getWidth() - snappedLeftInset() - snappedRightInset();
        if (width <= 0) return;
        String shown = text.getText();
        text.setText(current);
        text.applyCss();
        double needed = text.prefHeight(width) + snappedTopInset() + snappedBottomInset();
        text.setText(shown);
        setMinHeight(Math.max(getPrefHeight(), Math.ceil(needed)));
    }
}
