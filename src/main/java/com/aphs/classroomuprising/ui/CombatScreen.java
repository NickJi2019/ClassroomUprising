package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatRules;
import com.aphs.classroomuprising.CombatSession;
import com.aphs.classroomuprising.CourseType;
import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.GameController;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An assessment played as a combat (see docs/frontend-combat-handoff.md).
 *
 * <p>Everything on screen comes from {@link GameSession#combatView()}: the question cards in
 * difficulty order with their initial weights, the open question's backend clock, Dasher and
 * Combo state, pending tickets and, at the end, the backend's breakdown. The screen never
 * times, weighs, damages or marks anything itself. It starts questions, captures and locks the
 * answer, sends it to the trusted evaluator off the UI thread, hands the result back with its
 * ticket, and animates what the backend reports.</p>
 *
 * <ul>
 *   <li>Between questions the Esc menu is available. While a question is open the backend
 *       clock keeps running (there is no pause rule), so the menu is not offered.</li>
 *   <li>A refused or failed evaluation keeps the answer and its ticket; RETRY sends the same
 *       answer again. Nothing is marked zero or skipped by the screen.</li>
 *   <li>Results that arrive after the player has left the combat are dropped.</li>
 * </ul>
 */
final class CombatScreen extends Screen {
    private static final String RULES = String.join("\n",
            "Choose any target. Its clock starts when you open it and keeps running; there is no pause.",
            "30 seconds is a soft limit: you can still answer after it, but that question loses its weight bonus and the Combo is lost.",
            "Whichever question you answer last gets the Dasher bonus. Every evaluated answer costs HP.");

    @FXML private SceneView scene;
    @FXML private Label title;
    @FXML private Label flags;
    @FXML private ChoiceList targets;
    @FXML private Label cleared;
    @FXML private Node dasherChip;
    @FXML private Node comboChip;
    @FXML private Label combo;
    @FXML private VBox panel;
    @FXML private Label questionTag;
    @FXML private Label weight;
    @FXML private Label weightNote;
    @FXML private Label time;
    @FXML private Label limit;
    @FXML private TimeGauge gauge;
    @FXML private Label prompt;
    @FXML private StackPane answerBox;
    @FXML private TextArea answer;
    @FXML private VBox busy;
    @FXML private Label busyTitle;
    @FXML private Label busySub;
    @FXML private Label keys;
    @FXML private VBox results;
    @FXML private Label creditsLine;
    @FXML private GridPane resultRows;
    @FXML private Label comboLine;
    @FXML private Label resultNote;
    @FXML private VBox notice;
    @FXML private ChoiceList commands;
    /** Holds QUIT TO TITLE while a refusal leaves no way forward. */
    @FXML private ChoiceList exit;
    @FXML private Region overtimeGlow;
    @FXML private Region hitFlash;
    @FXML private Hud hud;
    @FXML private Pane cutins;

    private final GameController.AssessmentView assessment;
    private final AnimationTimer ticker = new AnimationTimer() {
        @Override public void handle(long now) { tick(now); }
    };
    private DayPlan plan;
    private CombatEvaluations evaluations;
    private CombatSession.View last;

    /** The locked submission and the exact work sent for it, kept until the core accepts a result. */
    private CombatSession.AnswerTicket submitted;
    private CombatEvaluator.Work submittedWork;
    /** The last result for the submission was refused or failed; RETRY is offered. */
    private boolean evaluationStuck;
    private CombatSession.RewardTicket reward;
    private long rewardRetryAt;
    private boolean rewardNoticeShown;
    /** A refusal is showing that may leave no way forward; QUIT is offered. */
    private boolean blocked;
    private boolean left;
    private boolean resultsShown;
    private String openQuestion;
    private boolean wasTimedOut;
    private List<String> commandLabels = List.of();

    CombatScreen(AppContext ctx, GameController.AssessmentView assessment) {
        super(ctx);
        this.assessment = assessment;
    }

    @Override
    protected Parent build() {
        StackPane root = Views.load("combat.fxml", this);
        plan = ctx.session().view().currentCourse();
        ctx.stage(scene, place(plan.course()), null, "combat");
        root.getChildren().add(1, new Backdrop(
                new Backdrop.Glyph(UiText.course(plan.course()), 640, 300, 200, true),
                new Backdrop.Glyph("while (t <= 30) answer();", 560, 590, 40, false)));
        title.setText(UiText.courseLine(plan));
        flags.setText(flagLine());
        cutins.setClip(new Rectangle(ClassroomApp.WIDTH, ClassroomApp.HEIGHT));
        if (plan.course() == CourseType.LAB) answer.getStyleClass().addAll("mono", "code-area");
        answer.setPromptText(plan.course() == CourseType.LAB ? "// write your solution here" : "Type your answer");
        answer.addEventFilter(KeyEvent.KEY_PRESSED, this::onAnswerKey);
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && (e.isShortcutDown() || e.isControlDown())) {
                e.consume();
                submit();
            }
        });
        CombatEvaluator evaluator = ctx.combatEvaluator();
        if (evaluator != null) evaluations = CombatEvaluations.background(evaluator, Platform::runLater);
        return root;
    }

    @Override
    protected void onShown() {
        refresh();
        ticker.start();
    }

    @Override
    protected void onHidden() {
        leave();
    }

    /** Only between questions: an open question's clock cannot be paused. */
    @Override
    protected boolean allowsPause() { return last != null && last.currentQuestionId() == null; }

    @Override
    protected void focus() {
        if (last == null) root().requestFocus();
        else if (answer.isEditable() && answerBox.isVisible()) answer.requestFocus();
        else if (commands.hasChoices() && (last.complete() || last.currentQuestionId() != null)) commands.requestFocus();
        else targets.requestFocus();
    }

    // ---- reading the backend ---------------------------------------------------------

    /** Redraws everything from a fresh view; called after each player action. */
    private void refresh() {
        if (left || !ctx.hasSession()) return;
        Fetch<CombatSession.View> fetch = ctx.session().combatView();
        if (!fetch.ok()) {
            ticker.stop();
            notice(notice, Diagnostic.of(fetch.failure()));
            blocked = true;
            setCommands(List.of());
            showExit();
            return;
        }
        CombatSession.View view = fetch.value();
        last = view;
        hud.show(ctx.session().view(), ctx.demoLabel());
        hud.setHint(view.currentQuestionId() == null ? "ESC  MENU" : "CLOCK RUNNING  ·  NO PAUSE");
        showTargets(view);
        showStatus(view);
        if (view.complete()) showResults();
        else showQuestion(view);
        showCommands(view);
        showClock(view, System.nanoTime());
    }

    /** Every frame: the clock, overtime, reward countdown. */
    private void tick(long now) {
        if (left || !ctx.hasSession()) {
            ticker.stop();
            return;
        }
        Fetch<CombatSession.View> fetch = ctx.session().combatView();
        if (!fetch.ok()) {
            ticker.stop();
            return;
        }
        last = fetch.value();
        showClock(last, now);
    }

    private void showClock(CombatSession.View view, long now) {
        boolean open = view.currentQuestionId() != null;
        if (open) {
            time.setText(CombatText.seconds(view.elapsed()));
            gauge.show(view.elapsed().toNanos() / (double) CombatRules.QUESTION_LIMIT.toNanos(), view.timedOut());
            weightNote.setText(view.timedOut() ? "WEIGHT BONUS LOST" : "");
            toggle(weight, "struck", view.timedOut());
            toggle(time, "overtime", view.timedOut());
        }
        if (view.timedOut()) {
            // A stepped pulse at the interface's drawing rate (24 steps per second).
            long step = (now / (1_000_000_000L / MotionTokens.STEP_RATE)) % 24;
            double tri = step < 12 ? step / 12.0 : (24 - step) / 12.0;
            overtimeGlow.setOpacity(0.35 + 0.4 * tri);
        } else if (overtimeGlow.getOpacity() != 0) {
            overtimeGlow.setOpacity(0);
        }
        if (view.timedOut() && !wasTimedOut)
            CutIn.play(cutins, 300, "OVERTIME", "WEIGHT BONUS LOST  ·  COMBO LOST  ·  KEEP ANSWERING", "danger");
        wasTimedOut = view.timedOut();
        showCombo(view);

        if (view.pendingReward() != null) {
            busySub.setText(CombatText.seconds(view.rewardRemaining()) + "s left  ·  timing only, no item effect (T09)");
            if (view.rewardRemaining().isZero() && reward != null && now >= rewardRetryAt) finishReward();
        }
    }

    // ---- drawing --------------------------------------------------------------------

    private void showTargets(CombatSession.View view) {
        List<ChoiceList.Choice> list = new ArrayList<>();
        for (int i = 0; i < view.questions().size(); i++) {
            CombatSession.QuestionView q = view.questions().get(i);
            boolean dasher = view.dasherReady() && q.status() != CombatSession.QuestionStatus.ANSWERED;
            String state = dasher && q.status() == CombatSession.QuestionStatus.AVAILABLE ? "DASHER" : CombatText.status(q.status());
            String label = CombatText.label(i) + "  " + CombatText.weight(q.initialWeight()) + "  " + state;
            String hint = q.status() == CombatSession.QuestionStatus.ANSWERED ? answerLine(view, q.id())
                    : CombatText.excerpt(q.prompt(), 40);
            String tone = switch (q.status()) {
                case ANSWERING, SUBMITTED -> "cyan";
                default -> dasher ? "caret" : "normal";
            };
            list.add(new ChoiceList.Choice(label, hint, tone, q.status() != CombatSession.QuestionStatus.ANSWERED,
                    () -> startQuestion(q.id())));
        }
        if (targets.hasChoices()) targets.update(list);
        else targets.setChoices(list);
    }

    private void showStatus(CombatSession.View view) {
        long done = view.questions().stream().filter(q -> q.status() == CombatSession.QuestionStatus.ANSWERED).count();
        cleared.setText("CLEARED  " + done + " / " + view.questions().size());
        toggle(dasherChip, "chip-lit", view.dasherReady());
        showCombo(view);
    }

    private void showCombo(CombatSession.View view) {
        String text = "COMBO  ·  " + CombatText.combo(view.combo());
        if (!text.equals(combo.getText())) combo.setText(text);
        toggle(comboChip, "chip-lost", view.combo() == CombatSession.ComboStatus.LOST);
        toggle(comboChip, "chip-lit", view.combo() == CombatSession.ComboStatus.EARNED);
    }

    private void showQuestion(CombatSession.View view) {
        show(results, false);
        show(panel, true);
        CombatSession.QuestionView q = current(view);
        boolean open = q != null;
        show(answerBox, open);
        gauge.setVisible(open);
        limit.setVisible(open);
        if (!open) {
            openQuestion = null;
            questionTag.setText("SELECT A TARGET");
            weight.setText("");
            weightNote.setText("");
            time.setText("");
            prompt.setText(RULES);
            keys.setText("UP / DOWN  choose     ENTER  open it and start its clock");
            return;
        }
        if (!q.id().equals(openQuestion)) {
            openQuestion = q.id();
            answer.clear();
            for (Node child : panel.getChildren()) Motion.slideIn(child, 0);
        }
        questionTag.setText(CombatText.label(view.questions().indexOf(q)));
        weight.setText(CombatText.weight(q.initialWeight()));
        prompt.setText(q.prompt());

        boolean answering = q.status() == CombatSession.QuestionStatus.ANSWERING;
        boolean editable = answering && view.pendingReward() == null;
        answer.setEditable(editable);
        toggle(answer, "locked", !editable);
        if (view.pendingReward() != null) {
            busyTitle.setText(view.pendingReward().kind() + " REWARD");
            busySub.setText(CombatText.seconds(view.rewardRemaining()) + "s left");
            busy.setVisible(true);
        } else if (q.status() == CombatSession.QuestionStatus.SUBMITTED) {
            busyTitle.setText(evaluationStuck ? "NOT EVALUATED" : "EVALUATING");
            busySub.setText(evaluationStuck ? "Your answer and its ticket are kept" : "Answer locked  ·  waiting for the evaluator");
            busy.setVisible(true);
        } else {
            busy.setVisible(false);
        }
        toggle(busy, "busy-stuck", evaluationStuck);
        keys.setText(editable ? "CTRL+ENTER  submit     TAB  commands" : "");
    }

    private void showResults() {
        show(panel, false);
        show(results, true);
        if (resultsShown) return;
        Fetch<CombatSession.Summary> fetch = ctx.session().combatSummary();
        if (!fetch.ok()) {
            notice(notice, Diagnostic.of(fetch.failure()));
            return;
        }
        resultsShown = true;
        CombatSession.Summary summary = fetch.value();
        creditsLine.setText("WEIGHTED CREDITS  " + CombatText.credits(summary.weightedCredits()));
        resultRows.getChildren().clear();
        String[] head = {"#", "Q", "TIME", "WEIGHT", "SCORE", "CREDIT", "HP"};
        for (int c = 0; c < head.length; c++) resultRows.add(cell(head[c], "result-head"), c, 0);
        List<CombatSession.QuestionView> order = last.questions();
        for (int i = 0; i < summary.answers().size(); i++) {
            CombatSession.Answer a = summary.answers().get(i);
            int position = indexOf(order, a.questionId());
            String weightText = a.timedOut()
                    ? CombatText.weight(a.initialWeight()) + " → " + CombatText.weight(a.effectiveWeight())
                    : CombatText.weight(a.effectiveWeight());
            String hp = "−" + StatDelta.number(a.appliedHpCost())
                    + (a.appliedHpCost() == 0 && a.requestedHpCost() > 0 ? " (tutorial)" : "");
            Node[] row = {
                    cell("#" + (i + 1), "result-index"),
                    cell(CombatText.label(position), "result-q"),
                    cell(CombatText.seconds(a.elapsed()) + "s", a.timedOut() ? "result-late" : "result-cell"),
                    tagged(weightText, a.timedOut() ? "OVERTIME" : null, a.dasher() ? "DASHER" : null),
                    cell(CombatText.percent(a.earnedFraction()), "result-cell"),
                    cell(CombatText.credits(a.weightedCredit()), "result-credit"),
                    cell(hp, "result-hp")};
            for (int c = 0; c < row.length; c++) {
                resultRows.add(row[c], c, i + 1);
                Motion.slideIn(row[c], 120 + i * 90L);
            }
        }
        comboLine.setText("COMBO  " + CombatText.combo(summary.combo())
                + (summary.comboBonus() > 0 ? "   +" + summary.comboBonus() + " BONUS, KEPT SEPARATE" : ""));
        toggle(comboLine, "combo-lost", summary.combo() == CombatSession.ComboStatus.LOST);
        resultNote.setText("Weighted credits are not a percentage. SETTLE converts the credits and the Combo bonus "
                + "with the approved policy and records the mark through the normal result flow.");
        for (Node child : results.getChildren()) Motion.slideIn(child, 0);
    }

    private void showCommands(CombatSession.View view) {
        List<ChoiceList.Choice> list = new ArrayList<>();
        CombatSession.QuestionView q = current(view);
        if (view.complete()) {
            list.add(ChoiceList.Choice.of("SETTLE ASSESSMENT", "Convert and record this result", this::settle).tone("cyan"));
        } else if (q != null && view.pendingReward() == null) {
            if (q.status() == CombatSession.QuestionStatus.ANSWERING) {
                list.add(ChoiceList.Choice.of("SUBMIT ANSWER", "CTRL+ENTER  ·  locks it for evaluation", this::submit));
                list.add(ChoiceList.Choice.of("SAFE REWARD  " + CombatText.seconds(CombatRules.SAFE_REWARD_DURATION) + "s",
                        "Timing only  ·  item effects are T09", () -> beginReward(CombatSession.RewardKind.SAFE)));
                list.add(ChoiceList.Choice.of("RISKY REWARD", "Timing only  ·  duration set by policy",
                        () -> beginReward(CombatSession.RewardKind.RISKY)));
            } else if (evaluationStuck && submittedWork != null) {
                list.add(ChoiceList.Choice.of("RETRY EVALUATION", "Same ticket  ·  same answer", this::retry).tone("cyan"));
            }
        }
        setCommands(list);
        showExit();
    }

    private void showExit() {
        if (blocked == exit.hasChoices()) return;
        exit.setChoices(blocked ? List.of(quit()) : List.of());
    }

    private void setCommands(List<ChoiceList.Choice> list) {
        List<String> labels = list.stream().map(ChoiceList.Choice::label).toList();
        if (labels.equals(commandLabels) && commands.hasChoices()) commands.update(list);
        else commands.setChoices(list);
        commandLabels = labels;
    }

    // ---- player actions ----------------------------------------------------------------

    private void startQuestion(String id) {
        boolean reopen = last != null && id.equals(last.currentQuestionId());
        Outcome outcome = ctx.session().startCombatQuestion(id);
        if (outcome.ok()) {
            if (!reopen) notice(notice, null);
            refresh();
            if (answer.isEditable()) answer.requestFocus();
        } else {
            notice(notice, Diagnostic.of(outcome));
            targets.unlock();
        }
    }

    private void submit() {
        if (left) return;
        Fetch<CombatSession.View> fetch = ctx.session().combatView();
        CombatSession.QuestionView q = fetch.ok() ? current(fetch.value()) : null;
        if (q == null || q.status() != CombatSession.QuestionStatus.ANSWERING || fetch.value().pendingReward() != null
                || (evaluations != null && evaluations.isBusy())) {
            commands.unlock();
            return;
        }
        String text = answer.getText();
        if (text == null || text.isBlank()) {
            notice(notice, Diagnostic.error("error: the answer is empty", "Write an answer first. Nothing was submitted."));
            commands.unlock();
            answer.requestFocus();
            return;
        }
        if (evaluations == null) {
            String rule = CombatEvaluator.pendingRule(plan.course());
            notice(notice, Diagnostic.warning(
                    "warning[" + rule + "]: no trusted evaluator for " + UiText.course(plan.course()) + " answers",
                    "Graded submission stays unavailable until the backend supplies one. Nothing was submitted; the clock is still running."));
            blocked = true;
            showCommands(fetch.value());
            return;
        }
        Fetch<CombatSession.AnswerTicket> ticket = ctx.session().submitCombatAnswer();
        if (!ticket.ok()) {
            notice(notice, Diagnostic.of(ticket.failure()));
            commands.unlock();
            return;
        }
        submitted = ticket.value();
        submittedWork = new CombatEvaluator.Work(plan.assessmentId(), plan.course(), q.id(), q.prompt(), text);
        evaluationStuck = false;
        notice(notice, null);
        dispatch();
        refresh();
    }

    private void dispatch() {
        if (!evaluations.dispatch(submitted, submittedWork, this::onEvaluated))
            notice(notice, Diagnostic.error("error: an evaluation is already running", "Wait for it to finish."));
    }

    private void retry() {
        if (submitted == null || submittedWork == null || evaluations == null || evaluations.isBusy()) {
            commands.unlock();
            return;
        }
        evaluationStuck = false;
        blocked = false;
        notice(notice, null);
        dispatch();
        refresh();
    }

    /** On the UI thread, only for this screen's live run (abandoned results never arrive here). */
    private void onEvaluated(CombatEvaluations.Result result) {
        if (left || !ctx.hasSession() || !Objects.equals(result.ticket(), submitted)) return;
        switch (result) {
            case CombatEvaluations.Result.Evaluated evaluated -> {
                Outcome outcome = ctx.session().recordCombatEvaluation(evaluated.ticket(),
                        evaluated.evaluation().earnedFraction(), evaluated.evaluation().evidence());
                if (outcome instanceof Outcome.Done done) accepted(done);
                else stuck(Diagnostic.of(outcome));
            }
            case CombatEvaluations.Result.Failed failed -> stuck(Diagnostic.error(
                    "error: evaluation failed: " + failed.reason(),
                    "Your answer and its ticket are kept. RETRY sends the same answer again."));
        }
    }

    private void accepted(Outcome.Done done) {
        boolean wasReady = last != null && last.dasherReady();
        submitted = null;
        submittedWork = null;
        evaluationStuck = false;
        blocked = false;
        openQuestion = null;
        answer.clear();
        notice(notice, null);
        ctx.toasts().outcome(done);
        if (done.after().student().hp() < done.before().student().hp()) hit();
        refresh();
        if (last == null) return;
        if (last.complete()) {
            CutIn.play(cutins, 330, "ALL CLEAR", "EVERY QUESTION EVALUATED", "caret");
            commands.requestFocus();
            return;
        }
        if (last.dasherReady() && !wasReady)
            CutIn.play(cutins, 330, "DASHER READY", "THE LAST QUESTION YOU ANSWER GETS THE DASHER BONUS", "caret");
        targets.requestFocus();
    }

    private void stuck(Node card) {
        evaluationStuck = true;
        blocked = true;
        notice(notice, card);
        refresh();
        commands.requestFocus();
    }

    private void beginReward(CombatSession.RewardKind kind) {
        Fetch<CombatSession.RewardTicket> fetch = ctx.session().beginRewardDelay(kind);
        if (!fetch.ok()) {
            notice(notice, Diagnostic.of(fetch.failure()));
            commands.unlock();
            return;
        }
        reward = fetch.value();
        rewardRetryAt = 0;
        rewardNoticeShown = false;
        notice(notice, null);
        refresh();
    }

    /** The backend says the time is up; ask it to close the delay (an early call is refused). */
    private void finishReward() {
        Outcome outcome = ctx.session().finishRewardDelay(reward);
        if (outcome.ok()) {
            reward = null;
            ctx.toasts().show("REWARD TIME OVER  ·  NO ITEM EFFECT YET (T09)", "toast-warn");
            refresh();
            answer.requestFocus();
        } else {
            rewardRetryAt = System.nanoTime() + 250_000_000L;
            if (!rewardNoticeShown) notice(notice, Diagnostic.of(outcome));
            rewardNoticeShown = true;
        }
    }

    private void settle() {
        Outcome outcome = ctx.session().finishCombatAssessment();
        if (outcome instanceof Outcome.Done done) {
            leave();
            ctx.toasts().outcome(done);
            ctx.advance();
        } else {
            notice(notice, Diagnostic.of(outcome));
            blocked = true;
            showCommands(last);
        }
    }

    private ChoiceList.Choice quit() {
        return ChoiceList.Choice.of("QUIT TO TITLE", "Abandons this assessment  ·  last day-end save kept", () -> {
            leave();
            ctx.toTitle();
        }).tone("danger");
    }

    /** Stops reading the run: no more frames, and any evaluation in flight is dropped. */
    private void leave() {
        left = true;
        ticker.stop();
        if (evaluations != null) evaluations.close();
    }

    // ---- input and effects ------------------------------------------------------------

    private void onAnswerKey(KeyEvent e) {
        if (e.getCode() != KeyCode.TAB || e.isControlDown()) return;
        e.consume();
        if (e.isShiftDown()) targets.requestFocus();
        else if (commands.hasChoices()) commands.requestFocus();
        else targets.requestFocus();
    }

    /** The answer cost HP: a red flash and a short jolt of the HUD, where HP is shown. */
    private void hit() {
        new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(hitFlash.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(40), new KeyValue(hitFlash.opacityProperty(), 0.5)),
                new KeyFrame(Duration.millis(340), new KeyValue(hitFlash.opacityProperty(), 0, MotionTokens.OUT))).play();
        Timeline shake = new Timeline();
        double[] xs = {-12, 9, -6, 3, 0};
        for (int i = 0; i < xs.length; i++)
            shake.getKeyFrames().add(new KeyFrame(Duration.millis(45 * (i + 1)), new KeyValue(hud.translateXProperty(), xs[i])));
        shake.play();
    }

    // ---- helpers --------------------------------------------------------------------

    private String flagLine() {
        if (assessment == null) return "";
        return Stream.of(assessment.makeup() ? "MAKEUP" : null, assessment.peekingAllowed() ? null : "NO PEEKING",
                        assessment.increasedDetectionRisk() ? "HIGHER DETECTION RISK" : null)
                .filter(Objects::nonNull).collect(Collectors.joining("  ·  "));
    }

    private static String place(CourseType type) {
        return switch (type) {
            case LAB -> "computer-lab";
            case MIDTERM, FINAL -> "opening";
            default -> "classroom";
        };
    }

    private static CombatSession.QuestionView current(CombatSession.View view) {
        String id = view.currentQuestionId();
        if (id == null) return null;
        return view.questions().stream().filter(q -> q.id().equals(id)).findFirst().orElse(null);
    }

    private static int indexOf(List<CombatSession.QuestionView> questions, String id) {
        for (int i = 0; i < questions.size(); i++) if (questions.get(i).id().equals(id)) return i;
        return -1;
    }

    private static String answerLine(CombatSession.View view, String id) {
        return view.answers().stream().filter(a -> a.questionId().equals(id)).findFirst()
                .map(a -> CombatText.percent(a.earnedFraction()) + "  ·  " + CombatText.credits(a.weightedCredit()) + " credits"
                        + (a.dasher() ? "  ·  DASHER" : "") + (a.timedOut() ? "  ·  OVERTIME" : ""))
                .orElse("");
    }

    private static Label cell(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().addAll("mono", style);
        return label;
    }

    private static Node tagged(String text, String late, String dasher) {
        HBox box = new HBox(8, cell(text, "result-cell"));
        if (late != null) box.getChildren().add(cell(late, "result-tag-late"));
        if (dasher != null) box.getChildren().add(cell(dasher, "result-tag-dasher"));
        return box;
    }

    private static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static void toggle(Node node, String style, boolean on) {
        if (on && !node.getStyleClass().contains(style)) node.getStyleClass().add(style);
        else if (!on) node.getStyleClass().remove(style);
    }
}
