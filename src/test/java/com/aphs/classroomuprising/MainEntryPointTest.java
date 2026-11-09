package com.aphs.classroomuprising;

import com.aphs.classroomuprising.ui.ClassroomApp;
import javafx.application.Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Main now opens the game window, so it cannot be called inside a headless test.
 * These checks keep the entry-point contract: one route into the UI, no argument parsing,
 * no former command routes, no application parameters read by the UI.
 */
class MainEntryPointTest {
    private static final Path SOURCES = Path.of("src/main/java/com/aphs/classroomuprising");

    @Test void mainOpensTheGameWindowOnly() throws IOException {
        String source = Files.readString(SOURCES.resolve("Main.java"));
        assertTrue(source.contains("Application.launch(ClassroomApp.class);"), "Main must launch the game window");
        assertEquals(1, Pattern.compile("\\bargs\\b").matcher(source).results().count(),
                "the argument array may only appear as main's parameter: never read or forwarded");
    }

    @Test void mainHasNoOtherEntryPoints() {
        List<Method> publicMethods = Arrays.stream(Main.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())).toList();
        assertEquals(1, publicMethods.size());
        assertEquals("main", publicMethods.get(0).getName());
        assertTrue(Application.class.isAssignableFrom(ClassroomApp.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--gui", "--calendar", "--demo", "--lab-demo", "--rules", "--help", "getParameters()"})
    void formerCommandRoutesDoNotExist(String route) throws IOException {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            List<Path> hits = files.filter(p -> p.toString().endsWith(".java")).filter(p -> {
                try {
                    return Files.readString(p).contains(route);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }).toList();
            assertEquals(List.of(), hits, "no production source may recognise " + route);
        }
    }
}
