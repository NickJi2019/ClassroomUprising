package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class GuiDemoTest {
    @TempDir Path temp;
    private static void lesson(DemoSession s) { s.act("participate"); s.act("other-focus"); }
    private static void next(DemoSession s) { s.act("sleep"); s.act("next"); s.act("relax"); }
    private static JavaAssessmentRunner.Report report(int exit, JavaAssessmentRunner.Status status, boolean style) {
        var process = new JavaAssessmentRunner.ProcessEvidence(exit, false, false, "", "");
        return new JavaAssessmentRunner.Report(process,
                List.of(new JavaAssessmentRunner.TestResult("a", status, process)),
                List.of(new StyleRule.Finding("line", style, "fixture")));
    }
    @Test void startsWithOpeningAndNoInventedGrade() {
        var s = new DemoSession(false); assertFalse(s.state().openingCompleted());
        assertEquals(10, s.state().student().hp()); assertTrue(s.state().grades().records().isEmpty());
        assertThrows(IllegalStateException.class, () -> s.act("participate"));
    }
    @Test void openingShowsExactlyOneChoiceAndTutorialWaivesSanLoss() {
        var s = new DemoSession(false); s.act("opening"); String message = s.act("participate");
        assertEquals(20, s.state().effectiveSan()); assertEquals(4, s.state().student().codingVibe());
        assertTrue(message.contains("warnings only")); assertEquals(DayPeriod.OTHER_CLASSES, s.state().period());
        assertThrows(IllegalStateException.class, () -> s.act("listen"));
    }
    @Test void earlySleepSaveLoadHalfSlot() throws Exception {
        var s = new DemoSession(false); s.act("opening"); lesson(s); s.act("sleep");
        Path file = temp.resolve("save.properties"); s.save(file); s.act("next"); s.load(file);
        assertEquals(1, s.state().day()); assertEquals(DayPeriod.DAY_END, s.state().period());
        s.act("next"); assertEquals(23, s.state().effectiveSan()); s.act("study");
        assertEquals(6, s.state().student().codingVibe()); assertEquals(DayPeriod.ICS_CLASS, s.state().period());
    }
    @Test void saveDuringClassRejectedAndCreatesNoFile() {
        var s = new DemoSession(false); Path file = temp.resolve("bad.properties");
        assertThrows(IllegalStateException.class, () -> s.save(file)); assertFalse(Files.exists(file));
    }
    @Test void malformedLoadLeavesCurrentSessionUntouched() throws Exception {
        var s = new DemoSession(true); Path file = temp.resolve("bad.properties"); Files.writeString(file, "wrong=true");
        assertThrows(Exception.class, () -> s.load(file)); assertEquals(5, s.state().day()); assertNotNull(s.assessment());
    }
    @Test void outOfSliceSaveRejectedWithoutReplacingRun() throws Exception {
        var state = DemoScenario.initial(true); state.day = 9; state.period = DayPeriod.DAY_END;
        Path file = temp.resolve("future.properties"); new SaveManager().saveAtDayEnd(state, file);
        var s = new DemoSession(false); assertThrows(IllegalArgumentException.class, () -> s.load(file));
        assertEquals(1, s.state().day()); assertFalse(s.state().openingCompleted());
    }
    @Test void labShortcutStartsActualDayFiveAssessment() {
        var s = new DemoSession(true); assertEquals(5, s.state().day()); assertEquals("day-5-lab", s.assessment().id());
        assertTrue(s.state().grades().records().isEmpty()); assertFalse(s.state().canSave());
    }
    @Test void noSubmissionWithoutAssessment() {
        assertThrows(IllegalStateException.class, () -> new DemoSession(false).submitLab(report(0, JavaAssessmentRunner.Status.PASSED, true)));
    }
    @Test void demoRubricPassed() { assertEquals(100, DemoScenario.labMark(report(0, JavaAssessmentRunner.Status.PASSED, true))); }
    @Test void demoRubricWrongAnswerKeepsOnlyStyle() { assertEquals(20, DemoScenario.labMark(report(0, JavaAssessmentRunner.Status.WRONG_OUTPUT, true))); }
    @Test void demoRubricStyleFailure() { assertEquals(80, DemoScenario.labMark(report(0, JavaAssessmentRunner.Status.PASSED, false))); }
    @Test void demoRubricCompileFailureZero() { assertEquals(0, DemoScenario.labMark(report(1, JavaAssessmentRunner.Status.COMPILE_ERROR, true))); }
    @Test void labReturnQuizAndDemoBoundary() {
        var s = new DemoSession(true); s.submitLab(report(0, JavaAssessmentRunner.Status.PASSED, true));
        assertTrue(s.state().grades().record("day-5-lab").officialScore().isEmpty());
        assertThrows(IllegalStateException.class, () -> s.submitLab(report(0, JavaAssessmentRunner.Status.PASSED, true)));
        s.act("other-focus"); next(s); lesson(s); next(s);
        assertEquals(7, s.state().day()); assertThrows(IllegalArgumentException.class, () -> s.answerQuiz("not an option"));
        assertEquals(DayPeriod.ICS_CLASS, s.state().period()); s.answerQuiz("3");
        s.act("other-focus"); next(s); assertEquals(100, s.state().grades().record("day-5-lab").officialScore().orElseThrow());
        assertTrue(s.state().grades().record("day-7-unit_test").officialScore().isEmpty());
        assertEquals(10, s.state().student().hp()); lesson(s); s.act("sleep");
        assertThrows(IllegalStateException.class, () -> s.act("next")); assertEquals(8, s.state().day());
    }
    @Test void normalRouteActuallyReachesLab() {
        var s = new DemoSession(false); s.act("opening");
        for (int day = 1; day <= 4; day++) { lesson(s); next(s); }
        assertEquals(5, s.state().day()); assertEquals(CourseType.LAB, s.assessment().type());
    }
    @Test void tutorialLateNightDoesNotScheduleNextDayDebuff() {
        var s = new DemoSession(false); s.act("opening"); lesson(s);
        s.act("study"); s.act("study"); String warning = s.act("study");
        assertTrue(warning.contains("no next-day")); s.act("next"); assertEquals(1, s.state().xpMultiplier());
    }
    @Test void ordinaryLateNightUsesNextDayOnly() {
        var s = new DemoSession(false); s.act("opening"); lesson(s); next(s); lesson(s); next(s); lesson(s);
        s.act("study"); s.act("study"); s.act("study"); assertEquals(1, s.state().xpMultiplier());
        s.act("next"); assertEquals(.5, s.state().xpMultiplier());
        assertEquals(s.state().student().san() - 4, s.state().effectiveSan());
    }
    @Test void viewMustBeCreatedOnEdt() { assertThrows(IllegalStateException.class, () -> new GameWindow(temp.resolve("x"))); }
    @Test void realButtonsAdvanceAndSaveOnlyAtDayEnd() throws Exception {
        var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                var ui = new GameWindow(temp.resolve("gui.properties")); assertFalse(button(ui, "save").isEnabled());
                button(ui, "opening").doClick(0); button(ui, "participate").doClick(0);
                assertNull(find(ui, "listen")); button(ui, "other-focus").doClick(0); button(ui, "sleep").doClick(0);
                assertTrue(button(ui, "save").isEnabled()); button(ui, "save").doClick(0);
                assertTrue(Files.isRegularFile(temp.resolve("gui.properties"))); button(ui, "next").doClick(0);
                button(ui, "load").doClick(0); assertEquals(1, ui.session().state().day());
                assertEquals(DayPeriod.DAY_END, ui.session().state().period());
            } catch (Throwable e) { failure.set(e); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    @Test void editorStartsWithSubmitDisabled() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var ui = new GameWindow(temp.resolve("x")); button(ui, "lab-shortcut").doClick(0);
            assertFalse(button(ui, "submit-lab").isEnabled()); assertTrue(button(ui, "run-lab").isEnabled());
            assertTrue(((JTextArea)find(ui, "source-editor")).getText().contains("a - b"));
        });
    }
    private static JButton button(Container root, String name) { return (JButton) java.util.Objects.requireNonNull(find(root, name), name); }
    private static Component find(Container root, String name) {
        for (var c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container child) { var found = find(child, name); if (found != null) return found; }
        }
        return null;
    }
}
