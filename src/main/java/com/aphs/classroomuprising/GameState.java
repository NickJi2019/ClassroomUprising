package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * The complete state of one game session. This is what gets saved and restored.
 *
 * @author TeamPike
 * @since 1.0
 */
public class GameState {

    private final StudentState student;
    private final SchoolCalendar calendar;
    private final ActionBudget actionBudget;
    private final ContentProgress contentProgress;
    private final ArrayList<AssessmentResult> assessmentHistory;

    /**
     * Creates a game state with an empty assessment history.
     *
     * @param student the student's stats
     * @param calendar the current day and period
     * @param actionBudget the remaining actions per period
     * @param contentProgress the unlocked and completed content
     */
    public GameState(StudentState student, SchoolCalendar calendar, ActionBudget actionBudget,
                     ContentProgress contentProgress) {
        this.student = student;
        this.calendar = calendar;
        this.actionBudget = actionBudget;
        this.contentProgress = contentProgress;
        this.assessmentHistory = new ArrayList<>();
    }

    /**
     * Records a finished assessment. A result whose ID was already recorded is rejected.
     *
     * @param result the result to record
     * @return true if recorded; false if rejected as a duplicate
     */
    public boolean recordAssessment(AssessmentResult result) {
        throw new UnsupportedOperationException("GameState.recordAssessment not implemented yet");
    }

    /**
     * Returns the student's stats.
     *
     * @return the student state
     */
    public StudentState getStudent() {
        return student;
    }

    /**
     * Returns the calendar.
     *
     * @return the school calendar
     */
    public SchoolCalendar getCalendar() {
        return calendar;
    }

    /**
     * Returns the action budget.
     *
     * @return the action budget
     */
    public ActionBudget getActionBudget() {
        return actionBudget;
    }

    /**
     * Returns the content progress.
     *
     * @return the content progress
     */
    public ContentProgress getContentProgress() {
        return contentProgress;
    }

    /**
     * Returns the recorded assessment results.
     *
     * @return the assessment history
     */
    public ArrayList<AssessmentResult> getAssessmentHistory() {
        return assessmentHistory;
    }
}
