package com.aphs.classroomuprising;

/**
 * A question that belongs to a knowledge topic.
 *
 * @author TeamPike
 * @since 1.0
 */
public class Question {

    private final String id;
    private final String topicId;
    private final String prompt;
    private final String answer;

    /**
     * Creates a question.
     *
     * @param id the unique question ID
     * @param topicId the topic the question belongs to
     * @param prompt the text shown to the player
     * @param answer the correct answer
     */
    public Question(String id, String topicId, String prompt, String answer) {
        this.id = id;
        this.topicId = topicId;
        this.prompt = prompt;
        this.answer = answer;
    }

    /**
     * Returns the question ID.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the topic the question belongs to.
     *
     * @return the topic ID
     */
    public String getTopicId() {
        return topicId;
    }

    /**
     * Returns the text shown to the player.
     *
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Returns the correct answer.
     *
     * @return the answer
     */
    public String getAnswer() {
        return answer;
    }
}
