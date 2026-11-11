package com.aphs.classroomuprising.ui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;

/**
 * A container drawn as a parallelogram at the game's one slant angle (about 14 degrees).
 * The slant offset follows the height, so every bar shares the same angle at any width.
 * Colours come from CSS ({@code .slant-bg}); the content itself is not skewed.
 *
 * <p>The parallelogram is kept as a cached bitmap and its points are only replaced when the size
 * changes: a slanted shape is otherwise rasterized again on the CPU every frame the screen
 * repaints (which is every frame while anything animates), and the game has many of them.</p>
 */
public class Slant extends StackPane {
    /** tan(14 degrees), the angle of the "//" motif. */
    static final double SLOPE = 0.25;
    private final Polygon background = new Polygon();
    private double shapedWidth = -1, shapedHeight = -1;

    public Slant() {
        getStyleClass().add("slant");
        background.getStyleClass().add("slant-bg");
        background.setManaged(false);
        background.setCache(true);
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
        if (width == shapedWidth && height == shapedHeight) return;
        shapedWidth = width;
        shapedHeight = height;
        double offset = height * SLOPE;
        background.getPoints().setAll(offset, 0.0, width, 0.0, width - offset, height, 0.0, height);
    }
}
