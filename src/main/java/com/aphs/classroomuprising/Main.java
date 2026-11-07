package com.aphs.classroomuprising;

/**
 * Application entry point. Builds the controllers and shows the main menu.
 *
 * @author TeamPike
 * @since 1.0
 */
public class Main {

    /** Location of the save file used by {@link SaveManager}. */
    private static final String SAVE_PATH = "classroom-uprising.sav";

    /**
     * Starts the application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        GameUI ui = new GameUI();
        // The menu is not used yet: GameUI has no link back to the controllers (see README, Design gaps).
        createMainMenu(ui);
        ui.showMainMenu();
    }

    /**
     * Connects the controllers following the associations in the class diagram.
     *
     * @param ui the user interface shared by the controllers
     * @return the main menu controller, connected to the rest of the application
     */
    static MainMenuController createMainMenu(GameUI ui) {
        ContentCatalog catalog = new ContentCatalog();
        StoryController storyController = new StoryController(catalog, ui);
        ActivityController activityController = new ActivityController(catalog);
        AssessmentController assessmentController = new AssessmentController(new MrReward());
        SaveManager saveManager = new SaveManager(SAVE_PATH);
        GameController gameController = new GameController(
                ui, storyController, activityController, assessmentController, saveManager);
        return new MainMenuController(gameController, ui);
    }
}
