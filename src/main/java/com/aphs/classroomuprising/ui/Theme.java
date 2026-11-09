package com.aphs.classroomuprising.ui;

import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;

/**
 * Installs theme.css and picks installed fonts. JavaFX CSS has no font fallback list,
 * so the first installed family from each list is written into a tiny generated stylesheet.
 */
final class Theme {
    private Theme() {}

    static void apply(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(Theme.class.getResource("theme.css")).toExternalForm());
        String body = pick("Avenir Next", "Segoe UI", "Helvetica Neue", "Arial");
        String display = pick("Anton", "Impact", "Haettenschweiler", "Arial Black");
        String mono = pick("JetBrains Mono", "Menlo", "Consolas", "DejaVu Sans Mono");
        String css = ".root{-fx-font-family:\"%s\";}.display{-fx-font-family:\"%s\";}.mono{-fx-font-family:\"%s\";}"
                .formatted(body, display, mono);
        scene.getStylesheets().add("data:text/css;base64,"
                + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8)));
    }

    private static String pick(String... families) {
        var installed = new HashSet<>(Font.getFamilies());
        for (String family : families) if (installed.contains(family)) return family;
        return Font.getDefault().getFamily();
    }
}
