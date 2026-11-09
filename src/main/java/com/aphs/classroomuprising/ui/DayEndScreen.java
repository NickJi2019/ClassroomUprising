package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.GameState;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The closed day. The day-end snapshot is written here (the only legal save point),
 * then the player starts the next day or returns to the title.
 */
final class DayEndScreen extends Screen {
    @FXML private Label dayLabel;
    @FXML private FlowPane summary;
    @FXML private Label saveStatus;
    @FXML private VBox notice;
    @FXML private ChoiceList choices;
    @FXML private Hud hud;

    DayEndScreen(AppContext ctx) { super(ctx); }

    @Override
    protected Parent build() {
        StackPane root = Views.load("day-end.fxml", this);
        root.getChildren().add(0, new Backdrop(
                new Backdrop.Glyph("save();", 700, 120, 120, false),
                new Backdrop.Glyph("zzz", 980, 470, 150, true)));
        return root;
    }

    @Override
    protected boolean allowsPause() { return true; }

    @Override
    protected void focus() { choices.requestFocus(); }

    @Override
    protected void onShown() {
        GameState view = ctx.session().view();
        hud.show(view, ctx.demoLabel());
        dayLabel.setText("DAY %02d".formatted(view.day()));
        summary.getChildren().clear();
        GameState start = ctx.dayStart();
        if (start != null && start.day() == view.day()) {
            for (StatDelta delta : StatDelta.between(start, view)) {
                Label chip = new Label(delta.text());
                chip.getStyleClass().addAll("display", "effect-chip", delta.positive() ? "effect-up" : "effect-down");
                summary.getChildren().add(chip);
            }
            if (summary.getChildren().isEmpty()) summary.getChildren().add(plain("No stat changes today."));
        } else {
            summary.getChildren().add(plain("Loaded from your day-end save."));
        }
        save();
    }

    private void save() {
        Outcome saved = ctx.session().saveDayEnd();
        notice(notice, saved.ok() ? null : Diagnostic.of(saved));
        saveStatus.setText(saved.ok() ? "saved  →  " + shortPath(ctx.session().savePath()) : "not saved");
        saveStatus.getStyleClass().removeAll("save-ok", "save-failed");
        saveStatus.getStyleClass().add(saved.ok() ? "save-ok" : "save-failed");

        var list = new ArrayList<ChoiceList.Choice>();
        if (!saved.ok()) list.add(ChoiceList.Choice.of("RETRY SAVE", null, this::save).tone("danger"));
        list.add(ChoiceList.Choice.of("NEXT DAY", "Sleep and start tomorrow", this::nextDay));
        list.add(ChoiceList.Choice.of("RETURN TO TITLE", null, ctx::toTitle).tone("cyan"));
        choices.setChoices(List.copyOf(list));
        choices.requestFocus();
    }

    private void nextDay() {
        Outcome outcome = ctx.session().beginNextDay();
        if (outcome.ok()) {
            ctx.startDay(outcome);
        } else {
            notice(notice, Diagnostic.of(outcome));
            choices.unlock();
        }
    }

    private static String shortPath(java.nio.file.Path path) {
        java.nio.file.Path home = java.nio.file.Path.of(System.getProperty("user.home")).toAbsolutePath();
        java.nio.file.Path absolute = path.toAbsolutePath();
        return absolute.startsWith(home) ? "~/" + home.relativize(absolute) : absolute.toString();
    }

    private static Label plain(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("place-sub");
        return label;
    }
}
