package com.aphs.classroomuprising.ui.scene;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Ambient life over the art: drifting light motes, film grain and a vignette.
 * Motes and grain move in fixed steps (a few updates per second) rather than smoothly,
 * which reads as hand-made texture instead of a slide.
 */
final class AmbientLayer extends Pane {
    private static final double W = BackgroundLayer.W, H = BackgroundLayer.H;
    /** Updates per second: high enough to read as motion, still discrete like film grain. */
    private static final int STEPS_PER_SECOND = 24;
    /** Motes rise this many canvas pixels per second and sway sideways. */
    private static final double MOTE_RISE = 12.8;

    private final Timeline steps;

    AmbientLayer(Optional<Image> motes, Optional<Image> grain) {
        setPrefSize(W, H);
        setMinSize(W, H);
        setMaxSize(W, H);
        setMouseTransparent(true);

        Rectangle moteRect = new Rectangle(W, H);
        moteRect.setBlendMode(BlendMode.ADD);
        moteRect.setOpacity(0.28);
        Rectangle grainRect = new Rectangle(W, H);
        grainRect.setBlendMode(BlendMode.OVERLAY);
        grainRect.setOpacity(0.16);
        Rectangle vignette = new Rectangle(W, H);
        vignette.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.web("#0B0F2A", 0)), new Stop(1, Color.web("#0B0F2A", 0.72))));

        motes.ifPresent(image -> moteRect.setFill(new ImagePattern(image, 0, 0, image.getWidth(), image.getHeight(), false)));
        grain.ifPresent(image -> grainRect.setFill(new ImagePattern(image, 0, 0, image.getWidth(), image.getHeight(), false)));
        if (motes.isPresent()) getChildren().add(moteRect);
        getChildren().add(vignette);
        if (grain.isPresent()) getChildren().add(grainRect);

        Random random = new Random(7);
        double[] tick = {0};
        steps = new Timeline(new KeyFrame(Duration.millis(1000.0 / STEPS_PER_SECOND), e -> {
            tick[0]++;
            double seconds = tick[0] / STEPS_PER_SECOND;
            grain.ifPresent(image -> grainRect.setFill(new ImagePattern(image,
                    random.nextInt((int) image.getWidth()), random.nextInt((int) image.getHeight()),
                    image.getWidth(), image.getHeight(), false)));
            motes.ifPresent(image -> moteRect.setFill(new ImagePattern(image,
                    Math.sin(seconds / 5.0) * 18, -seconds * MOTE_RISE, image.getWidth(), image.getHeight(), false)));
        }));
        steps.setCycleCount(Animation.INDEFINITE);
    }

    List<Animation> animations() { return List.of(steps); }
}
