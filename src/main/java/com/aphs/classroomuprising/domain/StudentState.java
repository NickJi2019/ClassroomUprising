package com.aphs.classroomuprising.domain;

public class StudentState {
    private String name;
    private int mentalState;
    private int icsKnowledge;
    private int otherKnowledge;
    private int gradeHp;

    public StudentState(String name) {
        this.name = name;
        this.mentalState = 100;
        this.icsKnowledge = 0;
        this.otherKnowledge = 0;
        this.gradeHp = 100;
    }

    public String getName() {
        return name;
    }

    public int getMentalState() {
        return mentalState;
    }

    public int getIcsKnowledge() {
        return icsKnowledge;
    }

    public int getOtherKnowledge() {
        return otherKnowledge;
    }

    public int getGradeHp() {
        return gradeHp;
    }

    public void apply(StatChange change) {
        if (change == null) return;
        this.mentalState = Math.max(0, Math.min(100, this.mentalState + change.getMentalDelta()));
        this.icsKnowledge = Math.max(0, this.icsKnowledge + change.getIcsDelta());
        this.otherKnowledge = Math.max(0, this.otherKnowledge + change.getOtherDelta());
        this.gradeHp = Math.max(0, Math.min(100, this.gradeHp + change.getGradeHpDelta()));
    }

    public boolean isGradeDepleted() {
        return gradeHp <= 0;
    }
}
