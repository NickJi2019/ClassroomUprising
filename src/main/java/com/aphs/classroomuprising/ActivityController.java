package com.aphs.classroomuprising;

/**
 * Validates and performs daily activities (IPO 4, 5 and 6).
 *
 * @author TeamPike
 * @since 1.0
 */
public class ActivityController {

    private final ContentCatalog catalog;

    /**
     * Creates the activity controller.
     *
     * @param catalog the activity and question definitions
     */
    public ActivityController(ContentCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Performs an activity. A rejected activity changes neither the state nor the budget.
     * An accepted activity spends its cost once and applies its stat change.
     *
     * @param state the current game state
     * @param activityId the chosen activity
     * @param answer the submitted answer, or null if the activity needs none
     * @return the outcome and feedback
     */
    public ActivityResult perform(GameState state, String activityId, String answer) {
        throw new UnsupportedOperationException("ActivityController.perform not implemented yet");
    }
}
