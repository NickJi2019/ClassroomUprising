package com.aphs.classroomuprising;

/** The optional half-slot and the five confirmed normal slots. */
public enum DayPeriod {
    MORNING_BONUS, ICS_CLASS, OTHER_CLASSES, AFTER_SCHOOL, EVENING, LATE_NIGHT, DAY_END;
    public boolean isHubActivityPeriod() {
        return this == MORNING_BONUS || this == AFTER_SCHOOL || this == EVENING || this == LATE_NIGHT;
    }
    public double activityUnits() { return this == MORNING_BONUS ? 0.5 : 1.0; }
    public DayPeriod next() {
        return switch (this) {
            case MORNING_BONUS -> ICS_CLASS;
            case ICS_CLASS -> OTHER_CLASSES;
            case OTHER_CLASSES -> AFTER_SCHOOL;
            case AFTER_SCHOOL -> EVENING;
            case EVENING -> LATE_NIGHT;
            case LATE_NIGHT -> DAY_END;
            case DAY_END -> throw new IllegalStateException("The day is already closed.");
        };
    }
}
