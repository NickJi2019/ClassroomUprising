package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.OpeningBattle;
import com.aphs.classroomuprising.OpeningContent;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
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
import java.util.stream.IntStream;

/**
 * The previous semester's ICS4U final, before summer school: the authored story, then the
 * exam as a turn-based battle, then one of its endings, then Day 1.
 *
 * <p>The battle: the ten questions spring out of Mr. Reward and hang in an arc in front of him
 * ({@link Danmaku}). Then rounds: <b>your turn</b> (face the next question, use an item, read the
 * log; the exam clock is paused), then <b>his attack</b>: the next question flies at you. Its
 * impact is the core's hit ({@link GameSession#hitOpeningQuestion}): the HP is lost there, with the
 * HUD's hit effects, and a lethal hit ends the exam at once. Otherwise the paper opens out of the
 * impact as a two-tone ripple ({@link Ripple}); only when it is fully open does the core resume
 * the clock ({@link GameSession#startOpeningQuestion}). All ten questions share one exam time
 * (180 seconds), spent only while a question is open; the screen shows what is left, paused or
 * running, from the view. You answer; the verdict shows at once (no HP cost); then the paper folds
 * back into you and flies at him as your bullet: it lands when you were right and fizzles when you
 * were wrong. Then your turn again.</p>
 *
 * <p>The exam itself belongs to the core ({@code OpeningBattle}): this screen sends the
 * player's moves (start the next question, an option, the duck) and draws the view it reads
 * back. The core times every question, judges every answer, applies HP and decides the ending;
 * nothing here computes a deadline, a result or a grade. Mr. Reward fires the questions in their
 * authored order (the first unanswered one). While the exam runs, a timer asks the core to settle
 * time ({@link GameSession#advanceOpeningBattle()}) every {@link #PULSE_NANOS}. Status effects
 * (the HUD's red flash, the edge pulse at low HP, the twitching SAN bar) follow the HP and SAN
 * the core reports. A defeat breaks the screen like glass ({@link Shatter}); the duck's banner
 * stays in front until the duck has worked ({@link ItemBanner}). There is no pause: no menu opens over the exam, and the log is closed while
 * a question is on the clock.</p>
 */
final class OpeningScreen extends Screen {
    private enum Stage { INTRO, FAN_OUT, TURN, ATTACK, REVEAL, QUESTION, FEEDBACK, COUNTER, ENDING, DONE }

    /** Mr. Reward as he steps in and throws, when he is hit, when he mocks a defeat, and when the player wins. */
    static final String ARRIVES = "gotcha", STRUCK = "stern", MOCKS = "ridicule", ASTONISHED = "staring";

    /** How often the core is asked to settle time (deadline, duck) while the exam runs. */
    static final long PULSE_NANOS = 100_000_000L;
    private static final PseudoClass HIGHLIGHT = PseudoClass.getPseudoClass("highlight");
    private static final PseudoClass CHOSEN = PseudoClass.getPseudoClass("chosen");
    private static final PseudoClass RIGHT = PseudoClass.getPseudoClass("right");
    private static final PseudoClass WRONG = PseudoClass.getPseudoClass("wrong");
    private static final PseudoClass PAUSED = PseudoClass.getPseudoClass("paused");
    /** Where the two stand when no art is delivered. */
    private static final Point2D PLAYER_FALLBACK = new Point2D(256, 430), REWARD_FALLBACK = new Point2D(947, 330);

    @FXML private SceneView scene;
    @FXML private DialogueBox dialogue;
    @FXML private ChoiceList actions;
    @FXML private VBox notice;
    @FXML private Hud hud;
    @FXML private Region examScrim;
    @FXML private VBox turn;
    @FXML private Label round;
    @FXML private ChoiceList commands;
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
    @FXML private Pane effects;

