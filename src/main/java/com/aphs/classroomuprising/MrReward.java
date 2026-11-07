package com.aphs.classroomuprising;

/**
 * The boss. Chooses each challenge and decides its effect on the student.
 *
 * @author TeamPike
 * @since 1.0
 */
public class MrReward {

    /**
     * Selects the challenge pattern for the next turn.
     *
     * @param session the assessment in progress
     * @return the challenge pattern
     */
    public String selectChallenge(AssessmentSession session) {
        throw new UnsupportedOperationException("MrReward.selectChallenge not implemented yet");
    }

    /**
     * Calculates the effect of the challenge on the student.
     *
     * @param student the student being challenged
     * @param correct whether the student's answer was correct
     * @return the stat change to apply to the student
     */
    public StatChange calculateResponse(StudentState student, boolean correct) {
        throw new UnsupportedOperationException("MrReward.calculateResponse not implemented yet");
    }
}
