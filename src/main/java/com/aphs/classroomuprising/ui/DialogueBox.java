package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;

/**
 * Bottom-of-screen dialogue: a name tag, typewriter text and an advance marker.
 * {@link #advance()} first completes the line being typed, then moves to the next line.
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

        getChildren().addAll(text, tag, more);
        setOnMouseClicked(e -> advance());
        widthProperty().addListener((obs, a, b) -> fit());
    }

    /** Plays the lines in order and runs {@code whenDone} once after the last line is dismissed. */
    public void play(List<DialogueScript.Line> script, Runnable whenDone) {
        lines = List.copyOf(script);
        onFinished = whenDone;
        finished = false;
        index = -1;
        next();
    }

    /** Shows one line with no follow-up action. */
    public void say(String who, String line) {
        play(List.of(new DialogueScript.Line(who, line)), () -> {});
        finished = true;
    }

    public boolean isFinished() { return finished; }

    /** The line on screen, counted from 1 (0 before the first line). */
    int lineNumber() { return index + 1; }

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
