package com.aphs.classroomuprising.ui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;

/**
 * A container drawn as a parallelogram at the game's one slant angle (about 14 degrees).
 * The slant offset follows the height, so every bar shares the same angle at any width.
 * Colours come from CSS ({@code .slant-bg}); the content itself is not skewed.
 */
public class Slant extends StackPane {
    /** tan(14 degrees), the angle of the "//" motif. */
    static final double SLOPE = 0.25;
    private final Polygon background = new Polygon();

    public Slant() {
        getStyleClass().add("slant");
        background.getStyleClass().add("slant-bg");
        background.setManaged(false);
        getChildren().add(background);
    }

    public Slant(Node... content) {
        this();
        getChildren().addAll(content);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double width = getWidth();
        double height = getHeight();
        double offset = height * SLOPE;
        background.getPoints().setAll(offset, 0.0, width, 0.0, width - offset, height, 0.0, height);
    }
}
