package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * Holds the game's fixed content: stories, topics, questions and activities.
 * The lists are empty until the team writes the content.
 *
 * @author TeamPike
 * @since 1.0
 */
public class ContentCatalog {

    private final ArrayList<StoryEvent> stories;
    private final ArrayList<KnowledgeTopic> topics;
    private final ArrayList<Question> questions;
    private final ArrayList<Activity> activities;

    /** Creates an empty catalog. */
    public ContentCatalog() {
        this.stories = new ArrayList<>();
        this.topics = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.activities = new ArrayList<>();
    }

    /**
     * Finds an activity by its ID.
     *
     * @param id the activity ID
     * @return the matching activity
     */
    public Activity findActivity(String id) {
        throw new UnsupportedOperationException("ContentCatalog.findActivity not implemented yet");
    }

    /**
     * Selects a question from a topic.
     *
     * @param topicId the topic ID
     * @return a question from that topic
     */
    public Question selectQuestion(String topicId) {
        throw new UnsupportedOperationException("ContentCatalog.selectQuestion not implemented yet");
    }
}
