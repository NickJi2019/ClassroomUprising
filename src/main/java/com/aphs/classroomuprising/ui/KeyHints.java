package com.aphs.classroomuprising.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * The key hints in the bottom-right corner: each key is a slanted caret-yellow key cap followed
 * by what it does, on a dark plate so it reads over any background. The same control is used by
 * every screen, so the hints look and read the same everywhere.
 */
public class KeyHints extends Slant {
    /**
     * One hint: a key ("ESC", "HOLD ENTER") and what it does. Without a key it is plain text:
     * a warning ({@link #note}, in red) or information ({@link #info}).
     */
    record Hint(String key, String action, boolean warning) {
        static Hint of(String key, String action) { return new Hint(key, action, false); }
        static Hint note(String text) { return new Hint(null, text, true); }
        static Hint info(String text) { return new Hint(null, text, false); }
    }

    private final HBox row = new HBox(18);
    private List<Hint> shown = List.of();

    public KeyHints() {
        getStyleClass().add("key-hints");
        row.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(row);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMouseTransparent(true);
    }

    /** Replaces the hints; none hides the plate. */
    void set(Hint... hints) { set(List.of(hints)); }

    void set(List<Hint> hints) {
        if (hints.equals(shown)) return;
        shown = List.copyOf(hints);
        row.getChildren().clear();
        for (Hint hint : hints) {
            HBox item = new HBox(7);
            item.setAlignment(Pos.CENTER_LEFT);
            if (hint.key() != null) {
                Label key = new Label(hint.key());
                key.getStyleClass().addAll("display", "key-cap-label");
                Slant cap = new Slant(key);
                cap.getStyleClass().add("key-cap");
                cap.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
                item.getChildren().add(cap);
            }
            Label action = new Label(hint.action());
            action.getStyleClass().addAll("display",
                    hint.key() != null ? "key-action" : hint.warning() ? "key-note" : "key-info");
            item.getChildren().add(action);
            row.getChildren().add(item);
        }
        setVisible(!hints.isEmpty());
    }

    /** The texts shown, for checks. */
    List<String> texts() {
        return row.getChildren().stream().map(node -> ((HBox) node).getChildren().stream()
                .map(child -> child instanceof Label l ? l.getText() : ((Label) ((Slant) child).getChildren().get(1)).getText())
                .reduce((a, b) -> "[" + a + "] " + b).orElse("")).toList();
    }
}
