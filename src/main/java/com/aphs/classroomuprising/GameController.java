package com.aphs.classroomuprising;

/**
 * Runs the active game session and the daily routine (IPO 3, 7 and 9).
 *
 * @author TeamPike
 * @since 1.0
 */
public class GameController {

    /** The active session; null until a game is started or loaded. */
    private GameState state;

    private final GameUI ui;
    private final StoryController storyController;
    private final ActivityController activityController;
    private final StudentHubController studentHubController;
    private final AssessmentController assessmentController;
    private final SaveManager saveManager;

    /**
     * Creates the game controller and its Student Hub controller.
     *
     * @param ui presents the game state
     * @param storyController handles story gates and unlocks
     * @param activityController performs daily activities
     * @param assessmentController runs scheduled Lab/Test encounters
     * @param saveManager saves and restores the game
     */
    public GameController(GameUI ui, StoryController storyController, ActivityController activityController,
                          AssessmentController assessmentController, SaveManager saveManager) {
        this.ui = ui;
        this.storyController = storyController;
        this.activityController = activityController;
        this.assessmentController = assessmentController;
        this.saveManager = saveManager;
        // The Hub calls back into endPeriod(), so it is created here instead of being passed in.
        this.studentHubController = new StudentHubController(this, ui, activityController);
    }

    /**
     * Initializes the student state, calendar and content flags for a new game.
     *
     * @param name the student's name
     */
    public void newGame(String name) {
        throw new UnsupportedOperationException("GameController.newGame not implemented yet");
    }

    /**
     * Restores the complete saved state.
     *
     * @return true if the save was valid and restored
     */
    public boolean loadGame() {
        throw new UnsupportedOperationException("GameController.loadGame not implemented yet");
    }

    /**
     * Starts a school day: ICS period, new allowances, unlocks, then any scheduled events.
     * Accumulated knowledge, mental state and grade are not reset.
     */
    public void beginDay() {
        throw new UnsupportedOperationException("GameController.beginDay not implemented yet");
    }

    /** Sends the player to the screen or controller for the current period. */
    public void routeCurrentPeriod() {
        throw new UnsupportedOperationException("GameController.routeCurrentPeriod not implemented yet");
    }

    /**
     * Finishes the current period (ICS class, then other classes, then Student Hub, then the next day).
     * Unused actions are discarded. A required story or assessment blocks advancement.
     *
     * @return true if the routine advanced; false if it was blocked
     */
    public boolean endPeriod() {
        throw new UnsupportedOperationException("GameController.endPeriod not implemented yet");
    }

    /**
     * Records a finished assessment, applies its completion effects once and shows the result.
     *
     * @param result the outcome of the assessment
     */
    public void finishAssessment(AssessmentResult result) {
        throw new UnsupportedOperationException("GameController.finishAssessment not implemented yet");
    }

    /**
     * Saves the active session.
     *
     * @return true if the save succeeded
     */
    public boolean saveGame() {
        throw new UnsupportedOperationException("GameController.saveGame not implemented yet");
    }
}
