package com.aphs.classroomuprising.ui.scene;

import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

/**
 * A colour grade applied over background art: a tint layer blended onto the image, plus a
 * saturation/brightness adjustment of the image itself. Values come from the asset manifest,
 * so artists can tune each time of day without code changes.
 *
 * @param tint        colour of the tint layer
 * @param mode        how the tint layer blends onto the image
 * @param tintOpacity 0 (no tint) to 1
 * @param saturation  -1 (grey) to 1, 0 leaves the art unchanged
 * @param brightness  -1 to 1, 0 leaves the art unchanged
 */
public record Grade(Color tint, BlendMode mode, double tintOpacity, double saturation, double brightness) {
    public static final Grade NONE = new Grade(Color.BLACK, BlendMode.SRC_OVER, 0, 0, 0);

    public Grade {
        if (tint == null || mode == null) throw new IllegalArgumentException("Grade needs a tint and a blend mode.");
        if (!(tintOpacity >= 0 && tintOpacity <= 1) || !(saturation >= -1 && saturation <= 1)
                || !(brightness >= -1 && brightness <= 1))
            throw new IllegalArgumentException("Grade values out of range.");
    }

    /** Parses "#RRGGBB, MODE, tintOpacity, saturation, brightness". */
    static Grade parse(String text) {
        String[] parts = text.split(",");
        if (parts.length != 5) throw new IllegalArgumentException("Grade needs 5 comma-separated values: " + text);
        return new Grade(Color.web(parts[0].strip()), BlendMode.valueOf(parts[1].strip().toUpperCase(java.util.Locale.ROOT)),
                Double.parseDouble(parts[2].strip()), Double.parseDouble(parts[3].strip()), Double.parseDouble(parts[4].strip()));
    }
}
