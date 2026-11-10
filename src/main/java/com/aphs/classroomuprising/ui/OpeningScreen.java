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
    static final String SCRIPT = String.join("\n",
            "Mr. Reward[gotcha]: Final exam. Last question. Show me what you learned this semester.",
            "You[worried]: I... don't know this one.",
            "Mr. Reward[ridicule]: Then I'll see you again this summer.",
            "Narrator: You failed ICS4U. The retake starts on Day 1.");

    @FXML private SceneView scene;
    @FXML private DialogueBox dialogue;
    @FXML private ChoiceList actions;
    @FXML private VBox notice;
    private DialogueLog log;
    private final KeyHints keys = new KeyHints();

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
        log = new DialogueLog(ctx.assets());
        StackPane.setAlignment(keys, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(keys, new javafx.geometry.Insets(0, 18, 4, 0));
        root.getChildren().addAll(keys, log);
        dialogue.attachKeys(root);
        // Hold to fast-forward; a held key or button never presses BEGIN (see FastForward).
        if (ctx.router() != null) dialogue.followFastForward(ctx.router().fastForward());
        dialogue.setAdvanceAllowed(() -> !log.isOpen());
        dialogue.setOnHistory(this::openLog);
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.H) {
                openLog();
                e.consume();
            }
        });
        return root;
    }

    @Override
    protected void onShown() {
        if (ctx.assets() != null) {
            scene.enter("student", 0.2, "worried", Duration.millis(150));
            scene.enter("reward", 0.74, "gotcha", Duration.millis(320));
            titleCard();
        }
        keys.set(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"), KeyHints.Hint.of("H", "LOG"));
        dialogue.play(ctx.script(SCRIPT), () -> {
            keys.set(KeyHints.Hint.of("H", "LOG"));
            actions.setChoices(List.of(ChoiceList.Choice.of("BEGIN THE RETAKE", "Day 1 of 40", this::complete)));
            actions.requestFocus();
        });
    }

    /** Mr. Reward's hand-drawn title card on a red band as he steps in; skipped when the art is missing. */
    private void titleCard() {
        var logo = ctx.assets().ui("reward-logo");
        if (logo.isEmpty()) return;
        javafx.scene.image.ImageView mark = new javafx.scene.image.ImageView(logo.get());
        mark.setPreserveRatio(true);
        mark.setFitHeight(150);
        mark.setSmooth(true);
        javafx.scene.control.Label sub = new javafx.scene.control.Label("ICS4U  //  FINAL EXAM");
        sub.getStyleClass().addAll("display", "cutin-sub", "tone-danger");
        VBox card = new VBox(-8, mark, sub);
        javafx.scene.layout.Pane layer = new javafx.scene.layout.Pane();
        layer.setMouseTransparent(true);
        StackPane root = (StackPane) root();
        root.getChildren().add(root.getChildren().indexOf(log), layer);
        javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(Duration.millis(520));
        wait.setOnFinished(e -> CutIn.play(layer, 390, card, "danger", 200, Duration.millis(1500))
                .setOnFinished(done -> root.getChildren().remove(layer)));
        wait.play();
    }

    /** Esc: closes the log (its large slide first). There is no menu before the game starts. */
    @Override
    protected boolean back() { return log != null && log.back(); }

    @Override
    protected boolean isPlaying() { return !dialogue.isFinished() && (log == null || !log.isOpen()); }

    @Override
    protected void focus() {
        if (log != null && log.isOpen()) log.requestFocus();
        else if (actions.hasChoices()) actions.requestFocus();
        else root().requestFocus();
    }

    private void openLog() {
        if (dialogue.history().isEmpty() || log.isOpen()) return;
        ctx.router().hideTips(true);
        log.open(dialogue.history(), () -> {
            ctx.router().hideTips(false);
            focus();
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
