package com.aphs.classroomuprising;

/**
 * Saves and restores the complete game state using File I/O.
 *
 * @author TeamPike
 * @since 1.0
 */
public class SaveManager {

    private final String savePath;

    /**
     * Creates a save manager.
     *
     * @param savePath the location of the save file
     */
    public SaveManager(String savePath) {
        this.savePath = savePath;
    }

    /**
     * Writes the game state to the save file.
     *
     * @param state the state to save
     * @return true if the save succeeded
     */
    public boolean save(GameState state) {
        throw new UnsupportedOperationException("SaveManager.save not implemented yet");
    }

    /**
     * Reads and validates the game state from the save file.
     *
     * @return the restored game state
     */
    public GameState load() {
        throw new UnsupportedOperationException("SaveManager.load not implemented yet");
    }

    /**
     * Checks that a loaded state is complete and consistent.
     *
     * @param state the state to check
     * @return true if the state is valid
     */
    private boolean validate(GameState state) {
        throw new UnsupportedOperationException("SaveManager.validate not implemented yet");
    }
}
