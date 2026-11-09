package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CourseType;
import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The frontend controller against the real core and the backend's Demo profile.
 * Values are read from the profile, never restated here.
 */
class GameSessionTest {
    @TempDir Path temp;
    private DemoProfile profile;
    private Path save;
    private GameSession session;

    @BeforeEach void start() throws IOException {
        profile = DemoProfile.load();
        save = temp.resolve("slot/day-end-save.properties");
        session = GameSession.newGame(profile, "Tester", new Random(7), save);
    }

    @Test void openingIsRecordedOnlyOnce() {
        assertTrue(session.completeOpening().ok());
        Outcome again = session.completeOpening();
        assertInstanceOf(Outcome.Rejected.class, again);
        assertTrue(session.view().openingCompleted());
    }

    @Test void lessonContentIsMarkedShownOnceAndRereadWithoutChange() {
        session.completeOpening();
        Fetch<?> first = session.openLesson();
        assertTrue(first.ok());
        assertTrue(session.view().lessonShown());
        String before = signature(session.view());
        Fetch<?> second = session.openLesson();
        assertEquals(first.value(), second.value());
        assertEquals(before, signature(session.view()));
    }

    @Test void aLessonChoiceSettlesOnceAndARepeatChangesNothing() {
        session.completeOpening();
        session.openLesson();
        Outcome first = session.lessonChoice("ask");
        Outcome.Done done = assertInstanceOf(Outcome.Done.class, first);
        assertEquals(DayPeriod.OTHER_CLASSES, done.after().period());
        String afterFirst = signature(session.view());

        Outcome repeat = session.lessonChoice("ask");
        assertInstanceOf(Outcome.Rejected.class, repeat);
        assertEquals(afterFirst, signature(session.view()));
    }

    @Test void choiceBeforeTheLessonIsShownIsRefused() {
        session.completeOpening();
        String before = signature(session.view());
        assertInstanceOf(Outcome.Rejected.class, session.lessonChoice("ask"));
        assertEquals(before, signature(session.view()));
    }

    @Test void hubActivitySpendsExactlyOneSlotWithConfiguredEffect() {
        toAfterSchool();
        GameState before = session.view();
        Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.hubActivity("rest"));
        assertEquals(DayPeriod.EVENING, done.after().period());
        double configured = profile.content().hub("rest").san();
        assertEquals(before.student().san() + configured, done.after().student().san(), 1e-9);
    }

    @Test void unknownActivityIsReportedAndSpendsNoTime() {
        toAfterSchool();
        String before = signature(session.view());
        Outcome outcome = session.hubActivity("no-such-activity");
        assertFalse(outcome.ok());
        assertEquals(before, signature(session.view()));
    }

    @Test void saveIsRefusedBeforeTheDayEnds() {
        session.completeOpening();
        Outcome outcome = session.saveDayEnd();
        assertInstanceOf(Outcome.Rejected.class, outcome);
        assertFalse(Files.exists(save));
    }

    @Test void dayEndSaveLoadsTheSameClosedDayWithoutRepeatingRewards() throws IOException {
        toAfterSchool();
        session.hubActivity("study");
        assertTrue(session.sleepNormally().ok());
        GameState closed = session.view();
        assertEquals(DayPeriod.DAY_END, closed.period());
        assertTrue(session.saveDayEnd().ok());

        GameSession loaded = GameSession.load(profile, save, new Random(7));
        GameState restored = loaded.view();
        assertEquals(1, restored.day());
        assertEquals(DayPeriod.DAY_END, restored.period(), "loading must not advance to the next day");
        assertEquals(closed.student(), restored.student(), "loading must not re-apply rewards");

        Outcome.Done next = assertInstanceOf(Outcome.Done.class, loaded.beginNextDay());
        assertEquals(2, next.after().day());
        assertEquals(DayPeriod.ICS_CLASS, next.after().period());
    }

    @Test void sleepingRightAfterSchoolGivesAHalfBonusSlotWithHalfEffects() {
        toAfterSchool();
        assertTrue(session.sleepAfterSchool().ok());
        Outcome.Done morning = assertInstanceOf(Outcome.Done.class, session.beginNextDay());
        assertEquals(DayPeriod.MORNING_BONUS, morning.after().period());

        GameState before = session.view();
        Outcome.Done rest = assertInstanceOf(Outcome.Done.class, session.hubActivity("rest"));
        double half = profile.content().hub("rest").san() * DayPeriod.MORNING_BONUS.activityUnits();
        assertEquals(before.student().san() + half, rest.after().student().san(), 1e-9);
        assertEquals(DayPeriod.ICS_CLASS, rest.after().period());
    }

    @Test void theDay5LabReportsItsPendingRuleAndChangesNothing() {
        session.completeOpening();
        for (int day = 1; day <= 4; day++) playOrdinaryDay();
        GameState day5 = session.view();
        assertEquals(5, day5.day());
        assertEquals(CourseType.LAB, day5.currentCourse().course());

        String before = signature(day5);
        Fetch<?> lab = session.startAssessment();
        Outcome.Pending pending = assertInstanceOf(Outcome.Pending.class, lab.failure());
        assertEquals("T07", pending.ruleId());
        assertFalse(session.view().hasActiveAssessment());
        assertEquals(before, signature(session.view()));
    }

    @Test void previewsComeFromTheProfile() {
        assertEquals(profile.content().hubActivityIds(), session.hubActivityIds());
        assertEquals(profile.content().otherRevision(), session.revisionEffect().value());
        assertFalse(session.hubEffect("no-such-activity").ok());
    }

    private void toAfterSchool() {
        session.completeOpening();
        session.openLesson();
        session.lessonChoice("quiet");
        session.otherClasses(false);
        assertEquals(DayPeriod.AFTER_SCHOOL, session.view().period());
    }

    private void playOrdinaryDay() {
        assertTrue(session.openLesson().ok());
        assertTrue(session.lessonChoice("ask").ok());
        assertTrue(session.otherClasses(true).ok());
        assertTrue(session.hubActivity("study").ok());
        assertTrue(session.sleepNormally().ok());
        assertTrue(session.saveDayEnd().ok());
        assertTrue(session.beginNextDay().ok());
    }

    private static String signature(GameState s) {
        return String.join("|", List.of(String.valueOf(s.day()), s.period().name(), s.student().toString(),
                String.valueOf(s.effectiveSan()), s.effects().toString(), String.valueOf(s.lessonShown()),
                String.valueOf(s.hasActiveAssessment()), String.valueOf(s.hasMorningBonus()),
                s.grades().records().toString(), s.status().name()));
    }
}
