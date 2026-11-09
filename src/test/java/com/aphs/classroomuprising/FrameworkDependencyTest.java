package com.aphs.classroomuprising;

import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.GdxNativesLoader;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

/** Dependency/native checks only: never creates a game window or starts either engine. */
class FrameworkDependencyTest {
    @Test void fxglAndJavafxApisAreAvailable() {
        var settings = new GameSettings();
        settings.setTitle("Dependency test");
        assertEquals("Dependency test", settings.getTitle());
        var entity = new Entity();
        entity.setPosition(4, 7);
        assertEquals(4, entity.getX());
        assertEquals(7, entity.getY());
        assertNotNull(new FXMLLoader());
    }

    @Test void javafxHasExactlyOneNativePlatformVariant() throws Exception {
        String os = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        String nativeFile = os.contains("mac") ? "libglass.dylib" : os.contains("win") ? "glass.dll" : "libglass.so";
        var resources = java.util.Collections.list(FXMLLoader.class.getClassLoader().getResources(nativeFile));
        assertEquals(1, resources.size(), "JavaFX native artifacts must not mix host architectures: " + resources);
    }

    @Test void libgdxNativePixmapWorksWithoutAnOpenGlWindow() {
        GdxNativesLoader.load();
        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try {
            pixmap.drawPixel(0, 0, 0x123456ff);
            assertEquals(0x123456ff, pixmap.getPixel(0, 0));
        } finally {
            pixmap.dispose();
        }
    }

    @Test void lwjglDesktopBackendAndItsNativeLibraryLoad() {
        var configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Dependency test");
        assertNotNull(configuration);
        // This native GLFW query needs neither glfwInit() nor a window/main-thread event loop.
        assertNotNull(GLFW.glfwGetVersionString());
    }
}
