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

    /** One period's span on the 24-hour schedule ring (end may wrap past midnight). */
    record Segment(DayPeriod period, int startMinutes, int endMinutes) {
        int length() { return elapsed(startMinutes, endMinutes); }
    }

    /**
     * The school day as display segments, in order: bonus morning, ICS4U, other classes,
     * after school, evening, late night (until the after-midnight lights-out time).
     */
    static java.util.List<Segment> daySegments() {
        DayPeriod[] order = {DayPeriod.MORNING_BONUS, DayPeriod.ICS_CLASS, DayPeriod.OTHER_CLASSES,
                DayPeriod.AFTER_SCHOOL, DayPeriod.EVENING, DayPeriod.LATE_NIGHT};
        var segments = new java.util.ArrayList<Segment>();
        for (int i = 0; i < order.length; i++) {
            int start = minutes(order[i], order[i]);
            int end = i + 1 < order.length ? minutes(order[i + 1], order[i + 1])
                    : minutes(DayPeriod.DAY_END, DayPeriod.LATE_NIGHT);
            segments.add(new Segment(order[i], start, end));
        }
        return java.util.List.copyOf(segments);
    }

    /** Position on the 24-hour ring, in degrees clockwise from the top (midnight). */
    static double dayRingAngle(double minutes) {
        return Math.floorMod((long) Math.floor(minutes * 1000), DAY_MINUTES * 1000L) / 1000.0 / DAY_MINUTES * 360;
    }

    /**
     * How the minute hand travels: a smooth sweep to three minutes short, then three discrete
     * one-minute ticks. Short changes (under four minutes) just sweep.
     *
     * @param sweepTo the minutes value the smooth sweep ends on
     * @param ticks   minute values shown one after another after the sweep
     */
    record TickPlan(int sweepTo, java.util.List<Integer> ticks) {}

    static TickPlan tickPlan(int startMinutes, int endMinutes) {
        if (endMinutes - startMinutes < 4) return new TickPlan(endMinutes, java.util.List.of());
        return new TickPlan(endMinutes - 3, java.util.List.of(endMinutes - 2, endMinutes - 1, endMinutes));
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
