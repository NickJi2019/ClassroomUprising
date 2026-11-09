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

    @Test void dayEndCaptionIsLightsOut() {
        assertEquals("LIGHTS OUT", ClockTimes.caption(DayPeriod.DAY_END));
        assertEquals("EVENING", ClockTimes.caption(DayPeriod.EVENING));
    }

    @Test void clockOnlyForATimeChangeWithinTheShownDay() throws IOException {
        var session = GameSession.newGame(DemoProfile.load(), "Tester", new Random(3), temp.resolve("save.properties"));
        assertFalse(ClockTimes.animates(null, 1, session.view()), "before the opening there is nothing to animate from");

        session.completeOpening();
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
