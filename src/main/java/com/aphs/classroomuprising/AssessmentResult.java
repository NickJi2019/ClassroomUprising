package com.aphs.classroomuprising;

/**
 * The outcome of a finished Lab/Test.
 *
 * @author TeamPike
 * @since 1.0
 */
public class AssessmentResult {

    private final String assessmentId;
    private final boolean continuationAllowed;
    private final StatChange completionEffect;

    /**
     * Creates an assessment result.
     *
     * @param assessmentId the ID of the finished assessment
     * @param continuationAllowed whether the day can continue after this result
     * @param completionEffect the effect applied once on completion
     */
    public AssessmentResult(String assessmentId, boolean continuationAllowed, StatChange completionEffect) {
        this.assessmentId = assessmentId;
        this.continuationAllowed = continuationAllowed;
        this.completionEffect = completionEffect;
    }

    /**
     * Returns the ID of the finished assessment.
     *
     * @return the assessment ID
     */
    public String getAssessmentId() {
        return assessmentId;
    }

    /**
     * Returns whether the day can continue after this result.
     *
     * @return true if continuation is allowed
     */
    public boolean isContinuationAllowed() {
        return continuationAllowed;
    }

    /**
     * Returns the effect applied once on completion.
     *
     * @return the completion effect
     */
    public StatChange getCompletionEffect() {
        return completionEffect;
    }
}
