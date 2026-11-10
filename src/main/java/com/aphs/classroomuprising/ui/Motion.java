package com.aphs.classroomuprising.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** The small, shared set of animations, so every screen moves the same way. */
final class Motion {
    private Motion() {}

    /** Enter from the right along the slant direction. */
    static void slideIn(Node node, double delayMillis) {
        node.setOpacity(0);
        node.setTranslateX(48);
        node.setTranslateY(-12);
        TranslateTransition move = new TranslateTransition(MotionTokens.BASE, node);
        move.setToX(0);
        move.setToY(0);
        move.setInterpolator(MotionTokens.OUT);
        FadeTransition fade = new FadeTransition(MotionTokens.QUICK, node);
        fade.setToValue(1);
        ParallelTransition both = new ParallelTransition(move, fade);
        both.setDelay(Duration.millis(delayMillis));
        both.play();
    }

    /** A quick press feedback on the activated option. */
    static void pop(Node node) {
        ScaleTransition scale = new ScaleTransition(MotionTokens.SNAP, node);
        scale.setToX(1.05);
        scale.setToY(1.05);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    static void fadeIn(Node node, double millis) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(millis), node);
        fade.setToValue(1);
        fade.play();
    }
}
