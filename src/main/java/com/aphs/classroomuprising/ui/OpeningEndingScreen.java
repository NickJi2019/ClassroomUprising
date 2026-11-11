package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.OpeningBattle;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * After last semester's exam, out of the battle: the screen the exam's ending moves to through
 * the game's usual screen change. The grade is stamped in the corner and the authored ending is
 * told, with Mr. Reward still mocking the player after a defeat (or astonished after a win).
 * There are no stats here: the battle is over. Only once the story has been told may the player
 * go on: BEGIN SUMMER SCHOOL acknowledges the opening ({@code completeOpening}) and Day 1 starts.
 * It reads the finished exam from the core and changes nothing else.
 */
final class OpeningEndingScreen extends Screen {
    @FXML private SceneView scene;
    @FXML private Label heading;
    @FXML private VBox notice;
    @FXML private ChoiceList actions;
    @FXML private DialogueBox dialogue;
    @FXML private Hud hud;
    @FXML private Pane effects;
    private DialogueLog log;
    private boolean done;

    OpeningEndingScreen(AppContext ctx) { super(ctx); }

    @Override
    protected String music() { return "opening"; }

    @Override
    protected Parent build() {
        StackPane root = Views.load("opening-ending.fxml", this);
        ctx.stage(scene, "opening", null, "opening");
        root.getChildren().add(1, new Backdrop(
                new Backdrop.Glyph("FINAL GRADE", 330, 120, 150, true),
                new Backdrop.Glyph("return grade;", 60, 520, 64, false)));
        dialogue.setOnLine(line -> scene.speak(line.characterId(), line.expression()));
        log = new DialogueLog(ctx.assets());
        root.getChildren().add(log);
        dialogue.attachKeys(root);
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
        hud.showWithoutStats(ctx.demoLabel());
        Fetch<OpeningBattle.View> exam = ctx.session().openingBattle();
        if (!exam.ok() || !exam.value().finished()) {
            // Nothing to tell (the exam is not over): only the acknowledgement, which the core checks.
            offerDayOne();
            return;
        }
        OpeningBattle.Ending ending = exam.value().ending();
        boolean won = ending.animation() == OpeningBattle.AnimationCue.VICTORY;
        heading.setText(won ? "ALL TEN CORRECT  //  MR. REWARD" : "DEFEATED  //  MR. REWARD");
        if (ctx.assets() != null) {
            scene.enter("student", 0.2, "worried", Duration.millis(150));
            scene.enter("reward", 0.74, won ? OpeningScreen.ASTONISHED : OpeningScreen.MOCKS, Duration.millis(260));
        }
        PauseTransition stamp = new PauseTransition(Duration.millis(380));
        stamp.setOnFinished(e -> stampGrade(ending));
        stamp.play();
        hints();
        dialogue.play(ctx.script(String.join("\n", ending.dialogue())), this::offerDayOne);
        root().requestFocus();
    }

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

    /** The grade, stamped in the corner, stays while the story is told. */
    private void stampGrade(OpeningBattle.Ending ending) {
        Label word = new Label("FINAL GRADE");
        word.getStyleClass().addAll("display", "grade-word");
        Label number = new Label(Integer.toString(ending.grade()));
        number.getStyleClass().addAll("display", "grade-number");
        VBox content = new VBox(-10, word, number);
        content.setAlignment(Pos.CENTER_LEFT);
        Slant stamp = new Slant(content);
        stamp.getStyleClass().addAll("grade-stamp",
                ending.animation() == OpeningBattle.AnimationCue.VICTORY ? "tag-caret" : "tag-danger");
        stamp.setLayoutX(40);
        stamp.setLayoutY(96);
        stamp.setRotate(-6);
        effects.getChildren().add(stamp);
        stamp.setScaleX(2.2);
        stamp.setScaleY(2.2);
        stamp.setOpacity(0);
        Timeline land = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(stamp.scaleXProperty(), 1, MotionTokens.OUT),
                new KeyValue(stamp.scaleYProperty(), 1, MotionTokens.OUT),
                new KeyValue(stamp.opacityProperty(), 1)));
        land.setOnFinished(e -> Sfx.HIT.play(effects));
        land.play();
    }

    /** Only after the story: on to Day 1, whichever ending it was. */
    private void offerDayOne() {
        actions.setChoices(List.of(ChoiceList.Choice.of("BEGIN SUMMER SCHOOL", "Day 1 of 40", this::complete)));
        hints();
        actions.requestFocus();
    }

    private void complete() {
        if (done) return;
        Outcome outcome = ctx.session().completeOpening();
        if (outcome.ok()) {
            done = true;
            ctx.startDay(outcome);
        } else {
            notice(notice, Diagnostic.of(outcome));
            actions.unlock();
        }
    }

    private void hints() {
        if (actions.hasChoices()) hud.setHints(KeyHints.Hint.of("H", "LOG"));
        else hud.setHints(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"), KeyHints.Hint.of("H", "LOG"));
    }

    private void openLog() {
        if (dialogue.history().isEmpty() || log.isOpen()) return;
        ctx.router().hideTips(true);
        log.open(dialogue.history(), () -> {
            ctx.router().hideTips(false);
            focus();
        });
    }
}
