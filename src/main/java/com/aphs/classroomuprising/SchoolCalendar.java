package com.aphs.classroomuprising;

import java.util.ArrayList;
import java.util.List;

/** The one authoritative, immutable implementation of the approved forty-day schedule. */
public final class SchoolCalendar {
    private static final List<DayPlan> DAYS = create();
    private SchoolCalendar() {}
    private static List<DayPlan> create() {
        var days = new ArrayList<DayPlan>();
        for (int day = 1; day <= 40; day++) {
            CourseType type;
            int unit = 0;
            if (day <= 2) type = CourseType.LESSON;
            else if (day == 20) type = CourseType.MIDTERM;
            else if (day == 40) type = CourseType.FINAL;
            else if (day == 18 || day == 19 || day >= 36) type = CourseType.REVIEW;
            else {
                int offset = day <= 17 ? day - 3 : day - 21;
                unit = offset / 5 + (day <= 17 ? 1 : 4);
                type = switch (offset % 5) {
                    case 2 -> CourseType.LAB;
                    case 4 -> CourseType.UNIT_TEST;
                    default -> CourseType.LESSON;
                };
            }
            days.add(new DayPlan(day, type, unit, day <= 2));
        }
        return List.copyOf(days);
    }
    public static List<DayPlan> days() { return DAYS; }
    public static DayPlan day(int number) {
        if (number < 1 || number > 40) throw new IllegalArgumentException("Day must be 1..40.");
        return DAYS.get(number - 1);
    }
    /** Returns the first two subsequent original Lessons, without choosing or bypassing collisions. */
    public static List<Integer> makeupCandidates(int missedDay) {
        day(missedDay);
        return DAYS.stream().filter(d -> d.day() > missedDay && d.course() == CourseType.LESSON)
                .limit(2).map(DayPlan::day).toList();
    }
}
