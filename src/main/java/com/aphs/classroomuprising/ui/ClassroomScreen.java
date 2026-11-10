package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.CourseType;
import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.GameController;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.LessonDecision;
import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.fxml.FXML;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The ICS4U slot. A Lesson shows its content once, then offers its single choice node.
 * While Mr. Reward lectures, the slides his lines cue ({@code [slide:name]}) appear on a
 * projector panel, and the dialogue box carries what he says about each one.
 * After the lecture comes the lesson's decision in two menus ({@link LessonMenu}): take part or
 * stay quiet, then one of the authored responses. Only the final choice settles the class, once,
 * for the lesson day captured when the lecture opened; the authored feedback follows.
 * Holding Enter, Space or the mouse fast-forwards the lecture (never into a choice); H or
 * scrolling up opens the log, which replays any earlier line with its slide.
 * Review days go to the matching core operation; assessment days start a combat
 * ({@link CombatScreen}). When that hits a pending rule the notice says so and the player can
 * return to the title with the save intact.
 */
final class ClassroomScreen extends Screen {
    @FXML private SceneView scene;
    @FXML private Label prompt;
    @FXML private ChoiceList choices;
    @FXML private VBox notice;
    @FXML private DialogueBox dialogue;
    @FXML private Hud hud;
    private SlideProjector slides;
    /** The day whose lesson this screen opened; every settlement names it. */
    private int lessonDay;
    private ContentCatalog.Lesson lesson;
    /** Whether the response menu (second level) is showing, so Backspace can go back. */
    private boolean responding;
    private DialogueLog log;

    ClassroomScreen(AppContext ctx) { super(ctx); }

    @Override
    protected Parent build() {
        StackPane root = Views.load("classroom.fxml", this);
        ctx.stage(scene, "classroom", null, "ics_class");
        root.getChildren().add(1, new Backdrop(
                new Backdrop.Glyph("private int sanity;", 330, 150, 62, false),
                new Backdrop.Glyph("ICS4U", 640, 300, 170, true)));
        dialogue.setOnLine(line -> {
            scene.speak(line.characterId(), line.expression());
            if (slides != null && line.slide() != null) slides.show(line.slide());
        });
        log = new DialogueLog(ctx.assets());
        root.getChildren().add(log);
        dialogue.attachKeys(root);
        if (ctx.router() != null) dialogue.followFastForward(ctx.router().fastForward());
        dialogue.setAdvanceAllowed(() -> (slides == null || !slides.zoomed()) && !log.isOpen());
        dialogue.setOnHistory(this::openLog);
        root.setOnKeyPressed(e -> {
            if (slides != null && slides.zoomed()) {
                // The slide is open large: the lecture waits, and any of these keys (or Esc) closes it.
                switch (e.getCode()) {
                    case Z, ENTER, SPACE, BACK_SPACE -> {
                        slides.unzoom();
                        e.consume();
                    }
                    default -> { }
                }
            } else if (e.getCode() == KeyCode.H) {
                openLog();
                e.consume();
            } else if (e.getCode() == KeyCode.Z && slides != null) {
                slides.toggleZoom();
                e.consume();
            } else if (e.getCode() == KeyCode.BACK_SPACE && responding) {
                participation();
                e.consume();
            }
        });
        return root;
    }

    @Override
    protected boolean allowsPause() { return true; }

    /** Esc, one level back: the log (its large slide first), a large slide, the response menu. */
    @Override
    protected boolean back() {
        if (log != null && log.isOpen()) return log.back();
        if (slides != null && slides.zoomed()) slides.unzoom();
        else if (responding) participation();
        else return false;
        return true;
    }

    /** The lecture is playing (and not waiting on the log or a large slide). */
    @Override
    protected boolean isPlaying() {
        return dialogue.isVisible() && !dialogue.isFinished()
                && (slides == null || !slides.zoomed()) && (log == null || !log.isOpen());
    }

    @Override
    protected String music() { return "school"; }

    @Override
    protected void focus() {
        if (log != null && log.isOpen()) log.requestFocus();
        else if (choices.hasChoices()) choices.requestFocus();
        else root().requestFocus();
    }

    @Override
    protected void onShown() {
        GameState view = ctx.session().view();
        hud.show(view, ctx.demoLabel());
        if (ctx.assets() != null) scene.enter("reward", 0.24, "neutral", Duration.millis(120));
        DayPlan plan = view.currentCourse();
        boolean makeup = view.makeups().containsKey(view.day());
        if (plan.course() == CourseType.LESSON && !makeup) lesson();
        else if (plan.course() == CourseType.REVIEW) review();
        else assessment(plan, makeup);
    }