    private DialogueLog log;
    private final Danmaku danmaku = new Danmaku();
    private Stage stage = Stage.INTRO;
    /** The latest view read from the core. */
    private OpeningBattle.View last;
    /** The question on the paper (kept after answering, to show the feedback beside it). */
    private OpeningContent.QuestionView shown;
    private final List<HBox> rows = new ArrayList<>();
    private int highlighted = -1;
    /** The duck's announcement, in front while the duck works (null when none is up). */
    private ItemBanner duckBanner;
    /** The item list is open in the turn menu. */
    private boolean itemMenu;
    private String shownCountdown;
    /** An answer was just shown: the ending waits a moment so it can be read. */
    private boolean feedbackJustShown;
    private boolean endingPlayed;
    private long lastPulse;
    /** Counts attacks; a reveal that finishes after its attack is no longer current does nothing. */
    private int attackNumber;
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
        // The bullets fly over the scene but under the panels: the question paper covers the
        // ones still waiting, and the turn menu stays readable.
        root.getChildren().add(root.getChildren().indexOf(paper.getParent()), danmaku);
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
            if (stage == Stage.FEEDBACK && e.getButton() == MouseButton.PRIMARY) fireBack();
        });
        duck.setCursor(javafx.scene.Cursor.HAND);
        duck.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) useDuck();
        });
        // A hit that costs HP shakes the player's character as well as the HUD.
        hud.setOnHurt(() -> scene.hit("student"));
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

    /** Esc: out of the item list, or closes the log. There is no menu before the game starts. */
    @Override
    protected boolean back() {
        if (log != null && log.back()) return true;
        if (stage == Stage.TURN && itemMenu) {
            showTurn();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isPlaying() {
        return (stage == Stage.INTRO || stage == Stage.ENDING) && !dialogue.isFinished() && (log == null || !log.isOpen());
    }

    @Override
    protected void focus() {
        if (log != null && log.isOpen()) log.requestFocus();
        else if (actions.hasChoices()) actions.requestFocus();
        else if (stage == Stage.TURN && commands.hasChoices()) commands.requestFocus();
        else root().requestFocus();
    }

    // ---- the battle -------------------------------------------------------------------

    /** After the story: the core creates the exam (no clock yet) and the questions fan out of him. */
    private void startExam() {
        if (stage != Stage.INTRO) return;
        Outcome started = ctx.session().startOpeningBattle();
        if (!started.ok()) {
            notice(notice, Diagnostic.of(started));
            return;
        }
        read();
        showStats();
        dialogue.setVisible(false);
        lastPulse = 0;
        clock.start();
        fanOut();
    }

    /** Back into a running exam (the screen was rebuilt): the core's view says where it stands. */
    private void resume(OpeningBattle.View view) {
        last = view;
        showStats();
        dialogue.setVisible(false);
        if (view.finished()) {
            beginEnding();
            return;
        }
        clock.start();
        var active = view.availableQuestions().stream().filter(q -> q.id().equals(view.activeQuestionId())).findFirst();
        if (view.phase() == OpeningBattle.Phase.QUESTION && active.isPresent()) {
            showPaper(active.get(), view, false);   // its clock is running already: never started again
        } else if (view.phase() == OpeningBattle.Phase.QUESTION_REVEAL && active.isPresent()) {
            attackNumber++;
            showPaper(active.get(), view, true);    // already hit: the paper opens again, then the clock starts
        } else {
            fanOut();
        }
    }

    private void showStats() {
        hud.showStudent(last.student(), ctx.demoLabel());
        hud.setStatsVisible(true);
        duck.setVisible(true);
        Motion.slideIn(duck, 0);
        drawDuck(last);
    }

    /** The unanswered questions burst out of Mr. Reward and hang in an arc, in firing order. */
    private void fanOut() {
        stage = Stage.FAN_OUT;
        hints();
        int answered = last.answers().size();
        List<String> ids = last.availableQuestions().stream().map(OpeningContent.QuestionView::id).toList();
        List<String> numbers = IntStream.range(0, ids.size()).mapToObj(i -> "Q" + (answered + i + 1)).toList();
        List<String> lines = last.availableQuestions().stream().map(q -> excerpt(q.prompt())).toList();
        scene.speak("reward", ARRIVES);
        Sfx.CUTIN.play(danmaku);
        Animation out = danmaku.fanOut(ids, numbers, lines, reward());
        out.setOnFinished(e -> { if (stage == Stage.FAN_OUT) showTurn(); });
        out.play();
    }

    /** Every frame while the exam runs: settle time now and then, read the view, redraw. */
    private void pulse(long now) {
        if (stage == Stage.ENDING || stage == Stage.DONE || stage == Stage.INTRO || last == null) return;
        if (now - lastPulse >= PULSE_NANOS) {
            lastPulse = now;
            ctx.session().advanceOpeningBattle();
        }
        OpeningBattle.View before = last;
        OpeningBattle.View view = read();
        if (view.student().hp() != before.student().hp()) {
            hud.showStudent(view.student(), ctx.demoLabel());
            if (view.student().hp() > before.student().hp()) recovered(before, view);
        }
        if (view.finished()) {
            if (stage == Stage.QUESTION) timeUp();
            else if (stage != Stage.FEEDBACK && stage != Stage.COUNTER && stage != Stage.ATTACK) beginEnding();
            return;
        }
        drawClock(view);
        drawDuck(view);
        if (duckBanner != null && view.usingDuck()) duckBanner.update(duckDetail(view));
        if (before.usingDuck() != view.usingDuck() || before.ducksRemaining() != view.ducksRemaining()) {
            setAnswersEnabled(stage == Stage.QUESTION && !view.usingDuck());
            hints();
        }
        // When the duck finishes the menu is rebuilt, so the cursor rests on facing the next question.
        if (stage == Stage.TURN && before.usingDuck() && !view.usingDuck()) showTurn();
        else if (stage == Stage.TURN && view.usingDuck()) refreshTurn();
    }

    private OpeningBattle.View read() {
        Fetch<OpeningBattle.View> fetch = ctx.session().openingBattle();
        if (fetch.ok()) last = fetch.value();
        return last;
    }

    // ---- your turn ----------------------------------------------------------------------

    /** Between his attacks: face the next question, use an item, or read the log. No clock runs. */
    private void showTurn() {
        if (last == null || last.finished()) return;
        stage = Stage.TURN;
        itemMenu = false;
        shown = null;
        paper.setVisible(false);
        paper.setManaged(false);
        examScrim.setVisible(true);
        notice(notice, null);
        round.setText(roundLine());
        commands.setChoices(turnChoices());
        turn.setVisible(true);
        turn.setManaged(true);
        Motion.slideIn(turn, 0);
        hints();
        commands.requestFocus();
    }

    private List<ChoiceList.Choice> turnChoices() {
        String face = last.usingDuck()
                ? "The duck is working  ·  " + seconds(last.duckRemaining()) + " s"
                : "Q" + (last.answers().size() + 1) + " comes at you  ·  " + seconds(last.remaining()) + " s of exam time left";
        String item = last.ducksRemaining() > 0 ? "Ms Tang's rubber duck ×" + last.ducksRemaining() : "No items left";
        return List.of(
                ChoiceList.Choice.of("FACE THE NEXT QUESTION", face, this::attack).enabled(!last.usingDuck()),
                ChoiceList.Choice.of("ITEM", item, this::showItems).enabled(last.ducksRemaining() > 0 && !last.usingDuck()),
                ChoiceList.Choice.of("LOG", "Everything said so far", () -> {
                    openLog();
                    commands.unlock();
                }));
    }

    /** The turn menu's text follows the duck's countdown (each tenth of a second); the cursor stays. */
    private void refreshTurn() {
        if (itemMenu) {
            if (last.ducksRemaining() == 0) showTurn();
            return;
        }
        String countdown = seconds(last.duckRemaining());
        if (countdown.equals(shownCountdown)) return;
        shownCountdown = countdown;
        commands.update(turnChoices());
    }

    private void showItems() {
        itemMenu = true;
        OpeningContent content = ctx.session().openingContent();
        commands.setChoices(List.of(
                ChoiceList.Choice.of("MS TANG'S RUBBER DUCK", "HP " + StatDelta.signed(content.duckRecovery()) + " after "
                        + seconds(content.duckDuration()) + " s  ·  ×" + last.ducksRemaining(), this::useDuck).tone("caret"),
                ChoiceList.Choice.of("BACK", "Esc", this::showTurn).tone("cyan")));
        hints();
        commands.requestFocus();
    }

    // ---- his attack -------------------------------------------------------------------

    /** Mr. Reward fires the next question in order; where it lands the question opens. */
    private void attack() {
        if (stage != Stage.TURN || last.usingDuck() || last.availableQuestions().isEmpty()) {
            commands.unlock();
            return;
        }
        stage = Stage.ATTACK;
        turn.setVisible(false);
        turn.setManaged(false);
        commands.setChoices(List.of());
        hints();
        String id = last.availableQuestions().getFirst().id();
        int number = ++attackNumber;
        scene.speak("reward", ARRIVES);
        Point2D target = player();
        danmaku.fire(id, target, () -> impact(id, number)).play();
    }

    /**
     * The question lands on the player: the core's hit takes the HP now (the HUD shows the drop
     * with its hit effects and the character is struck). A lethal hit ends the exam at once;
     * otherwise the paper opens out of the impact, and only then does the clock start.
     */
    private void impact(String questionId, int number) {
        if (stage != Stage.ATTACK || number != attackNumber) return;
        Outcome hit = ctx.session().hitOpeningQuestion(questionId);
        if (!hit.ok()) {
            notice(notice, Diagnostic.of(hit));
            showTurn();
            return;
        }
        double hpBefore = last.student().hp();
        OpeningBattle.View view = read();
        hud.showStudent(view.student(), ctx.demoLabel());   // the drop plays the hit effects (Hud.onHurt)
        if (view.student().hp() < hpBefore) popHp(view.student().hp() - hpBefore);
        else scene.hit("student");
        if (view.finished()) {
            beginEnding();   // passed out on the hit: no paper, no clock
            return;
        }
        view.availableQuestions().stream().filter(q -> q.id().equals(questionId)).findFirst()
                .ifPresent(q -> showPaper(q, view, true));
    }

    /**
     * The paper has fully opened: now the core resumes the exam clock, once. A late or repeated
     * call (another attack since, the screen left, the exam over, the clock already running) does
     * nothing.
     */
    private void revealed(String questionId, int number) {
        if (stage != Stage.REVEAL || number != attackNumber || last == null || last.finished()) return;
        if (last.phase() != OpeningBattle.Phase.QUESTION_REVEAL || !questionId.equals(last.activeQuestionId())) return;
        Outcome started = ctx.session().startOpeningQuestion(questionId);
        OpeningBattle.View view = read();
        if (!started.ok() || view.phase() != OpeningBattle.Phase.QUESTION) {
            if (view.finished()) beginEnding();
            else notice(notice, Diagnostic.of(started));
            return;
        }
        stage = Stage.QUESTION;
        drawClock(view);
        setAnswersEnabled(!view.usingDuck());
        hints();
    }

    /** The question on the clock: its prompt and complete code, and its five options. */
    private void showPaper(OpeningContent.QuestionView question, OpeningBattle.View view, boolean ripple) {
        boolean running = view.phase() == OpeningBattle.Phase.QUESTION;
        stage = running ? Stage.QUESTION : Stage.REVEAL;
        shown = question;
        turn.setVisible(false);
        turn.setManaged(false);
        examScrim.setVisible(true);
        notice(notice, null);
        questionTag.setText("QUESTION " + view.questionOrdinal() + " / 10");
        verdict.setText("");
        verdict.getStyleClass().removeAll("verdict-right", "verdict-wrong");
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
        paper.setManaged(true);
        drawClock(view);
        // Nothing can be answered while the paper is still opening (the clock is not running yet).
        setAnswersEnabled(running && !view.usingDuck());
        if (ripple) {
            Animation open = Ripple.open(paper, player());
            int number = attackNumber;
            String id = question.id();
            open.setOnFinished(e -> revealed(id, number));   // the ripple cleans itself up separately
            open.play();
        } else {
            paper.setVisible(true);
            Motion.slideIn(paper, 0);
            if (!running) revealed(question.id(), attackNumber);
        }
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

    /** Sends the answer; the core judges it, applies HP and may end the exam. */
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
        stage = Stage.FEEDBACK;
        hints();
        if (view.finished()) {
            // The verdict stays readable for a moment, then the exam ends its own way.
            PauseTransition read = new PauseTransition(Duration.millis(1400));
            read.setOnFinished(e -> { if (stage == Stage.FEEDBACK) fireBack(); });
            read.play();
        }
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
        if (last != null) drawClock(last);   // the exam time stops here, and shows PAUSED
    }

    /**
     * The paper folds back into the player and flies at Mr. Reward as the player's bullet: it
     * lands when the answer was right and fizzles when it was wrong. A player who passed out
     * fires nothing. Then the next turn, or the ending.
     */
    private void fireBack() {
        if (stage != Stage.FEEDBACK || last == null) return;
        stage = Stage.COUNTER;
        hints();
        OpeningBattle.Feedback answer = last.answers().isEmpty() ? null : last.answers().getLast();
        boolean passedOut = last.finished() && last.ending().animation() == OpeningBattle.AnimationCue.PASS_OUT;
        Animation fold = Ripple.close(paper, player());
        fold.setOnFinished(e -> {
            paper.setManaged(false);
            if (answer == null || passedOut) {
                afterCounter();
                return;
            }
            String number = "Q" + last.answers().size();
            String line = shown == null ? "" : excerpt(shown.prompt());
            danmaku.counter(player(), reward(), number, line, answer.correct(), landed -> {
                if (landed) {
                    scene.hit("reward");
                    scene.speak("reward", STRUCK);
                } else {
                    scene.speak("reward", MOCKS);
                }
                PauseTransition settle = new PauseTransition(Duration.millis(420));
                settle.setOnFinished(f -> afterCounter());
                settle.play();
            }).play();
        });
        fold.play();
    }

    private void afterCounter() {
        if (stage != Stage.COUNTER) return;
        if (last.finished()) beginEnding();
        else showTurn();
    }

    /** No answer before the deadline: the paper folds away and the exam is over. */
    private void timeUp() {
        stage = Stage.COUNTER;
        dropDuckBanner();
        clock.stop();
        setAnswersEnabled(false);
        time.setText("0.0");
        time.getStyleClass().add("overtime");
        gauge.show(1, true);
        verdict.setText("TIME UP");
        verdict.getStyleClass().add("verdict-wrong");
        hints();
        PauseTransition read = new PauseTransition(Duration.millis(700));
        read.setOnFinished(e -> {
            Animation fold = Ripple.close(paper, player());
            fold.setOnFinished(f -> afterCounter());
            fold.play();
        });
        read.play();
    }

    // ---- the duck ---------------------------------------------------------------------

    /** Uses the one duck: a big announcement; its HP arrives later, on the core's clock. */
    private void useDuck() {
        if (stage != Stage.TURN && stage != Stage.QUESTION && stage != Stage.FEEDBACK) return;   // not while revealing
        if (last == null || last.ducksRemaining() == 0 || last.usingDuck()) {
            if (stage == Stage.TURN) commands.unlock();
            return;
        }
        Outcome outcome = ctx.session().useOpeningDuck();
        if (!outcome.ok()) {
            notice(notice, Diagnostic.of(outcome));
            return;
        }
        OpeningBattle.View view = read();
        if (view.finished()) {
            if (stage == Stage.QUESTION) timeUp();
            else beginEnding();
            return;
        }
        duckBanner = ItemBanner.show(effects, "ITEM", "MS TANG'S RUBBER DUCK", duckDetail(view), ItemBanner.duck());
        drawDuck(view);
        setAnswersEnabled(false);
        if (stage == Stage.TURN) showTurn();
        hints();
    }

    /** What the duck is doing, for its banner: the HP it will give and when (the question clock keeps running). */
    private String duckDetail(OpeningBattle.View view) {
        OpeningContent content = ctx.session().openingContent();
        return "HP " + StatDelta.signed(content.duckRecovery()) + " in " + seconds(view.duckRemaining()) + " s"
                + (stage == Stage.QUESTION ? "  ·  the clock keeps running" : "");
    }

    /** The duck's HP arrived: its banner, up all this time, shows the result and leaves. */
    private void recovered(OpeningBattle.View before, OpeningBattle.View after) {
        double change = after.student().hp() - before.student().hp();
        popHp(change);
        if (duckBanner == null) duckBanner = ItemBanner.show(effects, "EFFECT", "", "", ItemBanner.duck());
        duckBanner.finish("EFFECT", "HP " + StatDelta.signed(change), "Ms Tang's rubber duck worked", Duration.millis(650));
        duckBanner = null;
        Sfx.CONFIRM.play(duck);
    }

    private void dropDuckBanner() {
        if (duckBanner != null) duckBanner.dismiss();
        duckBanner = null;
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

    /**
     * The exam's shared time: what is left of the total (the same in every phase; the core never
     * refills it), and whether it is running now. Redrawn only when the shown tenth or the state
     * changes.
     */
    private void drawClock(OpeningBattle.View view) {
        String text = seconds(view.remaining());
        String state = (view.clockRunning() ? "/ " : "/ ") + seconds(view.timeLimit()) + " s EXAM TIME"
                + (view.clockRunning() ? "" : "  ·  PAUSED");
        if (!state.equals(limit.getText())) {
            limit.setText(state);
            time.pseudoClassStateChanged(PAUSED, !view.clockRunning());
        }
        if (text.equals(time.getText())) return;
        double total = view.timeLimit().toNanos() / 1e9;
        double left = view.remaining().toNanos() / 1e9;
        time.setText(text);
        gauge.show(total <= 0 ? 1 : 1 - left / total, false);
        if (stage == Stage.TURN) round.setText(roundLine());
    }

    /** The turn header: the round, the questions left and the exam time left (paused). */
    private String roundLine() {
        int answered = last.answers().size(), left = last.availableQuestions().size();
        return "ROUND " + (answered + 1) + "  ·  " + left + (left == 1 ? " QUESTION LEFT" : " QUESTIONS LEFT")
                + "  ·  EXAM TIME " + seconds(last.remaining()) + " s";
    }

    /** The duck chip; its style and text change only when they differ (no CSS pass every frame). */
    private void drawDuck(OpeningBattle.View view) {
        String state, text;
        if (view.usingDuck()) {
            state = "duck-working";
            text = "WORKING  " + seconds(view.duckRemaining()) + " s";
        } else if (view.ducksRemaining() > 0) {
            state = "duck-ready";
            OpeningContent content = ctx.session().openingContent();
            text = "×" + view.ducksRemaining() + "  ·  HP " + StatDelta.signed(content.duckRecovery())
                    + " AFTER " + seconds(content.duckDuration()) + " s";
        } else {
            state = "duck-used";
            text = "USED";
        }
        if (!duck.getStyleClass().contains(state)) {
            duck.getStyleClass().removeAll("duck-ready", "duck-working", "duck-used");
            duck.getStyleClass().add(state);
        }
        if (!text.equals(duckLabel.getText())) duckLabel.setText(text);
    }

    /** While the duck works, nothing can be answered. */
    private void setAnswersEnabled(boolean enabled) {
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
        dropDuckBanner();
        setAnswersEnabled(false);
        commands.setChoices(List.of());
        turn.setVisible(false);
        danmaku.clear();
        hints();
        OpeningBattle.Ending ending = last.ending();
        PauseTransition lead = new PauseTransition(Duration.millis(feedbackJustShown && paper.isVisible() ? 1400 : 300));
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
            default -> {
                defeated(ending);
                return;
            }
        }
        PauseTransition wait = new PauseTransition(Duration.millis(hold));
        wait.setOnFinished(e -> { if (stage == Stage.ENDING) endingStory(ending); });
        wait.play();
    }

    /**
     * Defeated (a wrong answer, the clock, or HP 0): the screen breaks like glass around the
     * player and falls away; behind it the exam is gone and the room is dim; then the call-out.
     */
    private void defeated(OpeningBattle.Ending ending) {
        boolean passedOut = ending.animation() == OpeningBattle.AnimationCue.PASS_OUT;
        Animation breaking = Shatter.play(effects, root(), player(), () -> {
            clearExam();
            scene.setDim(passedOut ? 0.55 : 0.3);
            scene.speak("reward", MOCKS);
        });
        breaking.setOnFinished(e -> {
            if (stage != Stage.ENDING) return;
            if (passedOut) CutIn.play(effects, 300, "PASSED OUT", "HP 0  ·  THE PAGE WENT DARK", "danger");
            else CutIn.play(effects, 300, "DEFEAT", reason(ending.reason()), "danger");
            PauseTransition wait = new PauseTransition(Duration.millis(1700));
            wait.setOnFinished(f -> { if (stage == Stage.ENDING) endingStory(ending); });
            wait.play();
        });
        breaking.play();
    }

    /** The exam's panels leave the screen (the story of the ending is told over the room). */
    private void clearExam() {
        paper.setVisible(false);
        paper.setManaged(false);
        turn.setVisible(false);
        turn.setManaged(false);
        examScrim.setVisible(false);
        duck.setVisible(false);
    }

    /** The grade stays on screen while the authored ending is told. */
    private void endingStory(OpeningBattle.Ending ending) {
        clearExam();
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
                new KeyValue(stamp.scaleXProperty(), 1, MotionTokens.OUT),
                new KeyValue(stamp.scaleYProperty(), 1, MotionTokens.OUT),
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

    /** Keys for the battle; the turn menu handles its own arrows and Enter when it has focus. */
    private boolean key(KeyCode code) {
        if (code == KeyCode.H) {
            // Not while a question is coming, opening or on the clock.
            if (stage != Stage.ATTACK && stage != Stage.REVEAL && stage != Stage.QUESTION) openLog();
            return true;
        }
        if (code == KeyCode.R) {   // the rubber duck (D is an answer)
            useDuck();
            return true;
        }
        if (stage == Stage.FEEDBACK && (code == KeyCode.ENTER || code == KeyCode.SPACE)) {
            if (!last.finished()) fireBack();
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
            case TURN -> {
                list.add(KeyHints.Hint.of("↑↓", "CHOOSE"));
                list.add(KeyHints.Hint.of("ENTER", "SELECT"));
                if (itemMenu) list.add(KeyHints.Hint.of("ESC", "BACK"));
                else if (duckReady) list.add(KeyHints.Hint.of("R", "DUCK"));
                list.add(KeyHints.Hint.of("H", "LOG"));
            }
            case QUESTION -> {
                list.add(KeyHints.Hint.of("A–E", "CHOOSE"));
                list.add(KeyHints.Hint.of("ENTER", "ANSWER"));
                if (duckReady) list.add(KeyHints.Hint.of("R", "DUCK"));
                list.add(KeyHints.Hint.note("CLOCK RUNNING  ·  NO PAUSE"));
            }
            case FEEDBACK -> {
                if (last != null && !last.finished()) {
                    list.add(KeyHints.Hint.of("ENTER", "FIRE BACK"));
                    if (duckReady) list.add(KeyHints.Hint.of("R", "DUCK"));
                    list.add(KeyHints.Hint.of("H", "LOG"));
                }
            }
            case ENDING -> {
                if (dialogue.isVisible() && !dialogue.isFinished() && !actions.hasChoices())
                    list.add(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"));
                list.add(KeyHints.Hint.of("H", "LOG"));
            }
            case FAN_OUT, ATTACK, COUNTER, DONE -> { }
        }
        hud.setHints(list.toArray(KeyHints.Hint[]::new));
    }

    /** Where the player's character is struck (the middle of the figure). */
    private Point2D player() { return scene.centreOf("student").orElse(PLAYER_FALLBACK); }

    /** Where Mr. Reward's questions come from and the player's answers land. */
    private Point2D reward() { return scene.centreOf("reward").orElse(REWARD_FALLBACK); }

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

    /** A short line for a question strip: the start of the prompt, cut at a word. */
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
