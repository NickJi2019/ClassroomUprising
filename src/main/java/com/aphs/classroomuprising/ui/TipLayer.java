package com.aphs.classroomuprising.ui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The tip card: a game-system notice at the top centre of the screen, styled apart from
 * dialogue (no speaker, no portrait, no text box). It drops in after a screen change has
 * finished, stays while the screen is up, and leaves on a click or when the screen changes.
 * It never takes focus or blocks input.
 */
final class TipLayer extends StackPane {
    private HBox card;

    TipLayer() {
        setPickOnBounds(false);
        // Between the HUD's date block (ends at x 336) and its stat chips (start at about x 865).
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(20, 0, 0, 348));
    }

    void show(TutorialTips.Tip tip) {
        clear();
        Label mark = new Label("?");
        mark.getStyleClass().addAll("display", "tip-mark");
        Label kind = new Label("TIP");
        kind.getStyleClass().addAll("display", "tip-kind");
        VBox badge = new VBox(-3, mark, kind);
        badge.setAlignment(Pos.CENTER);
        Slant badgeBox = new Slant(badge);
        badgeBox.getStyleClass().add("tip-badge");

        Label title = new Label(tip.title());
        title.getStyleClass().addAll("display", "tip-title");
        Label body = new Label(tip.body());
        body.getStyleClass().add("tip-body");
        body.setWrapText(true);
        Label close = new Label("CLICK TO DISMISS");
        close.getStyleClass().addAll("mono", "tip-close");
        VBox text = new VBox(3, title, body, close);

        HBox next = new HBox(14, badgeBox, text);
        next.getStyleClass().add("tip-card");
        next.setAlignment(Pos.CENTER_LEFT);
        next.setMaxWidth(500);
        next.setPrefWidth(500);
        next.setMaxHeight(USE_PREF_SIZE);
        next.setOnMouseClicked(e -> clear());
        card = next;
        getChildren().add(next);
        Sfx.TIP.play(this);

        // In: drops a little and opens left to right in drawn steps; the badge punches after.
        Rectangle wipe = new Rectangle(0, 400);
        next.setClip(wipe);
        next.setTranslateY(-18);
        Timeline open = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(wipe.widthProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(wipe.widthProperty(), 620, MotionTokens.steps(6))));
        open.setOnFinished(e -> next.setClip(null));
        TranslateTransition drop = new TranslateTransition(MotionTokens.BASE, next);
        drop.setToY(0);
        drop.setInterpolator(MotionTokens.OUT);
        new ParallelTransition(open, drop).play();
        badgeBox.setScaleX(0.6);
        badgeBox.setScaleY(0.6);
        new Timeline(
                new KeyFrame(Duration.millis(150), new KeyValue(badgeBox.scaleXProperty(), 0.6),
                        new KeyValue(badgeBox.scaleYProperty(), 0.6)),
                new KeyFrame(Duration.millis(260), new KeyValue(badgeBox.scaleXProperty(), 1.12, MotionTokens.OUT),
                        new KeyValue(badgeBox.scaleYProperty(), 1.12, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(360), new KeyValue(badgeBox.scaleXProperty(), 1, MotionTokens.IN_OUT),
                        new KeyValue(badgeBox.scaleYProperty(), 1, MotionTokens.IN_OUT))).play();
    }

    /** Removes the current tip, if any, with a short lift and fade. */
    void clear() {
        if (card == null) return;
        HBox leaving = card;
        card = null;
        leaving.setMouseTransparent(true);
        FadeTransition fade = new FadeTransition(MotionTokens.QUICK, leaving);
        fade.setToValue(0);
        TranslateTransition lift = new TranslateTransition(MotionTokens.QUICK, leaving);
        lift.setByY(-12);
        ParallelTransition out = new ParallelTransition(fade, lift);
        out.setOnFinished(e -> getChildren().remove(leaving));
        out.play();
    }

    boolean showing() { return card != null; }
}
