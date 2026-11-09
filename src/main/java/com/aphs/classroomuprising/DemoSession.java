package com.aphs.classroomuprising;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/** Small GUI adapter: the existing controller remains the owner of gameplay state changes. */
public final class DemoSession {
    private GameController game;
    private final ContentCatalog content = DemoScenario.content();
    private GameController.AssessmentView assessment;
    public DemoSession(boolean labShortcut) { replace(DemoScenario.initial(labShortcut)); }
    private void replace(GameState state) {
        if (state.day() > DemoScenario.LAST_DAY || state.status() != GameState.Status.ACTIVE
                || !state.makeups().isEmpty() || !state.missedAssessments().isEmpty())
            throw new IllegalArgumentException("This GUI demo supports Days 1-8 without makeup sessions.");
        game = new GameController(state, DemoScenario.rules(), content, new Random(7));
        prepare();
    }
    private void prepare() {
        assessment = null;
        var s = game.snapshot();
        if (!s.openingCompleted() || s.period() != DayPeriod.ICS_CLASS) return;
        if (s.currentCourse().course() == CourseType.LESSON) game.showLesson();
        else assessment = game.startAssessment();
    }
    public GameState state() { return game.snapshot(); }
    public ContentCatalog.Lesson lesson() { return content.lesson(state().day()); }
    public GameController.AssessmentView assessment() { return assessment; }
    public String act(String action) {
        GameController.Result result = switch (action) {
            case "opening" -> game.completeOpening();
            case "participate", "listen" -> game.lessonChoice(action);
            case "other-focus" -> game.otherClasses(false);
            case "other-revise" -> game.otherClasses(true);
            case "study", "relax" -> game.hubActivity(action);
            case "sleep" -> game.sleepImmediatelyAfterSchool();
            case "next" -> {
                if (state().day() >= DemoScenario.LAST_DAY) throw new IllegalStateException("Demo complete. The remaining calendar is not playable in this slice.");
                yield game.beginNextDay();
            }
            default -> throw new IllegalArgumentException("Unknown demo action.");
        };
        prepare();
        return message(result);
    }
    public String answerQuiz(String answer) {
        if (assessment == null || assessment.type() != CourseType.UNIT_TEST)
            throw new IllegalStateException("No demo quiz is active.");
        if (!java.util.List.of("3", "3.5", "4").contains(answer)) throw new IllegalArgumentException("Choose a listed answer.");
        var result = game.recordEvaluatedSubmission("3".equals(answer) ? 100 : 0, "DEMO: one fixed integer-division question; answer=" + answer);
        assessment = null;
        return message(result) + " Official mark returns on Day 10 (after this demo ends).";
    }
    public String submitLab(JavaAssessmentRunner.Report report) {
        if (assessment == null || assessment.type() != CourseType.LAB) throw new IllegalStateException("No demo Lab is active.");
        var result = game.recordEvaluatedSubmission(DemoScenario.labMark(report), "DEMO rubric 80% cases + 20% line length; " + report);
        assessment = null;
        return message(result) + " Official mark returns on Day 8. HP is unchanged.";
    }
    public void save(Path file) throws IOException { new SaveManager().saveAtDayEnd(game.snapshot(), file); }
    public void load(Path file) throws IOException {
        // Validate a candidate session first, so a bad/unsupported save never destroys the current run.
        var loaded = new SaveManager().load(file);
        var candidate = new DemoSession(false);
        candidate.replace(loaded);
        game = candidate.game; assessment = candidate.assessment;
    }
    private static String message(GameController.Result result) {
        return result.message() + (result.warnings().isEmpty() ? "" : "\n" + String.join("\n", result.warnings()));
    }
}
