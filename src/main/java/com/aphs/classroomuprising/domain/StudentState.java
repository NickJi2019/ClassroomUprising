package com.aphs.classroomuprising.domain;

public class StudentState {
    private String name;
    private int mentalState;
    private int icsKnowledge;
    private int otherKnowledge;
    private int gradeHp;

    public void apply(StatChange change) {}
    public boolean isGradeDepleted() { return false; }
}
