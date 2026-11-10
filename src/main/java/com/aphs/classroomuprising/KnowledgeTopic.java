package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * A knowledge topic that is released on a set date.
 *
 * @author TeamPike
 * @since 1.0
 */
public class KnowledgeTopic {

    private final String id;
    private final String subject;
    private final int unlockDay;
    private final ArrayList<String> prerequisiteIds;

    /**
     * Creates a knowledge topic.
     *
     * @param id the unique topic ID
     * @param subject the subject the topic belongs to
     * @param unlockDay the first day the topic can be unlocked
     * @param prerequisiteIds the IDs that must be completed first
     */
    public KnowledgeTopic(String id, String subject, int unlockDay, ArrayList<String> prerequisiteIds) {
        this.id = id;
        this.subject = subject;
        this.unlockDay = unlockDay;
        this.prerequisiteIds = prerequisiteIds;
    }

    /**
     * Checks whether the date and prerequisites for this topic are met.
     *
     * @param state the current game state
     * @return true if the topic can be unlocked
     */
    public boolean isEligible(GameState state) {
        throw new UnsupportedOperationException("KnowledgeTopic.isEligible not implemented yet");
    }
}
