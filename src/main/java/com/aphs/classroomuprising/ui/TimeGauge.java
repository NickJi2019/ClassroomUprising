package com.aphs.classroomuprising.ui;

import javafx.css.PseudoClass;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

/**
 * The question clock as a slanted bar: it fills over the 30-second soft limit, with a tick
 * every five seconds. Past the limit it stays full and turns red ({@code :overtime}).
 * It draws what it is given; the elapsed time and the overtime flag come from the backend.
 */
public class TimeGauge extends Region {
    private static final PseudoClass OVERTIME = PseudoClass.getPseudoClass("overtime");
    private static final PseudoClass LATE = PseudoClass.getPseudoClass("late");
    private static final int TICKS = 6;

    private final Polygon track = new Polygon();
    private final Polygon fill = new Polygon();
    private final Line[] ticks = new Line[TICKS - 1];
    private double fraction;

    public TimeGauge() {
        getStyleClass().add("time-gauge");
        track.getStyleClass().add("gauge-track");
        fill.getStyleClass().add("gauge-fill");
        getChildren().addAll(track, fill);
        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = new Line();
            ticks[i].getStyleClass().add("gauge-tick");
            getChildren().add(ticks[i]);
        }
        setMinHeight(14);
        setPrefHeight(14);
    }

    /**
     * @param fraction elapsed / limit (values above 1 draw as full)
     * @param overtime the backend's timed-out flag
     */
    public void show(double fraction, boolean overtime) {
        this.fraction = Math.max(0, Math.min(1, fraction));
        pseudoClassStateChanged(OVERTIME, overtime);
        pseudoClassStateChanged(LATE, !overtime && this.fraction >= 2.0 / 3);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth(), h = getHeight(), lean = h * Slant.SLOPE;
        track.getPoints().setAll(lean, 0.0, w, 0.0, w - lean, h, 0.0, h);
        double fw = Math.max(0, (w - lean) * fraction);
        fill.getPoints().setAll(lean, 0.0, lean + fw, 0.0, fw, h, 0.0, h);
        for (int i = 0; i < ticks.length; i++) {
            double x = (w - lean) * (i + 1) / TICKS;
            ticks[i].setStartX(x + lean);
            ticks[i].setStartY(0);
            ticks[i].setEndX(x);
            ticks[i].setEndY(h);
        }
    }
}
