package com.aphs.classroomuprising.ui;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Music and effects volume, shown in the Esc menu and on the title screen. Up/down choose a
 * row, left/right change it one step, Enter or Backspace (or Esc where nothing else uses it)
 * hands control back. A level can also be clicked. Changes apply and are kept at once.
 */
final class SoundPanel extends VBox {
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass ON = PseudoClass.getPseudoClass("on");

    private record Row(HBox box, List<Region> cells, Label value, IntSupplier level, IntConsumer set) {}

    private final AudioDirector audio;
    private final List<Row> rows;
    private int selected;
    private Runnable onBack = () -> {};

    SoundPanel(AudioDirector audio) {
        super(18);
        this.audio = audio;
        getStyleClass().add("sound-panel");
        setFocusTraversable(true);
        rows = List.of(
                row("MUSIC", () -> audio.settings().music(), audio::setMusic),
                row("EFFECTS", () -> audio.settings().effects(), level -> {
                    audio.setEffects(level);
                    Sfx.CURSOR.play(this);
                }));
        Label keys = new Label("↑↓  CHOOSE     ←→  ADJUST     ESC  BACK");
        keys.getStyleClass().addAll("mono", "sound-keys");
        rows.forEach(r -> getChildren().add(r.box()));
        getChildren().add(keys);
        addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        focusedProperty().addListener((obs, was, now) -> refresh());
        refresh();
    }

    /** Called when the player hands control back (Enter, Backspace or Esc). */
    void setOnBack(Runnable action) { onBack = action; }

    private Row row(String name, IntSupplier level, IntConsumer set) {
        Label label = new Label(name);
        label.getStyleClass().addAll("display", "sound-name");
        HBox meter = new HBox(5);
        meter.setAlignment(Pos.CENTER_LEFT);
        var cells = new java.util.ArrayList<Region>();
        for (int i = 1; i <= AudioSettings.MAX; i++) {
            Region cell = new Region();
            cell.getStyleClass().add("sound-cell");
            int value = i;
            cell.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                set.accept(level.getAsInt() == value ? value - 1 : value);
                refresh();
            });
            cells.add(cell);
        }
        meter.getChildren().addAll(cells);
        Label value = new Label();
        value.getStyleClass().addAll("display", "sound-value");
        HBox box = new HBox(22, label, meter, value);
        box.getStyleClass().add("sound-row");
        box.setAlignment(Pos.CENTER_LEFT);
        Row row = new Row(box, List.copyOf(cells), value, level, set);
        box.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            selected = rows.indexOf(row);
            refresh();
        });
        return row;
    }

    private void onKey(KeyEvent event) {
        Row row = rows.get(selected);
        switch (event.getCode()) {
            case UP, W -> selected = Math.floorMod(selected - 1, rows.size());
            case DOWN, S -> selected = Math.floorMod(selected + 1, rows.size());
            case LEFT, A -> row.set().accept(row.level().getAsInt() - 1);
            case RIGHT, D -> row.set().accept(row.level().getAsInt() + 1);
            case ENTER, SPACE, BACK_SPACE, ESCAPE -> {
                event.consume();
                onBack.run();
                return;
            }
            default -> { return; }
        }
        event.consume();
        refresh();
    }

    private void refresh() {
        for (int r = 0; r < rows.size(); r++) {
            Row row = rows.get(r);
            int level = row.level().getAsInt();
            row.box().pseudoClassStateChanged(ACTIVE, isFocused() && r == selected);
            for (int i = 0; i < row.cells().size(); i++) row.cells().get(i).pseudoClassStateChanged(ON, i < level);
            row.value().setText(level == 0 ? "OFF" : Integer.toString(level));
        }
    }
}
