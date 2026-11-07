package com.aphs.classroomuprising.domain;

import java.util.ArrayList;

public class SchoolCalendar {
    private int currentDay;
    private DayPeriod currentPeriod;
    private ArrayList<ScheduledAssessment> schedule;

    public void advancePeriod() {}
    public void advanceDay() {}
    public ScheduledAssessment assessmentDue() { return null; }
}
