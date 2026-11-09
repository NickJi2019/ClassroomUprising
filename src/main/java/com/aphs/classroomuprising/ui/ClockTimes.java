package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.GameState;

/**
 * Clock faces shown by the time-of-day transition. These are display positions only:
 * the game's rules know periods, not clock times, and nothing here affects state.
 */
final class ClockTimes {
    static final int DAY_MINUTES = 24 * 60;

    private ClockTimes() {}

    /** Minutes after midnight shown for a period; the day end depends on how the day closed. */
    static int minutes(DayPeriod period, DayPeriod from) {
        return switch (period) {
            case MORNING_BONUS -> 7 * 60;
            case ICS_CLASS -> 8 * 60 + 45;
            case OTHER_CLASSES -> 11 * 60 + 10;
            case AFTER_SCHOOL -> 15 * 60 + 20;
            case EVENING -> 19 * 60;
            case LATE_NIGHT -> 23 * 60 + 30;
            case DAY_END -> switch (from) {
                case AFTER_SCHOOL -> 16 * 60 + 30;   // slept right after school
                case LATE_NIGHT -> 60 + 30;         // after midnight
                default -> 22 * 60 + 30;
            };
        };
    }

    /** Forward time from one clock position to the next, wrapping past midnight. */
    static int elapsed(int fromMinutes, int toMinutes) {
        return Math.floorMod(toMinutes - fromMinutes, DAY_MINUTES);
    }

    static String format(double minutes) {
        int value = (int) Math.floorMod((long) Math.floor(minutes), DAY_MINUTES);
        return "%02d:%02d".formatted(value / 60, value % 60);
    }

    static String caption(DayPeriod period) {
        return period == DayPeriod.DAY_END ? "LIGHTS OUT" : UiText.period(period);
    }

    /**
     * Whether moving to this snapshot is a time change inside the same school day that the
     * screen just showed (the clock), rather than a new day, a load or the first screen (a wipe).
     */
    static boolean animates(DayPeriod shownPeriod, int shownDay, GameState view) {
        return shownPeriod != null && view.openingCompleted() && view.status() == GameState.Status.ACTIVE
                && shownDay == view.day() && shownPeriod != view.period();
    }
}
