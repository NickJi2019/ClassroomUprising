package com.aphs.classroomuprising.domain;

public class ScheduledAssessment {
    private final String id;
    private final String title;
    private final int day;
    private final DayPeriod period;
    private boolean completed;

    public ScheduledAssessment(String id, String title, int day, DayPeriod period) {
        this.id = id;
        this.title = title;
        this.day = day;
        this.period = period;
        this.completed = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDay() {
        return day;
    }

    public DayPeriod getPeriod() {
        return period;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }
}
