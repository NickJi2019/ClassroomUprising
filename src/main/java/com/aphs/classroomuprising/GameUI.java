package com.aphs.classroomuprising;

/**
 * Displays the game to the player. The UI technology has not been chosen yet;
 * {@link #showMainMenu()} prints to the console so that the skeleton can run.
 *
 * @author TeamPike
 * @since 1.0
 */
public class GameUI {

    /** Shows the main menu options (IPO 1). */
    public void showMainMenu() {
        System.out.println("=== Classroom Uprising ===");
        System.out.println("[skeleton] Main menu: New Game / Continue / Instructions / Exit");
    }

    /** Shows the instructions screen. */
    public void showInstructions() {
        throw new UnsupportedOperationException("GameUI.showInstructions not implemented yet");
    }

    /**
     * Shows the current period with the day, stats and remaining actions.
     *
     * @param state the current game state
     */
    public void showPeriod(GameState state) {
        throw new UnsupportedOperationException("GameUI.showPeriod not implemented yet");
    }

    /**
     * Shows one scene of a story event.
     *
     * @param event the story event being played
     * @param sceneIndex the index of the scene to show
     */
    public void showStory(StoryEvent event, int sceneIndex) {
        throw new UnsupportedOperationException("GameUI.showStory not implemented yet");
    }

    /**
     * Shows the Lab/Test battle screen.
     *
     * @param session the assessment in progress
     */
    public void showAssessment(AssessmentSession session) {
        throw new UnsupportedOperationException("GameUI.showAssessment not implemented yet");
    }

    /**
     * Shows a feedback message.
     *
     * @param message the message to show
     */
    public void showFeedback(String message) {
        throw new UnsupportedOperationException("GameUI.showFeedback not implemented yet");
    }

    /**
     * Shows the result of a finished assessment.
     *
     * @param result the assessment result
     */
    public void showResult(AssessmentResult result) {
        throw new UnsupportedOperationException("GameUI.showResult not implemented yet");
    }
}
