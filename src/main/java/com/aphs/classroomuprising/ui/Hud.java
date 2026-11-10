package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.GameState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The always-visible layer: date block, time-of-day strip and three stat chips.
 * Everything else (calendar, grades, details) lives in the ESC menu.
 */
public class Hud extends AnchorPane {
    private static final List<DayPeriod> SLOTS =
            List.of(DayPeriod.ICS_CLASS, DayPeriod.OTHER_CLASSES, DayPeriod.AFTER_SCHOOL, DayPeriod.EVENING, DayPeriod.LATE_NIGHT);

    private final Label day = new Label();
    private final Label course = new Label();
    private String courseTitle;
    private GameState shown;
    private final Label period = new Label();
    private final HBox pips = new HBox(5);
    private final Label hp = new Label();
    private final Label san = new Label();
    private final Label vibe = new Label();
    private final Label demo = new Label();
    private final KeyHints hints = new KeyHints();

    public Hud() {
        getStyleClass().add("hud");
        setPickOnBounds(false);

        // A soft gradient behind the HUD keeps it readable over any background art.
        javafx.scene.layout.Region scrim = new javafx.scene.layout.Region();
        scrim.getStyleClass().add("scrim-top");
        scrim.setMouseTransparent(true);
        scrim.setPrefHeight(170);
        AnchorPane.setLeftAnchor(scrim, 0.0);
        AnchorPane.setRightAnchor(scrim, 0.0);
        AnchorPane.setTopAnchor(scrim, 0.0);
        getChildren().add(scrim);

        day.getStyleClass().addAll("display", "hud-day-label");
        Slant dayTag = new Slant(day);
        dayTag.getStyleClass().addAll("tag-caret", "hud-day");
        dayTag.setMaxWidth(USE_PREF_SIZE);
        course.getStyleClass().addAll("display", "hud-course");
        // Ends before the tip card (which starts at x 340); a longer authored title ends in an ellipsis.
        course.setMaxWidth(300);
        period.getStyleClass().addAll("display", "hud-period");
        pips.setAlignment(Pos.CENTER_LEFT);
        HBox timeRow = new HBox(10, pips, period);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        VBox date = new VBox(4, dayTag, course, timeRow);
        AnchorPane.setLeftAnchor(date, 36.0);
        AnchorPane.setTopAnchor(date, 26.0);

        HBox stats = new HBox(12, chip("HP", "stat-hp", hp), chip("SAN", "stat-san", san), chip("VIBE", "stat-vibe", vibe));
        AnchorPane.setRightAnchor(stats, 36.0);
        AnchorPane.setTopAnchor(stats, 30.0);

        demo.getStyleClass().addAll("mono", "demo-label");
        AnchorPane.setLeftAnchor(demo, 24.0);
        AnchorPane.setBottomAnchor(demo, 8.0);

        hints.set(KeyHints.Hint.of("ESC", "MENU"));
        AnchorPane.setRightAnchor(hints, 18.0);
        AnchorPane.setBottomAnchor(hints, 4.0);

        getChildren().addAll(date, stats, demo, hints);
    }

    /** The key hints in the bottom-right corner, for what the screen offers right now. */
    void setHints(KeyHints.Hint... list) { hints.set(list); }

    /** Hides the key hints while something on top shows its own (a slide shown large). */
    void hideHints(boolean hidden) { hints.setOpacity(hidden ? 0 : 1); }

    /** An authored title for today's course (e.g. a lesson title), shown in the course line; null for none. */
    public void setCourseTitle(String title) {
        courseTitle = title;
        if (shown != null) show(shown, demo.getText());
    }

    public void show(GameState state, String demoLabel) {
        shown = state;
        day.setText("DAY %02d".formatted(state.day()));
        course.setText(UiText.courseLine(state.currentCourse(), courseTitle) + "  ·  %d DAYS LEFT".formatted(40 - state.day()));
        period.setText(UiText.period(state.period()));
        hp.setText(StatDelta.number(state.student().hp()));
        san.setText(StatDelta.number(state.effectiveSan()));
        vibe.setText(StatDelta.number(state.student().codingVibe()));
        demo.setText(demoLabel);

        pips.getChildren().clear();
        if (state.period() == DayPeriod.MORNING_BONUS) pips.getChildren().add(pip("pip-now", "pip-half"));
        for (DayPeriod slot : SLOTS) {
            String status = slot == state.period() ? "pip-now"
                    : slot.ordinal() < state.period().ordinal() ? "pip-done"
                    : slot == DayPeriod.LATE_NIGHT ? "pip-risk" : "pip-later";
            pips.getChildren().add(pip(status, null));
        }
    }

    private static Slant pip(String status, String extra) {
        Slant pip = new Slant();
        pip.getStyleClass().addAll("pip", status);
        if (extra != null) pip.getStyleClass().add(extra);
        return pip;
    }

    private static Slant chip(String name, String tone, Label value) {
        Label label = new Label(name);
        label.getStyleClass().addAll("display", "stat-name", tone);
        value.getStyleClass().addAll("display", "stat-value");
        HBox content = new HBox(7, label, value);
        content.setAlignment(Pos.BASELINE_LEFT);
        Slant chip = new Slant(content);
        chip.getStyleClass().add("stat-chip");
        return chip;
    }
}
