package com.aphs.classroomuprising.domain;

public class StatChange {
    private final int mentalDelta;
    private final int icsDelta;
    private final int otherDelta;
    private final int gradeHpDelta;

    public StatChange(int mentalDelta, int icsDelta, int otherDelta, int gradeHpDelta) {
        this.mentalDelta = mentalDelta;
        this.icsDelta = icsDelta;
        this.otherDelta = otherDelta;
        this.gradeHpDelta = gradeHpDelta;
    }

    public int getMentalDelta() {
        return mentalDelta;
    }

    public int getIcsDelta() {
        return icsDelta;
    }

    public int getOtherDelta() {
        return otherDelta;
    }

    public int getGradeHpDelta() {
        return gradeHpDelta;
    }
}
