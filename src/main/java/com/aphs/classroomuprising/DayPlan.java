package com.aphs.classroomuprising;

/** Unit zero denotes a tutorial/review/major exam, not an additional knowledge stat. */
public record DayPlan(int day, CourseType course, int unit, boolean tutorial) {
    public DayPlan {
        if (day < 1 || day > 40 || unit < 0 || unit > 6 || course == null)
            throw new IllegalArgumentException("Invalid day plan.");
    }
    public String assessmentId() {
        if (!course.isAssessment()) throw new IllegalStateException("This is not an assessment day.");
        return "day-" + day + "-" + course.name().toLowerCase(java.util.Locale.ROOT);
    }
}
