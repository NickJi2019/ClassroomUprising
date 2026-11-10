package com.aphs.classroomuprising.ui.scene;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.List;

/**
 * One character's standing art. The node's origin is the character's anchor point
 * (usually the feet), so placing it on a slot is a single relocate. Expressions crossfade;
 * the figure breathes and sways a little; a character who is not speaking steps back and dims.
 */
final class CharacterView extends Group {
    private static final Interpolator SETTLE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);

    private final AssetStore assets;
    private final AssetManifest.CharacterSpec spec;
    private final Group figure = new Group();
    private final ColorAdjust shade = new ColorAdjust();
    private final Scale breath = new Scale(1, 1, 0, 0);
    private final Timeline idle;
    private ImageView current;
    private String expression;

    CharacterView(AssetStore assets, AssetManifest.CharacterSpec spec, String expression) {
        this.assets = assets;
        this.spec = spec;
        figure.setEffect(shade);
        figure.getTransforms().add(breath);
        getChildren().add(figure);
        setMouseTransparent(true);
        setExpression(expression, false);

        idle = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(breath.yProperty(), 1.0), new KeyValue(figure.translateXProperty(), -1.5)),
                new KeyFrame(Duration.seconds(1.8), new KeyValue(breath.yProperty(), 1.009, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(2.7), new KeyValue(figure.translateXProperty(), 1.5, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(3.6), new KeyValue(breath.yProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(5.4), new KeyValue(figure.translateXProperty(), -1.5, Interpolator.EASE_BOTH)));
        idle.setCycleCount(Animation.INDEFINITE);
    }

    String id() { return spec.id(); }

    List<Animation> animations() { return List.of(idle); }

    /** Switches expression; unknown expressions fall back to neutral. */
    void setExpression(String name, boolean animate) {
        String wanted = name == null ? "neutral" : name;
        if (wanted.equals(expression)) return;
        expression = wanted;
        ImageView view = new ImageView(assets.character(spec, wanted));
        double scale = spec.height() / view.getImage().getHeight();
        view.setFitHeight(spec.height());
        view.setFitWidth(view.getImage().getWidth() * scale);
        view.setSmooth(true);
        view.relocate(-view.getFitWidth() * spec.anchorX(), -view.getFitHeight() * spec.anchorY());
        ImageView previous = current;
        current = view;
        figure.getChildren().add(view);
        if (previous == null) return;
        if (!animate) {
            figure.getChildren().remove(previous);
            return;
        }
        view.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(180), view);
        fade.setToValue(1);
        fade.setOnFinished(e -> figure.getChildren().remove(previous));
        fade.play();
    }

    /**
     * Turns the figure to look {@code towardsRight} (or left), mirroring art that faces the other
     * way, about the anchor so it stays on its slot. Art without a facing is left as it is.
     */
    void face(boolean towardsRight) {
        if (spec.faces() == null || spec.faces().equals("right") == towardsRight) return;
        figure.getTransforms().add(new Scale(-1, 1, 0, 0));
    }

    /** Speaking characters stand forward at full colour; others step back and dim. */
    void setSpeaking(boolean focused) {
        new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(shade.brightnessProperty(), focused ? 0 : -0.32, SETTLE),
                new KeyValue(shade.saturationProperty(), focused ? 0 : -0.3, SETTLE),
                new KeyValue(translateYProperty(), focused ? 0 : 10, SETTLE),
                new KeyValue(scaleXProperty(), focused ? 1 : 0.985, SETTLE),
                new KeyValue(scaleYProperty(), focused ? 1 : 0.985, SETTLE))).play();
    }

    /** Slides in from one side and fades up. */
    Animation enter(boolean fromLeft, Duration delay) {
        figure.setOpacity(0);
        figure.setTranslateX(fromLeft ? -90 : 90);
        Timeline slide = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(figure.translateXProperty(), fromLeft ? -90 : 90)),
                new KeyFrame(Duration.millis(460), new KeyValue(figure.translateXProperty(), 0, SETTLE)));
        FadeTransition fade = new FadeTransition(Duration.millis(300), figure);
        fade.setToValue(1);
        ParallelTransition both = new ParallelTransition(slide, fade);
        both.setDelay(delay);
        both.setOnFinished(e -> idle.play());
        return both;
    }
}