    private void lesson() {
        Fetch<ContentCatalog.Lesson> fetch = ctx.session().openLesson();
        if (!fetch.ok()) {
            blocked(fetch.failure());
            return;
        }
        lessonDay = ctx.session().view().day();
        lesson = fetch.value();
        hud.setCourseTitle(lesson.title());
        List<DialogueScript.Line> script = ctx.script(lesson.text());
        projector(SlideProjector.cues(script));
        var keys = new java.util.ArrayList<>(List.of(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"), KeyHints.Hint.of("H", "LOG")));
        if (slides != null) keys.add(KeyHints.Hint.of("Z", "SLIDE"));
        keys.add(KeyHints.Hint.of("ESC", "MENU"));
        hud.setHints(keys.toArray(KeyHints.Hint[]::new));
        dialogue.play(script, this::offer);
        root().requestFocus();
    }

    /**
     * Puts a projector for the lecture's slides on top of the classroom and HUD, if the lecture
     * cues any (on top, so the HUD's shading never dims them and the large view can cover the
     * whole screen).
     */
    private void projector(List<String> cued) {
        if (ctx.assets() == null || cued.isEmpty()) return;
        slides = new SlideProjector(ctx.assets(), cued);
        slides.setOnLargeView(on -> {
            hud.hideHints(on);   // the large view has its own key hints
            if (on) hud.collapse();
        });
        StackPane root = (StackPane) root();
        root.getChildren().add(root.getChildren().indexOf(log), slides);   // the log stays on top
    }

    private void offer() {
        if (slides != null) slides.close();
        ctx.tip(TutorialTips.Moment.LESSON_CHOICE);
        if (lesson.decision() == null) flatChoices();
        else {
            // A question the teacher asks the class is his line: it stays in the dialogue box.
            if (LessonMenu.spoken(lesson.decision()))
                dialogue.say(LessonMenu.withExpression(ctx.script(lesson.decision().prompt()).get(0), LessonMenu.ASKING));
            participation();
        }
    }

    /** Opens the log on the newest line; the lecture (or the menu) waits until it closes. */
    private void openLog() {
        if (dialogue.history().isEmpty() || log.isOpen()) return;
        hud.collapse();
        ctx.router().hideTips(true);
        log.open(dialogue.history(), () -> {
            ctx.router().hideTips(false);
            focus();
        });
    }

    /** First menu: take part or stay quiet. Showing it (again) changes nothing in the game. */
    private void participation() {
        responding = false;
        hud.setHints(KeyHints.Hint.of("H", "LOG"), KeyHints.Hint.of("ESC", "MENU"));
        notice(notice, null);
        prompt.setText(UiText.interactionPrompt(lesson.interaction()));
        choices.getStyleClass().remove("sentences");
        choices.setChoices(LessonMenu.first(lesson).stream().map(entry -> entry.opensResponses()
                ? ChoiceList.Choice.of(entry.label(), null, this::responses)
                : ChoiceList.Choice.of(entry.label(), null, () -> settle(entry.option()))).toList());
        choices.requestFocus();
    }

    /** Second menu: the authored responses. Opening it changes nothing in the game. */
    private void responses() {
        responding = true;
        hud.setHints(KeyHints.Hint.of("H", "LOG"), KeyHints.Hint.of("ESC", "BACK"));
        notice(notice, null);
        prompt.setText(LessonMenu.responsePrompt(lesson));
        if (!choices.getStyleClass().contains("sentences")) choices.getStyleClass().add("sentences");
        var list = new java.util.ArrayList<ChoiceList.Choice>();
        for (LessonMenu.Entry entry : LessonMenu.responses(lesson.decision()))
            list.add(ChoiceList.Choice.of(entry.label(), null, () -> settle(entry.option())));
        list.add(ChoiceList.Choice.of("BACK", "Esc", this::participation).tone("cyan"));
        choices.setChoices(list);
        choices.requestFocus();
    }

    /** The one settlement of this class: the chosen response or staying quiet. */
    private void settle(LessonDecision.Option option) {
        Outcome outcome = ctx.session().lessonChoice(lessonDay, option.id());
        if (!(outcome instanceof Outcome.Done done)) {
            notice(notice, Diagnostic.of(outcome));
            choices.unlock();
            return;
        }
        responding = false;
        ctx.toasts().deltas(done);
        hud.show(done.after(), ctx.demoLabel());
        VBox after = new VBox(10);
        String reaction = LessonMenu.reaction(option.outcome());
        boolean quiet = option.outcome() == LessonDecision.Outcome.SKIP;
        if (quiet) {
            Label narration = new Label(done.message());
            narration.getStyleClass().add("lesson-narration");
            narration.setWrapText(true);
            after.getChildren().add(narration);
        } else {
            // The teacher's reply to your answer or question.
            dialogue.say(LessonMenu.withExpression(reply(done.message()), reaction));
        }
        for (String warning : done.warnings()) after.getChildren().add(Diagnostic.warning("TUTORIAL", warning));
        Runnable end = () -> {
            notice(notice, after.getChildren().isEmpty() ? null : after);
            dismissed();
        };
        if (quiet) classmateTurn(LessonMenu.classmateAnswer(lesson.decision()), end);
        else end.run();
    }

    /**
     * You stayed quiet, so a classmate raises a hand (or asks) and gives the right response, and
     * Mr. Reward answers them. Only shown: the class is already settled as quiet. The menu
     * waits until the exchange is over; it can be fast-forwarded and read again in the log.
     */
    private void classmateTurn(LessonDecision.Option answer, Runnable then) {
        prompt.setText("");
        choices.setChoices(List.of());
        notice(notice, null);
        hud.setHints(KeyHints.Hint.of("HOLD ENTER / MOUSE", "FAST"), KeyHints.Hint.of("H", "LOG"), KeyHints.Hint.of("ESC", "MENU"));
        dialogue.play(List.of(
                new DialogueScript.Line("Narrator", UiText.classmateTurn(lesson.interaction())),
                new DialogueScript.Line(LessonMenu.CLASSMATE, answer.label()),
                LessonMenu.withExpression(reply(answer.feedback()), LessonMenu.reaction(answer.outcome()))), then);
        root().requestFocus();
    }

    private DialogueScript.Line reply(String message) {
        boolean hasSpeaker = DialogueScript.parse(message, null).stream().findFirst()
                .map(line -> line.speaker() != null).orElse(false);
        return ctx.script(hasSpeaker ? message : "Mr. Reward: " + message).get(0);
    }

    private void dismissed() {
        hud.setHints(KeyHints.Hint.of("H", "LOG"), KeyHints.Hint.of("ESC", "MENU"));
        prompt.setText("CLASS DISMISSED");
        choices.getStyleClass().remove("sentences");
        choices.setChoices(List.of(ChoiceList.Choice.of("CONTINUE", "Go to your other class", ctx::advance)));
        choices.requestFocus();
    }

    /** A lesson without an authored decision (older content): its choices in one list, as before. */
    private void flatChoices() {
        hud.setHints(KeyHints.Hint.of("H", "LOG"), KeyHints.Hint.of("ESC", "MENU"));
        prompt.setText(UiText.interactionPrompt(lesson.interaction()));
        choices.setChoices(UiText.choiceOrder(lesson.choices().keySet()).stream()
                .map(id -> ChoiceList.Choice.of(UiText.lessonChoice(id), null, () -> choose(id)))
                .toList());
        choices.requestFocus();
    }

    private void choose(String choiceId) {
        Outcome outcome = ctx.session().lessonChoice(lessonDay, choiceId);
        if (outcome instanceof Outcome.Done done) {
            ctx.toasts().outcome(done);
            hud.show(done.after(), ctx.demoLabel());
            notice(notice, null);
            dismissed();
        } else {
            notice(notice, Diagnostic.of(outcome));
            choices.unlock();
        }
    }

    private void review() {
        noLecture();
        prompt.setText("TODAY  ·  " + UiText.courseLine(ctx.session().view().currentCourse()));
        choices.setChoices(List.of(ChoiceList.Choice.of("START REVIEW", null, () -> {
            Outcome outcome = ctx.session().completeReview();
            if (outcome instanceof Outcome.Done done) {
                ctx.toasts().outcome(done);
                ctx.advance();
            } else {
                blocked(outcome);
            }
        })));
    }

    private void assessment(DayPlan plan, boolean makeup) {
        String name = (makeup ? "MAKEUP " : "") + UiText.course(plan.course());
        noLecture();
        prompt.setText("TODAY  ·  " + (makeup ? "MAKEUP " : "") + UiText.courseLine(plan));
        choices.setChoices(List.of(ChoiceList.Choice.of("BEGIN " + name, null, this::beginAssessment)));
    }

    /** Assessments are fought as a combat; a pending bank or policy is shown here instead. */
    private void beginAssessment() {
        Fetch<GameController.AssessmentView> fetch = ctx.session().startCombatAssessment();
        if (!fetch.ok()) {
            blocked(fetch.failure());
            return;
        }
        ctx.openCombat(fetch.value());
    }

    /** No one is speaking: the dialogue box is for characters, not for schedule notices. */
    private void noLecture() {
        dialogue.setVisible(false);
        dialogue.setManaged(false);
    }

    /** The class cannot go on: show why, and offer the title screen (the day-end save is kept). */
    private void blocked(Outcome reason) {
        notice(notice, Diagnostic.of(reason));
        prompt.setText("END OF DEMO CONTENT");
        choices.setChoices(List.of(returnToTitle()));
        choices.requestFocus();
    }

    private ChoiceList.Choice returnToTitle() {
        return ChoiceList.Choice.of("RETURN TO TITLE", "Your last day-end save is kept", ctx::toTitle).tone("cyan");
    }
}
