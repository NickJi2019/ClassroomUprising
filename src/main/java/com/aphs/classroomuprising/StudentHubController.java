package com.aphs.classroomuprising;

/**
 * Handles navigation and activities in the Student Hub (IPO 6).
 *
 * @author TeamPike
 * @since 1.0
 */
public class StudentHubController {

    private final GameController gameController;
    private final GameUI ui;
    private final ActivityController activityController;

    /**
     * Creates the Student Hub controller.
     *
     * @param gameController is told when the day ends
     * @param ui shows the Hub
     * @param activityController performs the chosen activities
     */
    public StudentHubController(GameController gameController, GameUI ui, ActivityController activityController) {
        this.gameController = gameController;
        this.ui = ui;
        this.activityController = activityController;
    }

    /**
     * Opens the Hub and shows the current stats and schedule.
     *
     * @param state the current game state
     */
    public void openHub(GameState state) {
        throw new UnsupportedOperationException("StudentHubController.openHub not implemented yet");
    }

    /**
     * Performs a Hub activity.
     *
     * @param state the current game state
     * @param id the chosen activity
     * @param answer the submitted answer, or null if the activity needs none
     * @return the outcome and feedback
     */
    public ActivityResult chooseActivity(GameState state, String id, String answer) {
        throw new UnsupportedOperationException("StudentHubController.chooseActivity not implemented yet");
    }

    /** Ends the day when the player leaves the Hub. */
    public void endDay() {
        throw new UnsupportedOperationException("StudentHubController.endDay not implemented yet");
    }
}
