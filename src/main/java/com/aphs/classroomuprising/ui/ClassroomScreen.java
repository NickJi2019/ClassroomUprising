package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.CourseType;
import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.GameController;
import com.aphs.classroomuprising.GameState;
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
        root.setOnKeyPressed(e -> {
            if (slides != null && slides.zoomed()) {
                // The slide is open large: the lecture waits, and any of these keys closes it.
                switch (e.getCode()) {
                    case Z, ENTER, SPACE, BACK_SPACE, ESCAPE -> {
                        slides.unzoom();
                        e.consume();
                    }
                    default -> { }
                }
            } else if ((e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) && !dialogue.isFinished()) {
                dialogue.advance();
                e.consume();
            } else if (e.getCode() == KeyCode.Z && slides != null) {
                slides.toggleZoom();
                e.consume();
            }
        });
        return root;
    }

    /** Esc closes an enlarged slide first; otherwise it opens the menu. */
    @Override
    protected boolean allowsPause() { return slides == null || !slides.zoomed(); }

    @Override
    protected String music() { return "school"; }

    @Override
    protected void focus() {
        if (choices.hasChoices()) choices.requestFocus();
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
        ContentCatalog.Lesson lesson = fetch.value();
        List<DialogueScript.Line> script = ctx.script(lesson.text());
        projector(SlideProjector.cues(script));
        dialogue.play(script, () -> offer(lesson));
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
        ((StackPane) root()).getChildren().add(slides);
        hud.setHint("Z  SLIDE     ESC  MENU");
    }

    private void offer(ContentCatalog.Lesson lesson) {
        if (slides != null) {
            slides.close();
            hud.setHint("ESC  MENU");
        }
        prompt.setText(UiText.interactionPrompt(lesson.interaction()));
        ctx.tip(TutorialTips.Moment.LESSON_CHOICE);
        choices.setChoices(UiText.choiceOrder(lesson.choices().keySet()).stream()
                .map(id -> ChoiceList.Choice.of(UiText.lessonChoice(id), null, () -> choose(id)))
                .toList());
        choices.requestFocus();
    }

    private void choose(String choiceId) {
        Outcome outcome = ctx.session().lessonChoice(choiceId);
        if (outcome instanceof Outcome.Done done) {
            ctx.toasts().outcome(done);
            hud.show(done.after(), ctx.demoLabel());
            notice(notice, null);
            prompt.setText("CLASS DISMISSED");
            choices.setChoices(List.of(ChoiceList.Choice.of("CONTINUE", "Go to your other class", ctx::advance)));
            choices.requestFocus();
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
