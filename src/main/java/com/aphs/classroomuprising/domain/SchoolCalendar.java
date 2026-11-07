package com.aphs.classroomuprising.domain;

import java.util.ArrayList;
import java.util.List;

public class SchoolCalendar {
    private int currentDay;
    private DayPeriod currentPeriod;
    private final List<ScheduledAssessment> schedule;

    public SchoolCalendar() {
        this.currentDay = 1;
        this.currentPeriod = DayPeriod.ICS_CLASS;
        this.schedule = new ArrayList<>();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public DayPeriod getCurrentPeriod() {
        return currentPeriod;
    }

    public List<ScheduledAssessment> getSchedule() {
        return schedule;
    }

    public void advancePeriod() {
        if (currentPeriod == DayPeriod.ICS_CLASS) {
            currentPeriod = DayPeriod.OTHER_CLASSES;
        } else if (currentPeriod == DayPeriod.OTHER_CLASSES) {
            currentPeriod = DayPeriod.STUDENT_HUB;
        } else {
            advanceDay();
        }
    }

    public void advanceDay() {
        currentDay++;
        currentPeriod = DayPeriod.ICS_CLASS;
    }

    public ScheduledAssessment assessmentDue() {
        for (ScheduledAssessment assessment : schedule) {
            if (assessment.getDay() == currentDay && assessment.getPeriod() == currentPeriod && !assessment.isCompleted()) {
                return assessment;
            }
        }
        return null;
    }
}
