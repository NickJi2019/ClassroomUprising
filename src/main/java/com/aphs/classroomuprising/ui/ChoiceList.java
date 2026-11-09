package com.aphs.classroomuprising.ui;

import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The game's one menu control: a staggered list of slanted options, driven by mouse or
 * keyboard (Up/Down or W/S, Enter or Space).
 *
 * <p>Click protection: after one option is activated the list locks, so double clicks and
 * key repeats cannot trigger a second action. The screen calls {@link #unlock()} only when
 * the action was refused and the same options remain valid.</p>
 */
public class ChoiceList extends VBox {
    public record Choice(String label, String hint, String tone, boolean enabled, Runnable action) {
        public static Choice of(String label, String hint, Runnable action) {
            return new Choice(label, hint, "normal", true, action);
        }
        public Choice tone(String value) { return new Choice(label, hint, value, enabled, action); }
        public Choice enabled(boolean value) { return new Choice(label, hint, tone, value, action); }
    }

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass UNAVAILABLE = PseudoClass.getPseudoClass("unavailable");

    private final List<Choice> choices = new ArrayList<>();
    private final List<Slant> rows = new ArrayList<>();
    private int selected = -1;
    private boolean locked;
    private double stagger = 22;
    private IntConsumer onSelect = index -> {};

    public ChoiceList() {
        getStyleClass().add("choice-list");
        setFocusTraversable(true);
        setSpacing(10);
        addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
    }

    public double getStagger() { return stagger; }

    /** Horizontal step between rows, giving the diagonal list shape. */
    public void setStagger(double stagger) { this.stagger = stagger; }

    public void setOnSelect(IntConsumer listener) { onSelect = listener; }

    public void setChoices(List<Choice> list) {
        getChildren().clear();
        choices.clear();
        rows.clear();
        selected = -1;
        locked = false;
        boolean rightAligned = getAlignment().getHpos() == HPos.RIGHT;
        for (int i = 0; i < list.size(); i++) {
            Choice choice = list.get(i);
            Label label = new Label(choice.label());
            label.getStyleClass().addAll("display", "choice-label");
            VBox text = new VBox(label);
            text.getStyleClass().add("choice-text");
            if (choice.hint() != null && !choice.hint().isBlank()) {
                Label hint = new Label(choice.hint());
                hint.getStyleClass().add("choice-hint");
                text.getChildren().add(hint);
            }
            Slant row = new Slant(text);
            row.getStyleClass().addAll("choice", "tone-" + choice.tone());
            row.pseudoClassStateChanged(UNAVAILABLE, !choice.enabled());
            row.setMaxWidth(USE_PREF_SIZE);
            double step = i * stagger;
            VBox.setMargin(row, rightAligned ? new Insets(0, step, 0, 0) : new Insets(0, 0, 0, step));
            int index = i;
            row.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> select(index));
            row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                select(index);
                activate();
            });
            getChildren().add(row);
            choices.add(choice);
            rows.add(row);
            Motion.slideIn(row, 40 + i * 45);
        }
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).enabled()) { select(i); break; }
        }
    }

    public void unlock() { locked = false; }

    public boolean isLocked() { return locked; }

    private void select(int index) {
        if (index < 0 || index >= rows.size() || index == selected) return;
        if (selected >= 0) rows.get(selected).pseudoClassStateChanged(SELECTED, false);
        selected = index;
        rows.get(index).pseudoClassStateChanged(SELECTED, true);
        onSelect.accept(index);
    }

    private void move(int direction) {
        if (rows.isEmpty()) return;
        int index = selected;
        for (int step = 0; step < rows.size(); step++) {
            index = Math.floorMod(index + direction, rows.size());
            if (choices.get(index).enabled()) { select(index); return; }
        }
    }

    private void activate() {
        if (locked || selected < 0 || !choices.get(selected).enabled()) return;
        locked = true;
        Motion.pop(rows.get(selected));
        choices.get(selected).action().run();
    }

    private void onKey(KeyEvent event) {
        switch (event.getCode()) {
            case UP, W -> move(-1);
            case DOWN, S -> move(1);
            case ENTER, SPACE -> activate();
            default -> { return; }
        }
        event.consume();
    }
}
