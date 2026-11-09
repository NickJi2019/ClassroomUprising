package com.aphs.classroomuprising.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** A full-screen stop with an explanation, for states the UI cannot continue from yet. */
final class BlockedScreen extends Screen {
    private final String title;
    private final Node reason;

    @FXML private Label heading;
    @FXML private VBox notice;
    @FXML private ChoiceList choices;

    BlockedScreen(AppContext ctx, String title, Node reason) {
        super(ctx);
        this.title = title;
        this.reason = reason;
    }

    @Override
    protected Parent build() {
        StackPane root = Views.load("blocked.fxml", this);
        root.getChildren().add(0, new Backdrop(new Backdrop.Glyph("throw new PendingRule();", 160, 470, 70, false)));
        heading.setText(title);
        notice(notice, reason);
        choices.setChoices(List.of(ctx.profile() != null
                ? ChoiceList.Choice.of("RETURN TO TITLE", "Your last day-end save is kept", ctx::toTitle).tone("cyan")
                : ChoiceList.Choice.of("QUIT", null, Platform::exit)));
        return root;
    }

    @Override
    protected void focus() { choices.requestFocus(); }
}
