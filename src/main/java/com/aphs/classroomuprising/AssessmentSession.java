package com.aphs.classroomuprising;

/**
 * The state of one Lab/Test encounter in progress.
 *
 * @author TeamPike
 * @since 1.0
 */
public class AssessmentSession {

    private final ScheduledAssessment assessment;
    private int turnNumber;

    /**
     * Creates a session at turn 1.
     *
     * @param assessment the scheduled assessment being played
     */
    public AssessmentSession(ScheduledAssessment assessment) {
        this.assessment = assessment;
        this.turnNumber = 1;
    }

    /**
     * Returns the scheduled assessment being played.
     *
     * @return the scheduled assessment
     */
    public ScheduledAssessment getAssessment() {
        return assessment;
    }

    /**
     * Returns the current turn.
     *
     * @return the turn number
     */
    public int getTurnNumber() {
        return turnNumber;
    }
}
