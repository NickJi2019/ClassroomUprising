package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Background decoration: large, low-contrast code glyphs at the slant angle, drifting slowly.
 * It is the game's equivalent of an ambient motif and never receives input.
 */
public class Backdrop extends Pane {
    /** One decorative glyph: position in the 1280x720 canvas, font size, display or mono face. */
    record Glyph(String text, double x, double y, double size, boolean display) {}

    public Backdrop(Glyph... glyphs) {
        getStyleClass().add("backdrop");
        setMouseTransparent(true);
        for (int i = 0; i < glyphs.length; i++) {
            Glyph glyph = glyphs[i];
            Text text = new Text(glyph.text());
            text.getStyleClass().addAll(glyph.display() ? "display" : "mono", "glyph");
            text.setStyle("-fx-font-size: " + glyph.size() + "px;");
            text.setRotate(-14);
            text.relocate(glyph.x(), glyph.y());
            getChildren().add(text);

            TranslateTransition drift = new TranslateTransition(Duration.seconds(16 + i * 5), text);
            drift.setByX(i % 2 == 0 ? 26 : -26);
            drift.setByY(-14);
            drift.setAutoReverse(true);
            drift.setCycleCount(Animation.INDEFINITE);
            drift.setInterpolator(Interpolator.EASE_BOTH);
            drift.play();
        }
    }
}
