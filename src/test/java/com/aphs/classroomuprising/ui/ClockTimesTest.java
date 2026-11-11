package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DemoProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Display clock for the time-of-day transition, and when the clock (not the wipe) is used. */
class ClockTimesTest {
    @TempDir Path temp;

    /** Every period change the core can make within one day. */
    private static final List<DayPeriod[]> MOVES = List.of(
            new DayPeriod[] {DayPeriod.MORNING_BONUS, DayPeriod.ICS_CLASS},
            new DayPeriod[] {DayPeriod.ICS_CLASS, DayPeriod.OTHER_CLASSES},
            new DayPeriod[] {DayPeriod.OTHER_CLASSES, DayPeriod.AFTER_SCHOOL},
            new DayPeriod[] {DayPeriod.AFTER_SCHOOL, DayPeriod.EVENING},
            new DayPeriod[] {DayPeriod.AFTER_SCHOOL, DayPeriod.DAY_END},
            new DayPeriod[] {DayPeriod.EVENING, DayPeriod.LATE_NIGHT},
            new DayPeriod[] {DayPeriod.EVENING, DayPeriod.DAY_END},
            new DayPeriod[] {DayPeriod.LATE_NIGHT, DayPeriod.DAY_END});

    @Test void timeAlwaysMovesForwardAndLessThanADay() {
        for (DayPeriod[] move : MOVES) {
            int from = ClockTimes.minutes(move[0], move[0]);
            int to = ClockTimes.minutes(move[1], move[0]);
            int elapsed = ClockTimes.elapsed(from, to);
            assertTrue(elapsed > 0 && elapsed < 12 * 60, move[0] + " -> " + move[1] + " took " + elapsed);
        }
    }

    @Test void lateNightEndsAfterMidnight() {
        int from = ClockTimes.minutes(DayPeriod.LATE_NIGHT, DayPeriod.LATE_NIGHT);
        int to = ClockTimes.minutes(DayPeriod.DAY_END, DayPeriod.LATE_NIGHT);
        assertEquals(120, ClockTimes.elapsed(from, to));
        assertEquals("01:30", ClockTimes.format(from + 120));
        assertEquals("23:30", ClockTimes.format(from));
    }

    @Test void daySegmentsChainWithoutGapsAndEndAfterMidnight() {
        var segments = ClockTimes.daySegments();
        for (int i = 1; i < segments.size(); i++)
            assertEquals(segments.get(i - 1).endMinutes(), segments.get(i).startMinutes(), "gap before " + segments.get(i).period());
        var late = segments.get(segments.size() - 1);
        assertEquals(DayPeriod.LATE_NIGHT, late.period());
        assertEquals(120, late.length(), "late night runs 23:30 to 01:30");
        assertTrue(segments.stream().allMatch(s -> s.length() > 0));
    }

    @Test void ringAnglesWrapAtMidnight() {
        assertEquals(0, ClockTimes.dayRingAngle(0), 1e-9);
        assertEquals(180, ClockTimes.dayRingAngle(12 * 60), 1e-9);
        assertEquals(15, ClockTimes.dayRingAngle(25 * 60), 1e-9);
    }

    @Test void longChangesEndWithThreeOneMinuteTicks() {
        var plan = ClockTimes.tickPlan(8 * 60 + 45, 11 * 60 + 10);
        assertEquals(11 * 60 + 7, plan.sweepTo());
        assertEquals(List.of(668, 669, 670), plan.ticks());
        var tiny = ClockTimes.tickPlan(100, 102);
        assertEquals(102, tiny.sweepTo());
        assertTrue(tiny.ticks().isEmpty());
    }

    @Test void nextAssessmentReadsTheFixedCalendar() {
        assertEquals(5, UiText.nextAssessment(3).orElseThrow().day());
        assertEquals("NEXT  LAB · DAY 05 · IN 2 DAYS", UiText.nextAssessmentLine(3));
        assertEquals("NEXT  LAB · DAY 05 · TOMORROW", UiText.nextAssessmentLine(4));
        assertEquals(20, UiText.nextAssessment(19).orElseThrow().day());
        assertTrue(UiText.nextAssessment(40).isEmpty());
        assertEquals("", UiText.nextAssessmentLine(40));
    }

    @Test void dayEndCaptionIsLightsOut() {
        assertEquals("LIGHTS OUT", ClockTimes.caption(DayPeriod.DAY_END));
        assertEquals("EVENING", ClockTimes.caption(DayPeriod.EVENING));
    }

    @Test void clockOnlyForATimeChangeWithinTheShownDay() throws IOException {
        var session = GameSession.newGame(DemoProfile.load(), "Tester", new Random(3), temp.resolve("save.properties"));
        assertFalse(ClockTimes.animates(null, 1, session.view()), "before the opening there is nothing to animate from");

        OpeningPlay.enterDayOne(session);
        var classroom = session.view();
        assertFalse(ClockTimes.animates(null, 1, classroom), "first screen of a day uses the wipe");
        assertFalse(ClockTimes.animates(DayPeriod.ICS_CLASS, 1, classroom), "same period: no time change");

        session.openLesson();
        session.lessonChoice("quiet");
        var otherClasses = session.view();
        assertTrue(ClockTimes.animates(DayPeriod.ICS_CLASS, 1, otherClasses));
        assertFalse(ClockTimes.animates(DayPeriod.ICS_CLASS, 0, otherClasses), "a different day uses the wipe");
    }
}
