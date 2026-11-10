package com.aphs.classroomuprising;

/**
 * Runs a scheduled Lab/Test encounter turn by turn (IPO 9).
 *
 * @author TeamPike
 * @since 1.0
 */
public class AssessmentController {

    private final MrReward mrReward;

    /** The assessment in progress; null when none is active. */
    private AssessmentSession session;

    /**
     * Creates the assessment controller.
     *
     * @param mrReward the opponent in each assessment
     */
    public AssessmentController(MrReward mrReward) {
        this.mrReward = mrReward;
    }

    /**
     * Validates the date, unresolved event and story prerequisites, then starts the encounter.
     *
     * @param state the current game state
     * @param event the scheduled assessment
     * @return the new session
     */
    public AssessmentSession start(GameState state, ScheduledAssessment event) {
        throw new UnsupportedOperationException("AssessmentController.start not implemented yet");
    }

    /**
     * Resolves one turn. An invalid action is rejected and the current turn is kept.
     *
     * @param state the current game state
     * @param action the player's chosen action
     * @param answer the submitted answer or solution
     * @return the turn feedback for the player
     */
    public String resolveTurn(GameState state, String action, String answer) {
        throw new UnsupportedOperationException("AssessmentController.resolveTurn not implemented yet");
    }

    /**
     * Returns the outcome of the finished assessment.
     *
     * @return the assessment result
     */
    public AssessmentResult result() {
        throw new UnsupportedOperationException("AssessmentController.result not implemented yet");
    }
}
