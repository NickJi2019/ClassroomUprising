package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The Esc menu's background: the screen the player paused on, captured once, then drawn
 * through a slow wave distortion (updated in steps, not smoothly), desaturated, darkened and
 * blurred, under an indigo wash. The menu reads as a layer of the scene, not a curtain over it.
 */
final class MenuBackdrop extends Pane {
    private static final double W = ClassroomApp.WIDTH, H = ClassroomApp.HEIGHT;
    private static final int MAP_W = 128, MAP_H = 72;
    private static final int WAVE_STEPS_PER_SECOND = 20;
    /** Wave phase advance per second (radians). */
    private static final double WAVE_SPEED = 3.6;

    private final Timeline wave;

    MenuBackdrop(Image capture) {
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);
        setMouseTransparent(true);
        getStyleClass().add("menu-backdrop");

        setClip(new Rectangle(W, H));
        FloatMap map = new FloatMap(MAP_W, MAP_H);
        DisplacementMap distort = new DisplacementMap(map);
        distort.setWrap(true);
        ColorAdjust colour = new ColorAdjust(0, -0.6, -0.08, 0.15);
        colour.setInput(distort);
        GaussianBlur blur = new GaussianBlur(7);
        blur.setInput(colour);

        // Slightly oversized so the wave and blur edges stay outside the canvas.
        ImageView view = new ImageView(capture);
        view.setFitWidth(W * 1.04);
        view.setFitHeight(H * 1.04);
        view.relocate(-W * 0.02, -H * 0.02);
        view.setEffect(blur);
        Rectangle wash = new Rectangle(W, H);
        wash.getStyleClass().add("menu-backdrop-wash");
        wash.setBlendMode(BlendMode.MULTIPLY);
        Rectangle glow = new Rectangle(W, H);
        glow.getStyleClass().add("menu-backdrop-glow");
        glow.setBlendMode(BlendMode.SCREEN);
        getChildren().addAll(view, wash, glow);

        int[] tick = {0};
        fill(map, 0);
        wave = new Timeline(new KeyFrame(Duration.millis(1000.0 / WAVE_STEPS_PER_SECOND), e -> {
            fill(map, ++tick[0]);
            distort.setMapData(map);
        }));
        wave.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((obs, old, now) -> {
            if (now == null) wave.stop();
            else wave.play();
        });
    }

    /** Horizontal ripples: each row is shifted by a sine of its height and the step count. */
    private static void fill(FloatMap map, int step) {
        double phase = step * WAVE_SPEED / WAVE_STEPS_PER_SECOND;
        for (int y = 0; y < MAP_H; y++) {
            float dx = (float) (Math.sin(y / (double) MAP_H * Math.PI * 6 + phase) * 0.0045);
            for (int x = 0; x < MAP_W; x++) map.setSamples(x, y, dx, 0f);
        }
    }
}
