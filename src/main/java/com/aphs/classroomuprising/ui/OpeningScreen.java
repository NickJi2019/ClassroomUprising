package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.OpeningBattle;
import com.aphs.classroomuprising.OpeningContent;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The previous semester's ICS4U final, before summer school: the authored story, then the
 * exam, then one of its endings, then Day 1.
 *
 * <p>The exam belongs to the core ({@code OpeningBattle}): this screen only sends the player's
 * choices (which question, which option, the duck) and draws the view it reads back. The core
 * times every question, judges every answer, applies HP and decides the ending; nothing here
 * computes a deadline, a result or a grade. While the exam runs, a timer asks the core to settle
 * time ({@link GameSession#advanceOpeningBattle()}) every {@link #PULSE_NANOS} and redraws the
 * clock from the view. There is no pause: no menu opens over the exam, and the log is closed
 * while a question is on the clock.</p>
 *
 * <p>Order: pick any unanswered question (no clock while picking), answer it (A–E then Enter, or
 * a click) before its clock runs out, see at once whether it was right, then pick the next. The
 * duck (R or a click on it) can be used between questions or during one; while it works,
 * answering and picking wait. When the
 * core reports an ending, input stops, its animation and authored dialogue play once, and only
 * then may the player go on to Day 1.</p>
 */
final class OpeningScreen extends Screen {
    private enum Stage { INTRO, PICK, QUESTION, FEEDBACK, ENDING, DONE }

    /** Mr. Reward as he steps in, when he mocks a defeat, and when the player wins. */
    static final String ARRIVES = "gotcha", MOCKS = "ridicule", ASTONISHED = "staring";

    /** How often the core is asked to settle time (deadline, duck) while the exam runs. */
    static final long PULSE_NANOS = 100_000_000L;
    private static final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("highlight");
    private static final PseudoClass CHOSEN = PseudoClass.getPseudoClass("chosen");
    private static final PseudoClass RIGHT = PseudoClass.getPseudoClass("right");
    private static final PseudoClass WRONG = PseudoClass.getPseudoClass("wrong");

    @FXML private SceneView scene;
    @FXML private DialogueBox dialogue;
    @FXML private ChoiceList actions;
    @FXML private VBox notice;
    @FXML private Hud hud;
    @FXML private Region examScrim;
    @FXML private VBox picker;
    @FXML private Label progress;
    @FXML private Label nextClock;
    @FXML private ChoiceList questions;
    @FXML private VBox paper;
    @FXML private Label questionTag;
    @FXML private Label verdict;
    @FXML private Label time;
    @FXML private Label limit;
    @FXML private TimeGauge gauge;
    @FXML private Label prompt;
    @FXML private ScrollPane codeScroll;
    @FXML private Label code;
    @FXML private ScrollPane optionScroll;
    @FXML private VBox options;
    @FXML private Label explanation;
    @FXML private Slant duck;
    @FXML private Label duckLabel;
    @FXML private Region blackout;
    @FXML private Pane effects;

    private DialogueLog log;
    private Stage stage = Stage.INTRO;
    /** The latest view read from the core. */
    private OpeningBattle.View last;
    /** The question on the paper (kept after answering, to show the feedback beside it). */
    private OpeningContent.QuestionView shown;
    private final List<HBox> rows = new ArrayList<>();
    private int highlighted = -1;
    /** An answer was just shown: the ending waits a moment so it can be read. */
    private boolean feedbackJustShown;
    private boolean endingPlayed;
    private long lastPulse;
    private final AnimationTimer clock = new AnimationTimer() {
        @Override
        public void handle(long now) { pulse(now); }
    };

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
        dialogue.setOnLine(line -> scene.speak(line.characterId(), line.expression()));
        log = new DialogueLog(ctx.assets());
        root.getChildren().add(log);
        dialogue.attachKeys(root);
        // Hold to fast-forward the story; a held key or button never presses anything that appears after it.
        if (ctx.router() != null) dialogue.followFastForward(ctx.router().fastForward());
        dialogue.setAdvanceAllowed(() -> !log.isOpen());
        dialogue.setOnHistory(this::openLog);
        root.setOnKeyPressed(e -> {
            if (key(e.getCode())) e.consume();
        });
        paper.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (stage == Stage.FEEDBACK && e.getButton() == MouseButton.PRIMARY) showPicker();
        });
        duck.setCursor(javafx.scene.Cursor.HAND);
        duck.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) useDuck();
        });
        // The code box fills its area when the code is short and scrolls when it is long.
        code.minWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> codeScroll.getViewportBounds().getWidth(), codeScroll.viewportBoundsProperty()));
        code.minHeightProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> codeScroll.getViewportBounds().getHeight(), codeScroll.viewportBoundsProperty()));
        code.setAlignment(Pos.TOP_LEFT);
        return root;
    }

    @Override
    protected void onShown() {
        if (ctx.assets() != null) {
            scene.enter("student", 0.2, "worried", Duration.millis(150));
            scene.enter("reward", 0.74, ARRIVES, Duration.millis(320));
        }
        hud.setStatsVisible(false);
        if (!ctx.session().hasOpeningExam()) {
            // A legacy fixture catalogue without the exam: nothing to fight, only the acknowledgement.
            stage = Stage.ENDING;
            offerDayOne();
            return;
        }
        hud.showStudent(ctx.session().openingContent().initialStudent(), ctx.demoLabel());
        Fetch<OpeningBattle.View> running = ctx.session().openingBattle();
        if (running.ok()) {
            resume(running.value());
            return;
        }
        if (ctx.assets() != null) titleCard();
        stage = Stage.INTRO;
        hints();
        dialogue.play(ctx.script(String.join("\n", ctx.session().openingIntro())), this::startExam);
    }

    @Override
    protected void onHidden() {
        clock.stop();
        stage = Stage.DONE;
    }

    /** Esc: closes the log (its large slide first). There is no menu before the game starts. */
    @Override
    protected boolean back() { return log != null && log.back(); }

    @Override
    protected boolean isPlaying() {
        return (stage == Stage.INTRO || stage == Stage.ENDING) && !dialogue.isFinished() && (log == null || !log.isOpen());
    }

    @Override
    protected void focus() {
        if (log != null && log.isOpen()) log.requestFocus();
        else if (actions.hasChoices()) actions.requestFocus();
        else if (stage == Stage.PICK && questions.hasChoices()) questions.requestFocus();
        else root().requestFocus();
    }

    // ---- the exam ---------------------------------------------------------------------

    /** After the story: the core creates the exam (no clock yet) and the player picks a question. */
    private void startExam() {
        if (stage != Stage.INTRO) return;
        Outcome started = ctx.session().startOpeningBattle();
        if (!started.ok()) {
            notice(notice, Diagnostic.of(started));
            return;
        }
        read();
        hud.showStudent(last.student(), ctx.demoLabel());
        hud.setStatsVisible(true);
        duck.setVisible(true);
        Motion.slideIn(duck, 0);
        showPicker();
        lastPulse = 0;
        clock.start();
    }

    /** Back into a running exam (the screen was rebuilt): the core's view says where it stands. */
    private void resume(OpeningBattle.View view) {
        last = view;
        hud.showStudent(view.student(), ctx.demoLabel());
        hud.setStatsVisible(true);
        duck.setVisible(true);
        dialogue.setVisible(false);
        if (view.finished()) {
            beginEnding();
            return;
        }
        if (view.phase() == OpeningBattle.Phase.QUESTION) {
            view.availableQuestions().stream().filter(q -> q.id().equals(view.activeQuestionId())).findFirst()
                    .ifPresent(q -> showPaper(q, view));
        } else {
            showPicker();
        }
        clock.start();
    }

    /** Every frame while the exam runs: settle time now and then, read the view, redraw the clock. */
    private void pulse(long now) {
        if (stage == Stage.ENDING || stage == Stage.DONE || stage == Stage.INTRO || last == null) return;
        if (now - lastPulse >= PULSE_NANOS) {
            lastPulse = now;
            ctx.session().advanceOpeningBattle();
        }
        OpeningBattle.View before = last;
        OpeningBattle.View view = read();
        if (view.student().hp() > before.student().hp()) recovered(before, view);
        if (view.finished()) {
            beginEnding();
            return;
        }
        drawClock(view);
        drawDuck(view);
        if (before.usingDuck() != view.usingDuck() || before.ducksRemaining() != view.ducksRemaining()) {
            setInputEnabled(!view.usingDuck());
            hints();
        }
    }

    private OpeningBattle.View read() {
        Fetch<OpeningBattle.View> fetch = ctx.session().openingBattle();
        if (fetch.ok()) last = fetch.value();
        return last;
    }

    /** Between questions: the unanswered ones, in any order. No clock runs while picking. */
    private void showPicker() {
        if (last == null || last.finished()) return;
        stage = Stage.PICK;
        shown = null;
        rows.clear();
        paper.setVisible(false);
        paper.setManaged(false);
        dialogue.setVisible(false);
        examScrim.setVisible(true);
        notice(notice, null);
        int answered = last.answers().size();
        progress.setText("QUESTION " + (answered + 1) + " / " + (answered + last.availableQuestions().size())
                + "  ·  CHOOSE ANY");
        nextClock.setText("CLOCK " + seconds(last.timeLimit()) + " s  ·  STARTS WHEN YOU PICK");
        questions.setChoices(last.availableQuestions().stream()
                .map(q -> ChoiceList.Choice.of(excerpt(q.prompt()), null, () -> start(q.id()))).toList());
        picker.setVisible(true);
        picker.setManaged(true);
        Motion.slideIn(picker, 0);
        setInputEnabled(!last.usingDuck());
        drawDuck(last);
        hints();
        questions.requestFocus();
    }

    private void start(String questionId) {
        if (stage != Stage.PICK) return;
        Outcome outcome = ctx.session().startOpeningQuestion(questionId);
        if (!outcome.ok()) {
            notice(notice, Diagnostic.of(outcome));
            questions.unlock();
            return;
        }
        OpeningBattle.View view = read();
        view.availableQuestions().stream().filter(q -> q.id().equals(questionId)).findFirst()
                .ifPresent(q -> showPaper(q, view));
    }

    /** The question on the clock: its prompt and complete code, and its five options. */
    private void showPaper(OpeningContent.QuestionView question, OpeningBattle.View view) {
        stage = Stage.QUESTION;
        shown = question;
        picker.setVisible(false);
        picker.setManaged(false);
        questions.setChoices(List.of());
        examScrim.setVisible(true);
        notice(notice, null);
        questionTag.setText("QUESTION " + view.questionOrdinal() + " / 10");
        verdict.setText("");
        verdict.getStyleClass().removeAll("verdict-right", "verdict-wrong");
        limit.setText("/ " + seconds(view.timeLimit()) + " s");
        time.getStyleClass().remove("overtime");
        prompt.setText(question.prompt());
        code.setText(question.code());
        codeScroll.setVvalue(0);
        codeScroll.setHvalue(0);
        rows.clear();
        options.getChildren().clear();
        for (int i = 0; i < question.options().size(); i++) options.getChildren().add(row(question.options().get(i), i));
        optionScroll.setVvalue(0);
        highlighted = -1;
        explanation.setVisible(false);
        explanation.setManaged(false);
        paper.setVisible(true);
        paper.setManaged(true);
        Motion.slideIn(paper, 0);
        drawClock(view);
        setInputEnabled(!view.usingDuck());
        hints();
        root().requestFocus();
    }

    private HBox row(OpeningContent.Option option, int index) {
        Label letter = new Label(option.id());
        letter.getStyleClass().addAll("display", "exam-option-letter");
        Slant tag = new Slant(letter);
        tag.getStyleClass().add("exam-option-tag");
        tag.setMinWidth(Region.USE_PREF_SIZE);
        Label text = new Label(option.text());   // newlines kept: some options are printed grids
        text.getStyleClass().addAll("mono", "exam-option-text");
        text.setWrapText(!option.text().contains("\n"));
        HBox row = new HBox(12, tag, text);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("exam-option");
        row.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { if (stage == Stage.QUESTION) highlight(index); });
        row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && stage == Stage.QUESTION) answer(index);
        });
        rows.add(row);
        return row;
    }

    private void highlight(int index) {
        if (index < 0 || index >= rows.size() || index == highlighted) return;
        if (highlighted >= 0) rows.get(highlighted).pseudoClassStateChanged(HIGHLIGHT, false);
        highlighted = index;
        rows.get(index).pseudoClassStateChanged(HIGHLIGHT, true);
        Sfx.CURSOR.play(paper);
        HBox row = rows.get(index);
        double top = row.getBoundsInParent().getMinY(), bottom = row.getBoundsInParent().getMaxY();
        double content = options.getHeight(), viewport = optionScroll.getViewportBounds().getHeight();
        if (content > viewport) {
            double y = optionScroll.getVvalue() * (content - viewport);
            if (top < y) optionScroll.setVvalue(top / (content - viewport));
            else if (bottom > y + viewport) optionScroll.setVvalue((bottom - viewport) / (content - viewport));
        }
    }

    /** Sends the answer; the core judges it, takes the HP and may end the exam. */
    private void answer(int index) {
        if (stage != Stage.QUESTION || shown == null || last == null || last.usingDuck()) return;
        if (index < 0 || index >= shown.options().size()) return;
        int answered = last.answers().size();
        double hpBefore = last.student().hp();
        Outcome outcome = ctx.session().answerOpeningQuestion(shown.id(), shown.options().get(index).id());
        if (!outcome.ok()) {
            notice(notice, Diagnostic.of(outcome));
            return;
        }
        OpeningBattle.View view = read();
        hud.showStudent(view.student(), ctx.demoLabel());
        if (view.answers().size() > answered) {
            double change = view.student().hp() - hpBefore;
            showFeedback(view.answers().getLast(), change);
            if (change != 0) popHp(change);
        }
        if (view.finished()) {
            beginEnding();
            return;
        }
        stage = Stage.FEEDBACK;
        hints();
    }

    /** Right or wrong, at once: the chosen and the correct option marked, and why. */
    private void showFeedback(OpeningBattle.Feedback feedback, double hpChange) {
        feedbackJustShown = true;
        for (int i = 0; i < rows.size(); i++) {
            String id = shown.options().get(i).id();
            HBox row = rows.get(i);
            row.pseudoClassStateChanged(HIGHLIGHT, false);
            row.pseudoClassStateChanged(CHOSEN, id.equals(feedback.selectedOption()));
            row.pseudoClassStateChanged(RIGHT, id.equals(feedback.correctOption()));
            row.pseudoClassStateChanged(WRONG, id.equals(feedback.selectedOption()) && !feedback.correct());
        }
        highlighted = -1;
        verdict.setText((feedback.correct() ? "CORRECT" : "WRONG  ·  ANSWER " + feedback.correctOption())
                + (hpChange != 0 ? "  ·  HP " + StatDelta.signed(hpChange) : ""));
        verdict.getStyleClass().add(feedback.correct() ? "verdict-right" : "verdict-wrong");
        explanation.setText(feedback.explanation());
        explanation.setVisible(true);
        explanation.setManaged(true);
        Motion.slideIn(verdict, 0);
        (feedback.correct() ? Sfx.CONFIRM : Sfx.HIT).play(paper);
        time.setText("—");
    }

    /** Uses the one duck: its HP arrives later, on the core's clock; the question clock keeps running. */
    private void useDuck() {
        if (stage != Stage.PICK && stage != Stage.QUESTION && stage != Stage.FEEDBACK) return;
        if (last == null || last.ducksRemaining() == 0 || last.usingDuck()) return;
        Outcome outcome = ctx.session().useOpeningDuck();
        if (!outcome.ok()) {
            notice(notice, Diagnostic.of(outcome));
            return;
        }
        OpeningBattle.View view = read();
        if (view.finished()) {
            beginEnding();
            return;
        }
        Sfx.CONFIRM.play(duck);
        CutIn.play(effects, 560, "QUACK", "MS TANG'S RUBBER DUCK  ·  HOLD ON", "cyan");
        drawDuck(view);
        setInputEnabled(false);
        hints();
    }

    private void recovered(OpeningBattle.View before, OpeningBattle.View after) {
        hud.showStudent(after.student(), ctx.demoLabel());
        popHp(after.student().hp() - before.student().hp());
        Sfx.CONFIRM.play(duck);
    }

    /**
     * An HP change pops up under the HUD's HP chip (not as a corner toast, which would cover the
     * question clock).
     */
    private void popHp(double change) {
        Label pop = new Label("HP " + StatDelta.signed(change));
        pop.getStyleClass().addAll("display", "hp-pop", change > 0 ? "hp-pop-up" : "hp-pop-down");
        long showing = effects.getChildren().stream().filter(n -> n.getStyleClass().contains("hp-pop")).count();
        pop.setLayoutX(852);
        pop.setLayoutY(70 + 26 * showing);   // a second change while the first is up goes under it
        effects.getChildren().add(pop);
        Timeline rise = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pop.translateYProperty(), -6), new KeyValue(pop.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(140), new KeyValue(pop.translateYProperty(), 0, MotionTokens.OUT),
                        new KeyValue(pop.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(1300), new KeyValue(pop.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(1700), new KeyValue(pop.opacityProperty(), 0)));
        rise.setOnFinished(e -> effects.getChildren().remove(pop));
        rise.play();
    }

    private void drawClock(OpeningBattle.View view) {
        if (stage != Stage.QUESTION || view.phase() != OpeningBattle.Phase.QUESTION) return;
        double limitSeconds = view.timeLimit().toNanos() / 1e9;
        double left = view.remaining().toNanos() / 1e9;
        time.setText(seconds(view.remaining()));
        gauge.show(limitSeconds <= 0 ? 1 : 1 - left / limitSeconds, false);
    }

    private void drawDuck(OpeningBattle.View view) {
        duck.getStyleClass().removeAll("duck-ready", "duck-working", "duck-used");
        if (view.usingDuck()) {
            duck.getStyleClass().add("duck-working");
            duckLabel.setText("WORKING  " + seconds(view.duckRemaining()) + " s");
        } else if (view.ducksRemaining() > 0) {
            duck.getStyleClass().add("duck-ready");
            OpeningContent content = ctx.session().openingContent();
            duckLabel.setText("×" + view.ducksRemaining() + "  ·  HP " + StatDelta.signed(content.duckRecovery())
                    + " AFTER " + seconds(content.duckDuration()) + " s");
        } else {
            duck.getStyleClass().add("duck-used");
            duckLabel.setText("USED");
        }
    }

    /** While the duck works, nothing can be answered or started. */
    private void setInputEnabled(boolean enabled) {
        questions.setDisable(!enabled);
        options.setDisable(!enabled);
        options.setOpacity(enabled ? 1 : 0.45);
    }

    // ---- the ending -------------------------------------------------------------------

    /** The core reported an ending: input stops; its animation and dialogue play once. */
    private void beginEnding() {
        if (endingPlayed) return;
        endingPlayed = true;
        stage = Stage.ENDING;
        clock.stop();
        setInputEnabled(false);
        questions.setChoices(List.of());
        hints();
        OpeningBattle.Ending ending = last.ending();
        if (ending.reason() == OpeningBattle.DefeatReason.TIMEOUT) {
            time.setText("0.0");
            time.getStyleClass().add("overtime");
            gauge.show(1, true);
        }
        PauseTransition lead = new PauseTransition(Duration.millis(feedbackJustShown ? 1400 : 500));
        lead.setOnFinished(e -> { if (stage == Stage.ENDING) playEnding(ending); });
        lead.play();
    }

    private void playEnding(OpeningBattle.Ending ending) {
        double hold;
        switch (ending.animation()) {
            case VICTORY -> {
                scene.speak("reward", ASTONISHED);   // he did not expect that
                CutIn.play(effects, 300, "ALL TEN CORRECT", "MR. REWARD DID NOT SEE THAT COMING", "caret");
                hold = 1700;
            }
            case PASS_OUT -> {
                Sfx.HIT.play(effects);
                Timeline faint = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(blackout.opacityProperty(), 0)),
                        new KeyFrame(Duration.millis(900), new KeyValue(blackout.opacityProperty(), 0.93, MotionTokens.steps(6))));
                faint.setOnFinished(e -> CutIn.play(effects, 300, "PASSED OUT", "HP 0  ·  THE PAGE WENT DARK", "danger"));
                faint.play();
                hold = 2700;
            }
            default -> {
                Sfx.HIT.play(effects);
                scene.speak("reward", MOCKS);
                CutIn.play(effects, 300, "DEFEAT", reason(ending.reason()), "danger");
                hold = 1700;
            }
        }
        PauseTransition wait = new PauseTransition(Duration.millis(hold));
        wait.setOnFinished(e -> { if (stage == Stage.ENDING) endingStory(ending); });
        wait.play();
    }

    /** The grade stays on screen while the authored ending is told. */
    private void endingStory(OpeningBattle.Ending ending) {
        paper.setVisible(false);
        paper.setManaged(false);
        picker.setVisible(false);
        picker.setManaged(false);
        examScrim.setVisible(false);
        duck.setVisible(false);
        if (ending.animation() == OpeningBattle.AnimationCue.PASS_OUT) {
            scene.speak("reward", MOCKS);
            Timeline wake = new Timeline(new KeyFrame(Duration.millis(700), new KeyValue(blackout.opacityProperty(), 0.5)));
            wake.play();
        }
        stampGrade(ending);
        dialogue.setVisible(true);
        dialogue.play(ctx.script(String.join("\n", ending.dialogue())), this::offerDayOne);
        hints();
        root().requestFocus();
    }

    private void stampGrade(OpeningBattle.Ending ending) {
        Label word = new Label("FINAL GRADE");
        word.getStyleClass().addAll("display", "grade-word");
        Label number = new Label(Integer.toString(ending.grade()));
        number.getStyleClass().addAll("display", "grade-number");
        VBox content = new VBox(-10, word, number);
        content.setAlignment(Pos.CENTER_LEFT);
        Slant stamp = new Slant(content);
        stamp.getStyleClass().addAll("grade-stamp", ending.animation() == OpeningBattle.AnimationCue.VICTORY ? "tag-caret" : "tag-danger");
        stamp.setLayoutX(40);
        stamp.setLayoutY(96);
        stamp.setRotate(-6);
        effects.getChildren().add(stamp);
        stamp.setScaleX(2.2);
        stamp.setScaleY(2.2);
        stamp.setOpacity(0);
        Timeline land = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(stamp.scaleXProperty(), 1, MotionTokens.steps(4)),
                new KeyValue(stamp.scaleYProperty(), 1, MotionTokens.steps(4)),
                new KeyValue(stamp.opacityProperty(), 1)));
        land.setOnFinished(e -> Sfx.HIT.play(effects));
        land.play();
    }

    /** Only after the ending has been shown: on to Day 1, whichever ending it was. */
    private void offerDayOne() {
        actions.setChoices(List.of(ChoiceList.Choice.of("BEGIN SUMMER SCHOOL", "Day 1 of 40", this::complete)));
        hints();
        actions.requestFocus();
    }

    private void complete() {
        if (stage == Stage.DONE) return;
        Outcome outcome = ctx.session().completeOpening();
        if (outcome.ok()) {
            stage = Stage.DONE;
            ctx.startDay(outcome);
        } else {
            notice(notice, Diagnostic.of(outcome));
            actions.unlock();
        }
    }

    // ---- input, hints, story helpers --------------------------------------------------

    /** Keys for the exam; the lists handle their own arrows and Enter when they have focus. */
    private boolean key(KeyCode code) {
        if (code == KeyCode.H) {
            if (stage != Stage.QUESTION) openLog();
            return true;
        }
        if (code == KeyCode.R) {   // the rubber duck (D is an answer)
            useDuck();
            return true;
        }
        if (stage == Stage.FEEDBACK && (code == KeyCode.ENTER || code == KeyCode.SPACE)) {
            showPicker();
            return true;
        }
        if (stage != Stage.QUESTION || shown == null) return false;
        switch (code) {
            case A, B, C, D, E -> highlight(code.ordinal() - KeyCode.A.ordinal());
            case UP, W -> highlight(highlighted <= 0 ? 0 : highlighted - 1);
            case DOWN, S -> highlight(highlighted < 0 ? 0 : Math.min(rows.size() - 1, highlighted + 1));
            case ENTER, SPACE -> { if (highlighted >= 0) answer(highlighted); }
            default -> { return false; }
        }
        return true;
    }

    private void hints() {
        boolean duckReady = last != null && last.ducksRemaining() > 0 && !last.usingDuck();
        var list = new ArrayList<KeyHints.Hint>();
        switch (stage) {
            case INTRO -> {
                list.add(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"));
                list.add(KeyHints.Hint.of("H", "LOG"));
            }
            case PICK -> {
                list.add(KeyHints.Hint.of("↑↓", "CHOOSE"));
                list.add(KeyHints.Hint.of("ENTER", "START"));
                if (duckReady) list.add(KeyHints.Hint.of("R", "DUCK"));
                list.add(KeyHints.Hint.of("H", "LOG"));
            }
            case QUESTION -> {
                list.add(KeyHints.Hint.of("A–E", "CHOOSE"));
                list.add(KeyHints.Hint.of("ENTER", "ANSWER"));
                if (duckReady) list.add(KeyHints.Hint.of("R", "DUCK"));
                list.add(KeyHints.Hint.note("CLOCK RUNNING  ·  NO PAUSE"));
            }
            case FEEDBACK -> {
                list.add(KeyHints.Hint.of("ENTER", "NEXT QUESTION"));
                if (duckReady) list.add(KeyHints.Hint.of("R", "DUCK"));
                list.add(KeyHints.Hint.of("H", "LOG"));
            }
            case ENDING -> {
                if (dialogue.isVisible() && !dialogue.isFinished() && !actions.hasChoices())
                    list.add(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"));
                list.add(KeyHints.Hint.of("H", "LOG"));
            }
            case DONE -> { }
        }
        hud.setHints(list.toArray(KeyHints.Hint[]::new));
    }

    /** Mr. Reward's hand-drawn title card on a red band as he steps in; skipped when the art is missing. */
    private void titleCard() {
        var logo = ctx.assets().ui("reward-logo");
        if (logo.isEmpty()) return;
        javafx.scene.image.ImageView mark = new javafx.scene.image.ImageView(logo.get());
        mark.setPreserveRatio(true);
        mark.setFitHeight(150);
        mark.setSmooth(true);
        Label sub = new Label("ICS4U  //  FINAL EXAM");
        sub.getStyleClass().addAll("display", "cutin-sub", "tone-danger");
        VBox card = new VBox(-8, mark, sub);
        Pane layer = new Pane();
        layer.setMouseTransparent(true);
        StackPane root = (StackPane) root();
        root.getChildren().add(root.getChildren().indexOf(log), layer);
        PauseTransition wait = new PauseTransition(Duration.millis(520));
        wait.setOnFinished(e -> CutIn.play(layer, 390, card, "danger", 200, Duration.millis(1500))
                .setOnFinished(done -> root.getChildren().remove(layer)));
        wait.play();
    }

    private void openLog() {
        if (dialogue.history().isEmpty() || log.isOpen()) return;
        hud.collapse();
        ctx.router().hideTips(true);
        log.open(dialogue.history(), () -> {
            ctx.router().hideTips(false);
            focus();
        });
    }

    // ---- text ---------------------------------------------------------------------------

    /** A short line for the question list: the start of the prompt, cut at a word. */
    static String excerpt(String prompt) {
        String text = prompt.strip().replaceAll("\\s+", " ");
        int max = 36;
        if (text.length() <= max) return text;
        int cut = text.lastIndexOf(' ', max);
        return text.substring(0, cut > 24 ? cut : max).replaceAll("[,.;:]$", "") + "…";
    }

    /** Seconds with one decimal, rounded down so a clock never shows time it does not have. */
    static String seconds(java.time.Duration duration) {
        long tenths = Math.max(0, duration.toMillis() / 100);
        return String.format(Locale.ROOT, "%d.%d", tenths / 10, tenths % 10);
    }

    static String reason(OpeningBattle.DefeatReason reason) {
        if (reason == null) return "";
        return switch (reason) {
            case WRONG_ANSWER -> "ONE WRONG ANSWER ENDS THE EXAM";
            case TIMEOUT -> "TIME RAN OUT";
            case PASS_OUT -> "HP 0";
        };
    }
}
