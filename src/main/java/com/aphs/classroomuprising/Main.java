package com.aphs.classroomuprising;

import com.aphs.classroomuprising.ui.ClassroomApp;
import javafx.application.Application;

/**
 * Application entry point: opens the game window. Command-line arguments are deliberately
 * ignored; there are no modes, debug commands or day jumps. Use the IDE/JVM debugger instead.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        Application.launch(ClassroomApp.class);
    }
}
