package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.StatChange;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/** The OTHER_CLASSES slot: focus on the other class (no change) or revise ICS. */
final class OtherClassesScreen extends Screen {
    @FXML private ChoiceList choices;
    @FXML private VBox notice;
    @FXML private Hud hud;

    OtherClassesScreen(AppContext ctx) { super(ctx); }

    @Override
    protected Parent build() {
        StackPane root = Views.load("other-classes.fxml", this);
        root.getChildren().add(0, new Backdrop(
                new Backdrop.Glyph("PERIOD 2", 620, 250, 170, true),
                new Backdrop.Glyph("/* not ICS4U */", 90, 560, 60, false)));
        return root;
    }

    @Override
    protected boolean allowsPause() { return true; }

    @Override
    protected void focus() { choices.requestFocus(); }

    @Override
    protected void onShown() {
        hud.show(ctx.session().view(), ctx.demoLabel());
        Fetch<StatChange> revision = ctx.session().revisionEffect();
        String revisionHint = revision.ok()
                ? StatDelta.of(revision.value(), 1).stream().map(StatDelta::text).collect(Collectors.joining("  "))
                : "Values pending";
        choices.setChoices(List.of(
                ChoiceList.Choice.of("FOCUS ON CLASS", "No stat change", () -> act(false)),
                ChoiceList.Choice.of("REVISE ICS", revisionHint, () -> act(true)).tone("cyan")));
    }

    private void act(boolean reviseIcs) {
        Outcome outcome = ctx.session().otherClasses(reviseIcs);
        if (outcome instanceof Outcome.Done done) {
            ctx.toasts().outcome(done);
            ctx.advance();
        } else {
            notice(notice, Diagnostic.of(outcome));
            choices.unlock();
        }
    }
}
