package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.TimedEffect;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** The day card: day number, today's course, and anything that applies today. */
final class DayIntroScreen extends Screen {
    private final Outcome.Done started;
    private boolean leaving;

    @FXML private Label dayNumber;
    @FXML private Label course;
    @FXML private Label tutorial;
    @FXML private VBox notes;
    @FXML private Label hint;

    /** @param started the outcome of starting this day, or null for Day 1 */
    DayIntroScreen(AppContext ctx, Outcome.Done started) {
        super(ctx);
        this.started = started;
    }

    @Override
    protected Parent build() {
        StackPane root = Views.load("day-intro.fxml", this);
        root.getChildren().add(0, new Backdrop(
                new Backdrop.Glyph("day++;", 760, 90, 110, false),
                new Backdrop.Glyph("while (day <= 40)", 40, 560, 64, false)));
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
        dayNumber.setText("%02d".formatted(view.day()));
        course.setText(UiText.courseLine(view.currentCourse()));
        tutorial.setVisible(view.isTutorial());
        tutorial.setManaged(view.isTutorial());

        notes.getChildren().clear();
        if (view.period() == DayPeriod.MORNING_BONUS)
            notes.getChildren().add(Diagnostic.info("Slept early: a ½ bonus slot before class",
                    "Activities in the bonus slot have half their effect."));
        for (TimedEffect effect : view.effects()) {
            if (!effect.activeOn(view.day())) continue;
            notes.getChildren().add(Diagnostic.warning(UiText.effect(effect) + " — today",
                    "SAN " + StatDelta.signed(effect.sanDelta()) + "  ·  XP gain ×" + StatDelta.number(effect.xpMultiplier())));
        }
        if (started != null) {
            ctx.toasts().outcome(started);
        }
        if (notes.getChildren().isEmpty()) {
            PauseTransition auto = new PauseTransition(Duration.seconds(2.4));
            auto.setOnFinished(e -> leave());
            auto.play();
        }
        hint.setText(notes.getChildren().isEmpty() ? "" : "ENTER  CONTINUE");
    }

    private void leave() {
        if (leaving) return;
        leaving = true;
        ctx.advance();
    }
}
