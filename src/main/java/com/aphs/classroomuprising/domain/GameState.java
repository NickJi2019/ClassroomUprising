package com.aphs.classroomuprising.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {
    private final StudentState student;
    private final SchoolCalendar calendar;
    private final ActionBudget actionBudget;
    private final ContentProgress contentProgress;
    private final List<AssessmentResult> assessmentHistory;

    public GameState(StudentState student) {
        this.student = student;
        this.calendar = new SchoolCalendar();
        this.actionBudget = new ActionBudget();
        this.contentProgress = new ContentProgress();
        this.assessmentHistory = new ArrayList<>();
        this.actionBudget.initializeDay(1);
    }

    public StudentState getStudent() {
        return student;
    }

    public SchoolCalendar getCalendar() {
        return calendar;
    }

    public ActionBudget getActionBudget() {
        return actionBudget;
    }

    public ContentProgress getContentProgress() {
        return contentProgress;
    }

    public List<AssessmentResult> getAssessmentHistory() {
        return Collections.unmodifiableList(assessmentHistory);
    }

    public boolean recordAssessment(AssessmentResult result) {
        if (result != null) {
            return assessmentHistory.add(result);
        }
        return false;
    }
}
