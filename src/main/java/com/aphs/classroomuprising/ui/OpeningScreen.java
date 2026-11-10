package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * The previous-semester Final, shown as a short scene. There is no battle yet: this screen
 * only presents the story and then calls {@code completeOpening()} once.
 */
final class OpeningScreen extends Screen {
    /** Placeholder story lines (T11/T14 content is not authored yet); labelled on screen. */
    private static final String SCRIPT = String.join("\n",
            "Mr. Reward[stern]: Final exam. Last question. Show me what you learned this semester.",
            "You[worried]: I... don't know this one.",
            "Mr. Reward[smile]: Then I'll see you again this summer.",
            "Narrator: You failed ICS4U. The retake starts on Day 1.");

    @FXML private SceneView scene;
    @FXML private DialogueBox dialogue;
    @FXML private ChoiceList actions;
    @FXML private VBox notice;

    OpeningScreen(AppContext ctx) { super(ctx); }

    @Override
    protected String music() { return "opening"; }

    @Override
    protected Parent build() {
        StackPane root = Views.load("opening.fxml", this);
        ctx.stage(scene, "opening", null, "opening");
        root.getChildren().add(1, new Backdrop(
                new Backdrop.Glyph("FINAL EXAM", 330, 120, 150, true),
                new Backdrop.Glyph("return grade;", 60, 520, 64, false)));
        notice(notice, Diagnostic.info("info: placeholder scene",
                "The opening battle against Mr. Reward is not built yet; its scripted defeat is told as text."));
        dialogue.setOnLine(line -> scene.speak(line.characterId(), line.expression()));
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
        if (ctx.assets() != null) {
            scene.enter("student", 0.2, "worried", Duration.millis(150));
            scene.enter("reward", 0.74, "stern", Duration.millis(320));
        }
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
