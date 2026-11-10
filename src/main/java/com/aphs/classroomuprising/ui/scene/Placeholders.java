package com.aphs.classroomuprising.ui.scene;

import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;

/**
 * Stand-in images for art that has not been delivered. They are deliberately plain and
 * labelled with the asset key and expected file, so the layout can be judged while nobody
 * mistakes them for final art. They go through the same layers and grades as real images.
 */
final class Placeholders {
    private static final Map<String, Image> CACHE = new HashMap<>();
    private static final Color LABEL = Color.web("#AEB6E0");
    private static final Color LINE = Color.web("#2A3366");

    private Placeholders() {}

    static Image background(String key, String file) {
        return CACHE.computeIfAbsent("bg|" + key, k -> {
            Canvas canvas = new Canvas(1280, 720);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#3A4270")), new Stop(1, Color.web("#151A38"))));
            g.fillRect(0, 0, 1280, 720);
            // A neutral grey-blue checker so grading and parallax can be judged on the plate.
            g.setFill(Color.web("#FFFFFF", 0.035));
            for (int y = 0; y < 720; y += 80)
                for (int x = (y / 80) % 2 * 80; x < 1280; x += 160) g.fillRect(x, y, 80, 80);
            g.setStroke(LINE);
            g.setLineWidth(2);
            g.strokeRect(24, 24, 1232, 672);
            // Right side, mid-height: the plate is scaled about 1.1x to cover the canvas and drifts,
            // so labels stay well inside the edges and clear of the HUD, toasts and dialogue box.
            g.setTextAlign(TextAlignment.RIGHT);
            label(g, "MISSING ART", 1110, 440, 26, FontWeight.BOLD);
            label(g, key, 1110, 466, 15, FontWeight.NORMAL);
            label(g, "assets/" + file, 1110, 488, 15, FontWeight.NORMAL);
            return snapshot(canvas, Color.web("#151A38"));
        });
    }

    static Image character(AssetManifest.CharacterSpec spec, String expression) {
        return CACHE.computeIfAbsent("ch|" + spec.id() + "|" + expression, k -> {
            double h = spec.height();
            double w = Math.round(h * 0.55);
            Canvas canvas = new Canvas(w, h);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.web("#0B0F2A", 0.55));
            g.fillRoundRect(6, 6, w - 12, h - 12, 24, 24);
            g.setStroke(LABEL);
            g.setLineWidth(2);
            g.setLineDashes(10, 8);
            g.strokeRoundRect(6, 6, w - 12, h - 12, 24, 24);
            g.setLineDashes();
            // Anchor marker: the point that stands on the slot.
            double ax = w * spec.anchorX(), ay = Math.min(h - 8, h * spec.anchorY());
            g.strokeLine(ax - 14, ay - 4, ax + 14, ay - 4);
            g.strokeLine(ax, ay - 18, ax, ay - 4);
            g.setTextAlign(TextAlignment.CENTER);
            label(g, spec.name().toUpperCase(java.util.Locale.ROOT), w / 2, h * 0.42, 28, FontWeight.BOLD);
            label(g, "expression: " + expression, w / 2, h * 0.42 + 34, 15, FontWeight.NORMAL);
            label(g, "MISSING ART", w / 2, h * 0.42 + 62, 13, FontWeight.BOLD);
            label(g, spec.expressions().getOrDefault(expression, "?"), w / 2, h * 0.42 + 84, 12, FontWeight.NORMAL);
            return snapshot(canvas, Color.TRANSPARENT);
        });
    }

    /** A 16:9 slide plate: which lesson and line it belongs to, and the file to deliver. */
    static Image slide(AssetManifest.Slide slide) {
        return CACHE.computeIfAbsent("slide|" + slide.file(), k -> {
            Canvas canvas = new Canvas(1280, 720);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.web("#E9ECF7"));
            g.fillRect(0, 0, 1280, 720);
            g.setFill(Color.web("#0B0F2A", 0.05));
            for (int y = 0; y < 720; y += 60)
                for (int x = (y / 60) % 2 * 60; x < 1280; x += 120) g.fillRect(x, y, 60, 60);
            g.setStroke(Color.web("#5A6499"));
            g.setLineWidth(4);
            g.setLineDashes(18, 12);
            g.strokeRect(30, 30, 1220, 660);
            g.setLineDashes();
            g.setTextAlign(TextAlignment.CENTER);
            ink(g, "SLIDE  ·  DAY " + slide.day() + "  ·  FROM LINE " + slide.fromLine(), 640, 300, 44, FontWeight.BOLD);
            ink(g, "MISSING SLIDE", 640, 380, 30, FontWeight.BOLD);
            ink(g, "assets/" + slide.file(), 640, 430, 28, FontWeight.NORMAL);
            return snapshot(canvas, Color.web("#E9ECF7"));
        });
    }

    private static void ink(GraphicsContext g, String text, double x, double y, double size, FontWeight weight) {
        g.setFill(Color.web("#2A3366"));
        g.setFont(Font.font("Menlo", weight, size));
        g.setTextBaseline(VPos.BASELINE);
        g.fillText(text, x, y);
    }

    private static void label(GraphicsContext g, String text, double x, double y, double size, FontWeight weight) {
        g.setFill(LABEL);
        g.setFont(Font.font("Menlo", weight, size));
        g.setTextBaseline(VPos.BASELINE);
        g.fillText(text, x, y);
    }

    private static Image snapshot(Canvas canvas, Color fill) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(fill);
        return canvas.snapshot(parameters, null);
    }
}
