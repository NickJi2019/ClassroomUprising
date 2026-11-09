package com.aphs.classroomuprising.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Loads an FXML layout from this package's resources with the given screen as its controller. */
final class Views {
    private Views() {}

    static <T extends Parent> T load(String name, Object controller) {
        var url = Views.class.getResource(name);
        if (url == null) throw new IllegalStateException("Missing layout resource: " + name);
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(controller);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load layout " + name, e);
        }
    }
}
