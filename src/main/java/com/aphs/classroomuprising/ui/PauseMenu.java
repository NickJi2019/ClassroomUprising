package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.GameState;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.function.Function;

/**
 * The ESC menu over a game screen. Moving through the list previews each page immediately;
 * it only reads the snapshot and never changes game state. The SOUND page is the one page you
 * can enter (Enter on its tab): it sets the window's volume, which is not part of the game.
 */
final class PauseMenu extends Screen {
    private record Page(String label, Function<GameState, Node> content) {}

    private final Runnable close;
    private final javafx.scene.image.Image backdrop;
    private final List<Page> pages = List.of(
            new Page("STATUS", PausePanels::status),
            new Page("CALENDAR", PausePanels::calendar),
            new Page("GRADES", PausePanels::grades),
            new Page("SOUND", view -> soundPage()));
    private SoundPanel sound;

    @FXML private ChoiceList tabs;
    @FXML private StackPane content;

    /** @param backdrop a capture of the screen being paused, used as the menu's background */
    PauseMenu(AppContext ctx, Runnable close, javafx.scene.image.Image backdrop) {
        super(ctx);
        this.close = close;
        this.backdrop = backdrop;
    }

    @Override
    protected Parent build() {
        StackPane root = Views.load("pause.fxml", this);
        if (backdrop != null) root.getChildren().add(0, new MenuBackdrop(backdrop));
        tabs.setOnSelect(this::showPage);
        return root;
    }

    @Override
    protected void onShown() {
        var choices = new java.util.ArrayList<ChoiceList.Choice>();
        for (Page page : pages)
            choices.add(ChoiceList.Choice.of(page.label(), null, page.label().equals("SOUND") ? this::enterSound : tabs::unlock));
        choices.add(ChoiceList.Choice.of("RESUME", "ESC", close).tone("cyan"));
        choices.add(ChoiceList.Choice.of("QUIT TO TITLE", "Progress since the last day-end save is lost", ctx::toTitle)
                .tone("danger"));
        tabs.setChoices(choices);
    }

    @Override
    protected void focus() { tabs.requestFocus(); }

    private javafx.scene.Node soundPage() {
        sound = new SoundPanel(ctx.audio());
        sound.setOnBack(() -> {
            tabs.unlock();
            tabs.requestFocus();
        });
        javafx.scene.layout.VBox page = new javafx.scene.layout.VBox(12,
                PausePanels.heading("SOUND", "Volume for this computer · not part of your save"), sound);
        page.getStyleClass().add("pause-page");
        return page;
    }

    private void enterSound() {
        if (sound != null) sound.requestFocus();
        else tabs.unlock();
    }

    private void showPage(int index) {
        if (index >= pages.size()) return;
        if (sound != null && sound.isFocused()) {
            // The pointer moved to another tab while the volume rows had the keys: hand them back.
            tabs.unlock();
            tabs.requestFocus();
        }
        sound = null;
        Node page = pages.get(index).content().apply(ctx.session().view());
        content.getChildren().setAll(page);
        Motion.slideIn(page, 0);
    }
}
