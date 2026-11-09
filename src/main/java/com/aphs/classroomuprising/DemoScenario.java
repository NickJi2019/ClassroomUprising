package com.aphs.classroomuprising;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/** Presentation fixtures only. These values and questions are NOT the production rule set. */
public final class DemoScenario {
    public static final int LAST_DAY = 8;
    public static final String LAB_STARTER = """
            public class Sum {
                public static void main(String[] args) {
                    var input = new java.util.Scanner(System.in);
                    int a = input.nextInt();
                    int b = input.nextInt();
                    // Fix the operator: the task asks for a sum.
                    System.out.println(a - b);
                }
            }
            """;
    private DemoScenario() {}
    public static RuleValues rules() {
        return new RuleValues(4.0, .5, 3.0, 7.0, 12.0, RuleValues.TutorialAbsences.IGNORE);
    }
    public static ContentCatalog content() {
        var lessons = new LinkedHashMap<Integer, ContentCatalog.Lesson>();
        String[] text = {
            "A new semester. The same classroom. Another chance.\n\nMr. Reward writes a question on the board: What does a variable actually store? Read the lesson, then make ONE classroom choice.",
            "A program follows instructions in order. Predict the result before running the code.\n\nToday you can raise your hand OR stay quiet. This is still a tutorial day: negative effects produce warnings, not penalties.",
            "Two int values can be added with +. The expression a + b is evaluated before println prints it.\n\nMr. Reward pauses. This time, asking a question takes a little confidence. Tutorial protection has ended.",
            "Scanner reads input; println writes output. Extra labels in stdout can make an exact-output test fail.\n\nTomorrow's Lab asks for a complete Java program, not a single expression.",
            "A good test includes more than the happy path. Try positive numbers, a negative number, and zero.\n\nYour Lab result is still being marked. It returns three days after submission.",
            "Your first Lab result returns today if you submitted on Day 5. Check the gradebook on the right.\n\nA new chapter begins. This short demo ends after today's activities; the full calendar still has 40 days."
        };
        int[] days = {1, 2, 3, 4, 6, 8};
        for (int i = 0; i < days.length; i++) {
            int day = days[i];
            lessons.put(day, new ContentCatalog.Lesson(text[i], day % 2 == 0
                    ? ContentCatalog.Interaction.RAISE_HAND : ContentCatalog.Interaction.ASK_TEACHER,
                    Map.of("participate", StatChange.learning(day == 8 ? 2 : 1, 4, 2, -2),
                           "listen", StatChange.learning(day == 8 ? 2 : 1, 2, 1, 0))));
        }
        return new ContentCatalog(lessons, Map.of(
                "study", StatChange.learning(1, 4, 2, -2),
                "relax", new StatChange(0, 4, 0, Collections.nCopies(6, 0.0))), Map.of(), Map.of(
                "day-5-lab", List.of(new ContentCatalog.Question("sum", "Read two integers. Print their sum, followed by a newline. Use public class Sum.")),
                "day-7-unit_test", List.of(new ContentCatalog.Question("int-division", "What does System.out.println(7 / 2); print when both operands are int?"))),
                Map.of(), StatChange.learning(1, 2, 1, -2));
    }
    public static GameState initial(boolean labShortcut) {
        var state = new GameState("Demo student", new StudentState(10, 20, 0, Collections.nCopies(6, 0.0)));
        if (labShortcut) { state.day = 5; state.openingCompleted = true; }
        return state;
    }
    public static JavaAssessmentRunner.Report evaluateLab(String source) throws Exception {
        var runner = new JavaAssessmentRunner(Path.of(System.getProperty("java.home")),
                new JavaAssessmentRunner.Limits(Duration.ofSeconds(15), Duration.ofSeconds(3), 16384));
        return runner.evaluate("Sum", source, List.of(
                new JavaAssessmentRunner.TestCase("positive", "2 3\n", "5\n"),
                new JavaAssessmentRunner.TestCase("negative", "-4 1\n", "-3\n"),
                new JavaAssessmentRunner.TestCase("zero", "0 0\n", "0\n")),
                List.of(new StyleRule.LineLength("line-length <= 100", 100)));
    }
    /** Explicit demo rubric: 80% exact-output cases, 20% line-length rule; compile failure = 0. */
    public static double labMark(JavaAssessmentRunner.Report report) {
        var compile = report.compilation();
        if (compile.exitCode() != 0 || compile.timedOut() || compile.outputLimit()) return 0;
        return 80.0 * report.tests().stream().filter(t -> t.status() == JavaAssessmentRunner.Status.PASSED).count() / report.tests().size()
                + 20.0 * report.style().stream().filter(StyleRule.Finding::passed).count() / report.style().size();
    }
}
