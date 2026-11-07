package com.aphs.classroomuprising.domain;

import java.util.ArrayList;

public class GameState {
    private StudentState student;
    private SchoolCalendar calendar;
    private ActionBudget actionBudget;
    private ContentProgress contentProgress;
    private ArrayList<AssessmentResult> assessmentHistory;

    public boolean recordAssessment(AssessmentResult result) { return false; }
}
