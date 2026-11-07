package com.aphs.classroomuprising.domain;

public class ActionBudget {
    private int initializedDay;
    private final int[] remainingByPeriod;

    public ActionBudget() {
        this.initializedDay = 0;
        this.remainingByPeriod = new int[DayPeriod.values().length];
    }

    public void initializeDay(int day) {
        this.initializedDay = day;
        this.remainingByPeriod[DayPeriod.ICS_CLASS.ordinal()] = 3;
        this.remainingByPeriod[DayPeriod.OTHER_CLASSES.ordinal()] = 2;
        this.remainingByPeriod[DayPeriod.STUDENT_HUB.ordinal()] = 3;
    }

    public boolean canSpend(DayPeriod period, int cost) {
        return remainingByPeriod[period.ordinal()] >= cost;
    }

    public void spend(DayPeriod period, int cost) {
        if (canSpend(period, cost)) {
            remainingByPeriod[period.ordinal()] -= cost;
        }
    }

    public void closePeriod(DayPeriod period) {
        remainingByPeriod[period.ordinal()] = 0;
    }

    public int remaining(DayPeriod period) {
        return remainingByPeriod[period.ordinal()];
    }
}
