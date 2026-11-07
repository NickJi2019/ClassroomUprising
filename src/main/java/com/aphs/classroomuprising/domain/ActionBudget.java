package com.aphs.classroomuprising.domain;

public class ActionBudget {
    private int initializedDay;
    private int[] remainingByPeriod;

    public void initializeDay(int day) {}
    public boolean canSpend(DayPeriod period, int cost) { return false; }
    public void spend(DayPeriod period, int cost) {}
    public void closePeriod(DayPeriod period) {}
    public int remaining(DayPeriod period) { return 0; }
}
