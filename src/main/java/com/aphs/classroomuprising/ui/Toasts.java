package com.aphs.classroomuprising.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Short-lived feedback in the top-right corner: stat changes and core warnings. */
final class Toasts extends VBox {
    Toasts() {
        super(6);
        getStyleClass().add("toasts");
        setAlignment(Pos.TOP_RIGHT);
        setMouseTransparent(true);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    /** Shows what actually changed (from snapshots) and any warnings the core returned. */
    void outcome(Outcome.Done done) {
        for (StatDelta delta : StatDelta.between(done.before(), done.after()))
            show(delta.text(), delta.positive() ? "toast-up" : "toast-down");
        for (String warning : done.warnings()) show(warning, "toast-warn");
    }

    void show(String text, String tone) {
        Label label = new Label(text);
        label.getStyleClass().addAll(tone.equals("toast-warn") ? "mono" : "display", "toast-label");
        label.setWrapText(true);
        label.setMaxWidth(420);
        Slant toast = new Slant(label);
        toast.getStyleClass().addAll("toast", tone);
        toast.setMaxWidth(USE_PREF_SIZE);
        getChildren().add(toast);
        Motion.slideIn(toast, getChildren().size() * 60);

        FadeTransition fade = new FadeTransition(Duration.millis(400), toast);
        fade.setToValue(0);
        var life = new SequentialTransition(new PauseTransition(Duration.seconds(tone.equals("toast-warn") ? 5 : 3)), fade);
        life.setOnFinished(e -> getChildren().remove(toast));
        life.play();
    }
}
