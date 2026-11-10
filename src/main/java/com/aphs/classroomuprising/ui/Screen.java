package com.aphs.classroomuprising.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/**
 * One full-canvas scene of the game. Layout comes from an FXML file; the subclass wires
 * player input to {@link GameSession} actions and presents the outcome.
 */
abstract class Screen {
    protected final AppContext ctx;
    private Parent root;

    protected Screen(AppContext ctx) { this.ctx = ctx; }

    final Parent root() {
        if (root == null) root = build();
        return root;
    }

    /** Builds the node tree once. */
    protected abstract Parent build();

    /** Called each time the screen becomes visible. */
    protected void onShown() {}

    /** Called when the screen is replaced. */
    protected void onHidden() {}

    /** Gives keyboard focus to the screen's main control. */
    protected void focus() { root().requestFocus(); }

    /**
     * The music cue for this screen (a {@code bgm.<cue>} key in the asset manifest), or null to
     * keep whatever is playing. A cue that is already playing carries on without restarting.
     */
    protected String music() { return null; }

    /** Whether ESC may open the menu over this screen. */
    protected boolean allowsPause() { return false; }

    /** Replaces the contents of a notice area with one card (or clears it with null). */
    protected static void notice(Pane area, Node card) {
        if (card == null) area.getChildren().clear();
        else area.getChildren().setAll(card);
    }
}
