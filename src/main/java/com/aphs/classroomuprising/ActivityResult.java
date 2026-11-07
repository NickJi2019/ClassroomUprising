package com.aphs.classroomuprising;

/**
 * The outcome of an attempted activity.
 *
 * @author TeamPike
 * @since 1.0
 */
public class ActivityResult {

    private final boolean accepted;
    private final String message;

    /**
     * Creates an activity result.
     *
     * @param accepted whether the activity was accepted
     * @param message the learning feedback, or the reason for rejection
     */
    public ActivityResult(boolean accepted, String message) {
        this.accepted = accepted;
        this.message = message;
    }

    /**
     * Returns whether the activity was accepted.
     *
     * @return true if accepted
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Returns the learning feedback, or the reason for rejection.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }
}
