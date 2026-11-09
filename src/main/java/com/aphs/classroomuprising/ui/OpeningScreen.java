package com.aphs.classroomuprising.ui;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The previous-semester Final, shown as a short scene. There is no battle yet: this screen
 * only presents the story and then calls {@code completeOpening()} once.
 */
final class OpeningScreen extends Screen {
    /** Placeholder story lines (T11/T14 content is not authored yet); labelled on screen. */
    private static final String SCRIPT = String.join("\n",
            "Mr. Reward: Final exam. Last question. Show me what you learned this semester.",
            "You: I... don't know this one.",
            "Mr. Reward: Then I'll see you again this summer.",
            "Narrator: You failed ICS4U. The retake starts on Day 1.");

    @FXML private StackPane portraitHost;
    @FXML private StackPane studentHost;
    @FXML private DialogueBox dialogue;
    @FXML private ChoiceList actions;
    @FXML private VBox notice;

    OpeningScreen(AppContext ctx) { super(ctx); }

    @Override
    protected Parent build() {
        StackPane root = Views.load("opening.fxml", this);
        root.getChildren().add(0, new Backdrop(
                new Backdrop.Glyph("FINAL EXAM", 330, 120, 150, true),
                new Backdrop.Glyph("return grade;", 60, 520, 64, false)));
        portraitHost.getChildren().add(Portraits.reward(600));
        studentHost.getChildren().add(Portraits.student(360));
        notice(notice, Diagnostic.info("info: placeholder scene",
                "The opening battle against Mr. Reward is not built yet; its scripted defeat is told as text."));
        root.setOnKeyPressed(e -> {
            if ((e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) && !dialogue.isFinished()) {
                dialogue.advance();
                e.consume();
            }
        });
        return root;
    }

    @Override
    protected void onShown() {
        dialogue.play(ctx.script(SCRIPT), () -> {
            actions.setChoices(List.of(ChoiceList.Choice.of("BEGIN THE RETAKE", "Day 1 of 40", this::complete)));
            actions.requestFocus();
        });
    }

    private void complete() {
        Outcome outcome = ctx.session().completeOpening();
        if (outcome.ok()) {
            ctx.startDay(outcome);
        } else {
            notice(notice, Diagnostic.of(outcome));
            actions.unlock();
        }
    }
}
