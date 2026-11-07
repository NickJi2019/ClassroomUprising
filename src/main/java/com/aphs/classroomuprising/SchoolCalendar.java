package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * Tracks the current day and period, and holds the preset Lab/Test schedule.
 *
 * @author TeamPike
 * @since 1.0
 */
public class SchoolCalendar {

    private int currentDay;
    private DayPeriod currentPeriod;
    private final ArrayList<ScheduledAssessment> schedule;

    /**
     * Creates a calendar starting on Day 1 in ICS class.
     *
     * @param schedule the preset Lab/Test schedule
     */
    public SchoolCalendar(ArrayList<ScheduledAssessment> schedule) {
        this.currentDay = 1;
        this.currentPeriod = DayPeriod.ICS_CLASS;
        this.schedule = schedule;
    }

    /** Moves to the next period of the day. */
    public void advancePeriod() {
        throw new UnsupportedOperationException("SchoolCalendar.advancePeriod not implemented yet");
    }

    /** Moves to the next day. */
    public void advanceDay() {
        throw new UnsupportedOperationException("SchoolCalendar.advanceDay not implemented yet");
    }

    /**
     * Finds the assessment due at the current day and period.
     *
     * @return the assessment that is due, or null if there is none
     */
    public ScheduledAssessment assessmentDue() {
        throw new UnsupportedOperationException("SchoolCalendar.assessmentDue not implemented yet");
    }

    /**
     * Returns the current day.
     *
     * @return the current day number
     */
    public int getCurrentDay() {
        return currentDay;
    }

    /**
     * Returns the current period.
     *
     * @return the current period
     */
    public DayPeriod getCurrentPeriod() {
        return currentPeriod;
    }
}
