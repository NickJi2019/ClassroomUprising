package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ui.scene.SceneView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.random.RandomGenerator;

/** New game, continue from the day-end save, or quit. */
final class TitleScreen extends Screen {
    private static final int MAX_NAME = 24;

    @FXML private SceneView scene;
    @FXML private ChoiceList menu;
    @FXML private VBox namePanel;
    @FXML private TextField nameField;
    @FXML private Label nameError;
    @FXML private ChoiceList nameActions;
    @FXML private VBox notice;
    @FXML private Label demoLabel;

    TitleScreen(AppContext ctx) { super(ctx); }

    @Override
    protected Parent build() {
        StackPane root = Views.load("title.fxml", this);
        ctx.stage(scene, "title", null, "title");
        root.getChildren().add(1, new Backdrop(
                new Backdrop.Glyph("class Uprising {", 420, 70, 92, false),
                new Backdrop.Glyph("// retake", 760, 470, 120, false),
                new Backdrop.Glyph("{ ; }", 80, 560, 70, false)));
        demoLabel.setText(ctx.demoLabel());
        nameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= MAX_NAME ? change : null));
        nameField.textProperty().addListener((obs, old, now) -> nameError.setText(""));
        nameField.setOnAction(e -> startNewGame());
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) nameActions.requestFocus();
        });
        nameActions.setChoices(List.of(
                ChoiceList.Choice.of("START", null, this::startNewGame),
                ChoiceList.Choice.of("BACK", null, this::closeNamePanel)));
        showMenu();
        return root;
    }

    @Override
    protected void focus() {
        if (namePanel.isVisible()) nameField.requestFocus();
        else menu.requestFocus();
    }

    private void showMenu() {
        boolean hasSave = GameSession.saveExists(ctx.savePath());
        menu.setChoices(List.of(
                ChoiceList.Choice.of("NEW GAME",
                        hasSave ? "Your save is replaced when Day 1 ends" : "Start the summer retake", this::openNamePanel),
                ChoiceList.Choice.of("CONTINUE",
                        hasSave ? "Resume from your last day-end save" : "No save yet", this::continueGame).enabled(hasSave),
                ChoiceList.Choice.of("QUIT", null, Platform::exit)));
    }

    private void openNamePanel() {
        notice(notice, null);
        menu.setVisible(false);
        namePanel.setManaged(true);
        namePanel.setVisible(true);
        Motion.slideIn(namePanel, 0);
        nameActions.unlock();
        nameField.requestFocus();
    }

    private void closeNamePanel() {
        namePanel.setVisible(false);
        namePanel.setManaged(false);
        menu.setVisible(true);
        showMenu();
        menu.requestFocus();
    }

    private void startNewGame() {
        String name = nameField.getText().strip();
        if (name.isEmpty()) {
            nameError.setText("Enter a name first.");
            nameActions.unlock();
            nameField.requestFocus();
            return;
        }
        ctx.begin(GameSession.newGame(ctx.profile(), name, RandomGenerator.getDefault(), ctx.savePath(), ctx.combatRules()));
        ctx.router().show(new OpeningScreen(ctx), null);
    }

    private void continueGame() {
        try {
            ctx.begin(GameSession.load(ctx.profile(), ctx.savePath(), RandomGenerator.getDefault(), ctx.combatRules()));
            ctx.advance();
        } catch (IOException e) {
            notice(notice, Diagnostic.of(new Outcome.Rejected("Your save can't be loaded: " + e.getMessage())));
            menu.unlock();
        }
    }
}
