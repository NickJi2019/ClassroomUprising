package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MainEntryPointTest {
    @TempDir Path temp;

    @Test void entryPointIsQuietWithoutAFrontend() {
        checkQuiet(new String[0]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--gui", "--calendar", "--demo", "--lab-demo", "--rules", "--help", "--unknown"})
    void formerCommandsHaveNoBehavior(String argument) {
        Path file = temp.resolve("must-not-be-created.properties");
        checkQuiet(new String[] {argument, file.toString()});
        assertFalse(Files.exists(file));
    }

    private static void checkQuiet(String[] args) {
        var out = System.out;
        var err = System.err;
        var captured = new ByteArrayOutputStream();
        try (var stream = new PrintStream(captured)) {
            System.setOut(stream);
            System.setErr(stream);
            assertDoesNotThrow(() -> Main.main(args));
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
        assertEquals("", captured.toString());
    }
}
