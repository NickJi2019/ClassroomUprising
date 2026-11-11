package com.aphs.classroomuprising.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The big announcement in the middle of the screen while an item is in use: a jagged slanted box
 * with the item's picture, its name and what it is doing. It snaps in when the item is used and
 * stays in front until the item has done its work ({@link #finish}), then shows the result for a
 * moment and snaps out. It only announces; the core applies the item.
 */
final class ItemBanner {
    private static final double W = ClassroomApp.WIDTH, H = ClassroomApp.HEIGHT;
    private static final double BOX_W = 780, BOX_H = 168;

    private final Pane layer;
    private final Group banner;
    private final Label kind = new Label();
    private final Label name = new Label();
    private final Label detail = new Label();
    private final Rectangle sweep = new Rectangle(-120, -20, 60, BOX_H + 50);
    private boolean leaving;

    private ItemBanner(Pane layer, Node icon) {
        this.layer = layer;
        Polygon shard = new Polygon(-14, 10, BOX_W - 30, -6, BOX_W + 6, BOX_H + 10, 20, BOX_H + 22);
        shard.getStyleClass().add("item-banner-shard");
        Polygon box = new Polygon(0, 0, BOX_W - 24, -14, BOX_W, BOX_H, 26, BOX_H + 12);
        box.getStyleClass().add("item-banner-box");

        kind.getStyleClass().addAll("display", "tag-label");
        Slant kindTag = new Slant(kind);
        kindTag.getStyleClass().add("tag-caret");
        kindTag.setMaxWidth(Pane.USE_PREF_SIZE);
        name.getStyleClass().addAll("display", "item-banner-name");
        detail.getStyleClass().add("item-banner-detail");
        detail.setWrapText(true);
        detail.setMaxWidth(BOX_W - 260);
        VBox words = new VBox(2, kindTag, name, detail);
        words.setAlignment(Pos.CENTER_LEFT);

        StackPane medal = new StackPane(icon);
        medal.getStyleClass().add("item-banner-medal");
        medal.setMinSize(118, 118);
        medal.setMaxSize(118, 118);
        HBox content = new HBox(26, medal, words);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setLayoutX(44);
        content.setLayoutY(18);

        sweep.getStyleClass().add("item-banner-sweep");
        sweep.setRotate(14);
        Group shine = new Group(sweep);   // the shine only runs across the box
        shine.setClip(new Polygon(0, 0, BOX_W - 24, -14, BOX_W, BOX_H, 26, BOX_H + 12));
        banner = new Group(shard, box, content, shine);
        banner.setLayoutX((W - BOX_W) / 2);
        banner.setLayoutY((H - BOX_H) / 2 - 30);
        banner.setMouseTransparent(true);
        banner.setCache(true);   // slides as one bitmap; redrawn only when its text changes
        banner.setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    /** Slides the banner in and keeps it there until {@link #finish} or {@link #dismiss}. */
    static ItemBanner show(Pane layer, String kind, String name, String detail, Node icon) {
        ItemBanner shown = new ItemBanner(layer, icon);
        shown.kind.setText(kind);
        shown.name.setText(name);
        shown.detail.setText(detail);
        layer.getChildren().add(shown.banner);
        shown.banner.setTranslateX(-W);
        shown.banner.setScaleY(0.6);
        Timeline in = new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(shown.banner.translateXProperty(), 0, MotionTokens.OUT),
                new KeyValue(shown.banner.scaleYProperty(), 1, MotionTokens.OUT)));
        new SequentialTransition(in, shown.shine()).play();
        Sfx.CUTIN.play(layer);
        return shown;
    }

    /** What the item is doing now (for example its countdown). */
    void update(String what) {
        if (!what.equals(detail.getText())) detail.setText(what);
    }

    /** The item has done its work: the banner shows the result for {@code hold}, then leaves. */
    void finish(String newKind, String newName, String what, Duration hold) {
        if (leaving) return;
        leaving = true;
        kind.setText(newKind);
        name.setText(newName);
        detail.setText(what);
        Timeline pop = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(banner.scaleXProperty(), 1.06), new KeyValue(banner.scaleYProperty(), 1.06)),
                new KeyFrame(Duration.millis(180), new KeyValue(banner.scaleXProperty(), 1, MotionTokens.OUT),
                        new KeyValue(banner.scaleYProperty(), 1, MotionTokens.OUT)));
        new SequentialTransition(new javafx.animation.ParallelTransition(pop, shine()), new PauseTransition(hold), out()).play();
    }

    /** Leaves at once (the exam ended while the item was working). */
    void dismiss() {
        if (leaving) return;
        leaving = true;
        out().play();
    }

    boolean showing() { return !leaving; }

    private Timeline shine() {
        sweep.setTranslateX(0);
        return new Timeline(new KeyFrame(Duration.millis(420), new KeyValue(sweep.translateXProperty(), BOX_W + 200, MotionTokens.OUT)));
    }

    private Timeline out() {
        Timeline out = new Timeline(new KeyFrame(Duration.millis(220),
                new KeyValue(banner.translateXProperty(), W, Interpolator.EASE_IN),
                new KeyValue(banner.opacityProperty(), 0.4)));
        out.setOnFinished(e -> layer.getChildren().remove(banner));
        return out;
    }

    /** A rubber duck, drawn in a few shapes (no art is delivered for it). */
    static Node duck() {
        Ellipse body = new Ellipse(4, 14, 34, 22);
        Ellipse wing = new Ellipse(-2, 12, 17, 11);
        Circle head = new Circle(20, -14, 16);
        Polygon beak = new Polygon(33, -17, 50, -11, 33, -6);
        Circle eye = new Circle(24, -18, 3.2);
        Ellipse gleam = new Ellipse(14, -20, 4, 2.5);
        body.setFill(Color.web("#FFD23F"));
        head.setFill(Color.web("#FFD23F"));
        wing.setFill(Color.web("#F2B705"));
        beak.setFill(Color.web("#FF8A3D"));
        eye.setFill(Color.web("#0B0F2A"));
        gleam.setFill(Color.web("#FFF6C8"));
        Group duck = new Group(body, wing, head, beak, eye, gleam);
        duck.setRotate(-8);
        return duck;
    }
}
