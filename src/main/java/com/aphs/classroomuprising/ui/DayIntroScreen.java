package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.TimedEffect;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The day card. The previous date flips over to today's while the calendar strip scrolls
 * one day forward; then today's course and anything that applies today slide in.
 * Enter, Space or a click skips ahead at any point.
 */
final class DayIntroScreen extends Screen {
    private final Outcome.Done started;
    private final int previousDay;
    private Animation intro;
    private PauseTransition autoLeave;
    private boolean leaving;

    @FXML private com.aphs.classroomuprising.ui.scene.SceneView scene;
    @FXML private FlipNumber dayNumber;
    @FXML private CalendarStrip strip;
    @FXML private Label course;
    @FXML private Label tutorial;
    @FXML private VBox notes;
    @FXML private Label hint;

    /**
     * @param started     the outcome of starting this day, or null for Day 1
     * @param previousDay the day shown before the flip (0 before Day 1)
     */
    DayIntroScreen(AppContext ctx, Outcome.Done started, int previousDay) {
        super(ctx);
        this.started = started;
        this.previousDay = previousDay;
    }

    @Override
    protected Parent build() {
        StackPane root = Views.load("day-intro.fxml", this);
        GameState view = ctx.session().view();
        boolean morningInDorm = view.period() == DayPeriod.MORNING_BONUS;
        ctx.stage(scene, morningInDorm ? "dorm" : "classroom", null, view.period().name().toLowerCase(java.util.Locale.ROOT));
        scene.setDim(0.6);
        root.getChildren().add(1, new Backdrop(
                new Backdrop.Glyph("day++;", 760, 90, 110, false),
                new Backdrop.Glyph("while (day <= 40)", 40, 600, 64, false)));
        root.setOnMouseClicked(e -> leave());
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER, SPACE -> leave();
                default -> { }
            }
        });
        return root;
    }

    @Override
    protected void onShown() {
        GameState view = ctx.session().view();
        course.setText(UiText.courseLine(view.currentCourse()));
        tutorial.setVisible(view.isTutorial());
        tutorial.setManaged(view.isTutorial());
        fillNotes(view);
        hint.setText(notes.getChildren().isEmpty() ? "" : "ENTER  CONTINUE");
        for (Node detail : details()) detail.setOpacity(0);

        dayNumber.setText("%02d".formatted(previousDay));
        int from = Math.max(1, previousDay);
        intro = new SequentialTransition(
                new PauseTransition(Duration.millis(260)),
                new ParallelTransition(dayNumber.flipTo("%02d".formatted(view.day())), strip.advance(from, view.day())));
        intro.setOnFinished(e -> reveal());
        intro.play();
    }

    @Override
    protected void onHidden() {
        if (intro != null) intro.stop();
        if (autoLeave != null) autoLeave.stop();
    }

    private void reveal() {
        double delay = 0;
        for (Node detail : details()) {
            Motion.slideIn(detail, delay);
            delay += 70;
        }
        if (started != null) ctx.toasts().outcome(started);
        if (notes.getChildren().isEmpty()) {
            autoLeave = new PauseTransition(Duration.seconds(1.8));
            autoLeave.setOnFinished(e -> leave());
            autoLeave.play();
        }
    }

    private void fillNotes(GameState view) {
        notes.getChildren().clear();
        if (view.period() == DayPeriod.MORNING_BONUS)
            notes.getChildren().add(Diagnostic.info("Slept early: a ½ bonus slot before class",
                    "Activities in the bonus slot have half their effect."));
        for (TimedEffect effect : view.effects()) {
            if (!effect.activeOn(view.day())) continue;
            notes.getChildren().add(Diagnostic.warning(UiText.effect(effect) + " — today",
                    "SAN " + StatDelta.signed(effect.sanDelta()) + "  ·  XP gain ×" + StatDelta.number(effect.xpMultiplier())));
        }
    }

    private Node[] details() { return new Node[] {course, tutorial, notes, hint}; }

    private void leave() {
        if (leaving) return;
        leaving = true;
        ctx.advance();
    }
}
