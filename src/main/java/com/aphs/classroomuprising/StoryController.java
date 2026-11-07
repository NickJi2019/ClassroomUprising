package com.aphs.classroomuprising;

/**
 * Unlocks content by date and plays story scenes (IPO 2 and 8).
 *
 * @author TeamPike
 * @since 1.0
 */
public class StoryController {

    private final ContentCatalog catalog;
    private final GameUI ui;

    /** The story being played; null when no story is active. */
    private StoryEvent activeEvent;
    private int sceneIndex;

    /**
     * Creates the story controller.
     *
     * @param catalog the story and topic definitions
     * @param ui shows the scenes
     */
    public StoryController(ContentCatalog catalog, GameUI ui) {
        this.catalog = catalog;
        this.ui = ui;
    }

    /**
     * Marks topics and stories available when their date and prerequisites are met.
     *
     * @param state the game state to update
     */
    public void refreshUnlocks(GameState state) {
        throw new UnsupportedOperationException("StoryController.refreshUnlocks not implemented yet");
    }

    /**
     * Plays the opening story, which must finish before normal daily gameplay.
     *
     * @param state the game state to update
     */
    public void playIntroduction(GameState state) {
        throw new UnsupportedOperationException("StoryController.playIntroduction not implemented yet");
    }

    /**
     * Plays any stories due at the current point in the day.
     *
     * @param state the game state to update
     */
    public void playDueStories(GameState state) {
        throw new UnsupportedOperationException("StoryController.playDueStories not implemented yet");
    }

    /**
     * Moves to the next scene. After the last scene, records completion and applies one-time effects.
     *
     * @param state the game state to update
     */
    public void advanceScene(GameState state) {
        throw new UnsupportedOperationException("StoryController.advanceScene not implemented yet");
    }

    /**
     * Checks whether a required story still blocks the daily routine.
     *
     * @param state the current game state
     * @return true if a required story is unfinished
     */
    public boolean hasPendingRequiredStory(GameState state) {
        throw new UnsupportedOperationException("StoryController.hasPendingRequiredStory not implemented yet");
    }
}
