package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * Records which topics, stories and assessments the player has unlocked or finished.
 *
 * @author TeamPike
 * @since 1.0
 */
public class ContentProgress {

    private boolean introductionCompleted;
    private final ArrayList<String> unlockedTopicIds;
    private final ArrayList<String> availableStoryIds;
    private final ArrayList<String> completedStoryIds;
    private final ArrayList<String> resolvedAssessmentIds;

    /** Creates progress for a new game: nothing unlocked or completed. */
    public ContentProgress() {
        this.introductionCompleted = false;
        this.unlockedTopicIds = new ArrayList<>();
        this.availableStoryIds = new ArrayList<>();
        this.completedStoryIds = new ArrayList<>();
        this.resolvedAssessmentIds = new ArrayList<>();
    }

    /**
     * Checks whether a topic is unlocked.
     *
     * @param id the topic ID
     * @return true if the topic is unlocked
     */
    public boolean isTopicUnlocked(String id) {
        throw new UnsupportedOperationException("ContentProgress.isTopicUnlocked not implemented yet");
    }

    /**
     * Checks whether a story has been completed.
     *
     * @param id the story ID
     * @return true if the story is completed
     */
    public boolean isStoryCompleted(String id) {
        throw new UnsupportedOperationException("ContentProgress.isStoryCompleted not implemented yet");
    }

    /**
     * Checks whether an assessment has been resolved.
     *
     * @param id the assessment ID
     * @return true if the assessment is resolved
     */
    public boolean isAssessmentResolved(String id) {
        throw new UnsupportedOperationException("ContentProgress.isAssessmentResolved not implemented yet");
    }
}
