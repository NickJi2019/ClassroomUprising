package com.aphs.classroomuprising;

/**
 * Routes main menu selections (IPO 1: New Game, Continue, Instructions, Exit).
 *
 * @author TeamPike
 * @since 1.0
 */
public class MainMenuController {

    private final GameController gameController;
    private final GameUI ui;

    /**
     * Creates the main menu controller.
     *
     * @param gameController starts or restores the game session
     * @param ui shows the menu and instructions
     */
    public MainMenuController(GameController gameController, GameUI ui) {
        this.gameController = gameController;
        this.ui = ui;
    }

    /**
     * Starts a new game for the named student.
     *
     * @param name the student's name
     */
    public void newGame(String name) {
        throw new UnsupportedOperationException("MainMenuController.newGame not implemented yet");
    }

    /**
     * Restores the saved game.
     *
     * @return true if a valid save was restored; false if a load-error message should be shown
     */
    public boolean continueGame() {
        throw new UnsupportedOperationException("MainMenuController.continueGame not implemented yet");
    }

    /** Shows the instructions screen. */
    public void showInstructions() {
        throw new UnsupportedOperationException("MainMenuController.showInstructions not implemented yet");
    }

    /** Exits the application. */
    public void exitGame() {
        throw new UnsupportedOperationException("MainMenuController.exitGame not implemented yet");
    }
}
