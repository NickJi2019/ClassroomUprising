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
 * The always-visible layer: date block, time-of-day strip and three stat chips: HP as a
 * number, SAN as a bar, and one XP total (Coding Vibe and knowledge together) that opens a
 * panel with its parts when clicked; Esc or another click closes it. Everything else
 * (calendar, grades, details) lives in the ESC menu.
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
    private final SanBar san = new SanBar(150, 16);
    private final Label xp = new Label();
    private final Label xpCaret = new Label("▾");
    private final XpBreakdown xpParts = new XpBreakdown();
    private boolean xpOpen;
    private final Label demo = new Label();
    private final KeyHints hints = new KeyHints();
    private VBox date;
    private HBox stats;
    private Slant hpChip;
    /** Edges of the screen pulsing red while HP is low. */
    private final javafx.scene.layout.Region lowHp = new javafx.scene.layout.Region();
    private final javafx.animation.Timeline lowHpPulse;
    /** The whole screen flashing red when HP drops. */
    private final javafx.scene.layout.Region hurtFlash = new javafx.scene.layout.Region();
    /** The HP last shown, to notice a drop (null until the first show). */
    private Double shownHp;
    private Runnable onHurt = () -> {};
    /** The hints the screen asked for; while the XP panel is open the plate says how to close it. */
    private KeyHints.Hint[] screenHints = {KeyHints.Hint.of("ESC", "MENU")};

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
        date = new VBox(4, dayTag, course, timeRow);
        AnchorPane.setLeftAnchor(date, 36.0);
        AnchorPane.setTopAnchor(date, 26.0);

        Slant xpChip = chip("XP", "stat-xp", xp);
        xpCaret.getStyleClass().addAll("display", "xp-caret");
        ((HBox) xpChip.getChildren().get(1)).getChildren().add(xpCaret);
        xpChip.getStyleClass().add("xp-chip");
        xpChip.setCursor(javafx.scene.Cursor.HAND);
        xpChip.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) toggleXp();
        });
        hpChip = chip("HP", "stat-hp", hp);
        stats = new HBox(12, hpChip, chip("SAN", "stat-san", san), xpChip);
        stats.setAlignment(Pos.CENTER_RIGHT);
        AnchorPane.setRightAnchor(stats, 36.0);
        AnchorPane.setTopAnchor(stats, 30.0);

        xpParts.getStyleClass().add("xp-panel");
        xpParts.setVisible(false);
        xpParts.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> toggleXp());
        AnchorPane.setRightAnchor(xpParts, 36.0);
        AnchorPane.setTopAnchor(xpParts, 82.0);

        demo.getStyleClass().addAll("mono", "demo-label");
        AnchorPane.setLeftAnchor(demo, 24.0);
        AnchorPane.setBottomAnchor(demo, 8.0);

        hints.set(KeyHints.Hint.of("ESC", "MENU"));
        AnchorPane.setRightAnchor(hints, 18.0);
        AnchorPane.setBottomAnchor(hints, 4.0);

        for (javafx.scene.layout.Region layer : List.of(lowHp, hurtFlash)) {
            layer.setMouseTransparent(true);
            layer.setOpacity(0);
            AnchorPane.setLeftAnchor(layer, 0.0);
            AnchorPane.setRightAnchor(layer, 0.0);
            AnchorPane.setTopAnchor(layer, 0.0);
            AnchorPane.setBottomAnchor(layer, 0.0);
        }
        // Full-screen gradients: drawn once, then only faded (cheap every frame).
        lowHp.setCache(true);
        hurtFlash.setCache(true);
        lowHp.getStyleClass().add("low-hp-vignette");
        hurtFlash.getStyleClass().add("hurt-flash");
        lowHpPulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(lowHp.opacityProperty(), 0.25)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(520),
                        new javafx.animation.KeyValue(lowHp.opacityProperty(), 0.85, javafx.animation.Interpolator.EASE_OUT)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(1150),
                        new javafx.animation.KeyValue(lowHp.opacityProperty(), 0.25, javafx.animation.Interpolator.EASE_IN)));
        lowHpPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);

        // The edge pulse and the hit flash lie under the stats, so the numbers stay readable.
        getChildren().addAll(lowHp, hurtFlash, date, stats, xpParts, demo, hints);
    }

    /** Called when HP drops, so the screen can shake the player's character. */
    void setOnHurt(Runnable action) { onHurt = action; }

    /** Whether the low-HP edge pulse is running (for checks). */
    boolean lowHpWarning() { return lowHpPulse.getStatus() == javafx.animation.Animation.Status.RUNNING; }

    /** Whether the SAN bar is twitching (for checks). */
    boolean sanDistressed() { return san.distressed(); }

    /**
     * The status effects for the HP and SAN now shown: a drop in HP flashes the screen red, jolts
     * the HP chip and tells the screen ({@link #setOnHurt}); low HP pulses the edges red; very low
     * SAN makes the SAN bar twitch. They only show what the core already changed.
     */
    private void effects(double hpNow, double sanNow) {
        if (shownHp != null && hpNow < shownHp - 1e-9) hurt();
        shownHp = hpNow;
        boolean low = hpNow < Stats.LOW_HP;
        if (low && lowHpPulse.getStatus() != javafx.animation.Animation.Status.RUNNING) lowHpPulse.play();
        if (!low && lowHpPulse.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            lowHpPulse.stop();
            lowHp.setOpacity(0);
        }
        san.setDistressed(sanNow < Stats.LOW_SAN);
    }

    private void hurt() {
        Sfx.HIT.play(this);
        new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(hurtFlash.opacityProperty(), 0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(40), new javafx.animation.KeyValue(hurtFlash.opacityProperty(), 0.55)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(380),
                        new javafx.animation.KeyValue(hurtFlash.opacityProperty(), 0, MotionTokens.OUT))).play();
        javafx.animation.Timeline jolt = new javafx.animation.Timeline();
        double[] xs = {-12, 9, -6, 3, 0};
        for (int i = 0; i < xs.length; i++)
            jolt.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(45 * (i + 1)),
                    new javafx.animation.KeyValue(hpChip.translateXProperty(), xs[i])));
        jolt.play();
        onHurt.run();
    }

    /** The key hints in the bottom-right corner, for what the screen offers right now. */
    void setHints(KeyHints.Hint... list) {
        screenHints = list.clone();
        if (!xpOpen) hints.set(list);
    }

    /** Opens or closes the panel with the parts of the XP total. */
    void toggleXp() {
        xpOpen = !xpOpen;
        xpParts.setVisible(xpOpen);
        xpCaret.setText(xpOpen ? "▴" : "▾");
        // While open the HUD is drawn above the screen's own layers (a lecture slide), so the panel is never hidden.
        setViewOrder(xpOpen ? -1 : 0);
        if (xpOpen) {
            Motion.slideIn(xpParts, 0);
            hints.set(KeyHints.Hint.of("ESC", "CLOSE XP"));
        } else {
            hints.set(screenHints);
        }
        (xpOpen ? Sfx.MENU_OPEN : Sfx.MENU_CLOSE).play(this);
    }

    /** Esc: closes the XP panel if it is open (true), otherwise does nothing (false). */
    boolean collapse() {
        if (!xpOpen) return false;
        toggleXp();
        return true;
    }

    boolean xpOpen() { return xpOpen; }

    /** Hides the key hints while something on top shows its own (a slide shown large). */
    void hideHints(boolean hidden) { hints.setOpacity(hidden ? 0 : 1); }

    /** An authored title for today's course (e.g. a lesson title), shown in the course line; null for none. */
    public void setCourseTitle(String title) {
        courseTitle = title;
        if (shown != null) show(shown, demo.getText());
    }

    /**
     * The opening exam's own stats (last semester's student), with no date or time of day: the
     * prologue is not a school day. HP has no maximum, so it stays a plain number.
     */
    void showStudent(com.aphs.classroomuprising.StudentState student, String demoLabel) {
        date.setVisible(false);
        hp.setText(StatDelta.number(student.hp()));
        san.set(Stats.sanFraction(student.san()));
        xp.setText(StatDelta.number(Stats.xp(student)));
        xpParts.show(student);
        demo.setText(demoLabel);
        effects(student.hp(), student.san());
    }

    /** Hides or shows the three stat chips (the opening's story has none yet). */
    void setStatsVisible(boolean visible) {
        if (stats.isVisible() == visible) return;
        stats.setVisible(visible);
        if (visible) Motion.slideIn(stats, 0);
        else collapse();
    }

    public void show(GameState state, String demoLabel) {
        shown = state;
        date.setVisible(true);
        day.setText("DAY %02d".formatted(state.day()));
        course.setText(UiText.courseLine(state.currentCourse(), courseTitle) + "  ·  %d DAYS LEFT".formatted(40 - state.day()));
        period.setText(UiText.period(state.period()));
        hp.setText(StatDelta.number(state.student().hp()));
        san.set(Stats.sanFraction(state.effectiveSan()));
        xp.setText(StatDelta.number(Stats.xp(state.student())));
        xpParts.show(state.student());
        effects(state.student().hp(), state.effectiveSan());
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

    private static Slant chip(String name, String tone, javafx.scene.Node value) {
        Label label = new Label(name);
        label.getStyleClass().addAll("display", "stat-name", tone);
        value.getStyleClass().addAll("display", "stat-value");
        HBox content = new HBox(7, label, value);
        content.setAlignment(value instanceof Label ? Pos.BASELINE_LEFT : Pos.CENTER_LEFT);
        Slant chip = new Slant(content);
        chip.getStyleClass().add("stat-chip");
        return chip;
    }
}
