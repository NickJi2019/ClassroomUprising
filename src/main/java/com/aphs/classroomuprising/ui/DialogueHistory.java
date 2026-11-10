package com.aphs.classroomuprising.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines a dialogue box has shown, oldest first, each with the lecture slide that was up
 * while it played, so the log can replay a line together with its slide. A script starts with
 * no slide until one of its lines cues one; a cue lasts until the next cue. Window state only:
 * never saved and never part of the game.
 */
final class DialogueHistory {
    /** One shown line and the slide on the projector at the time (null: no slide). */
    record Entry(DialogueScript.Line line, String slide) {}

    private final List<Entry> entries = new ArrayList<>();
    private String slide;

    /** A new script begins: nothing is on the projector until it cues a slide. */
    void beginScript() { slide = null; }

    /** A line has started. */
    void shown(DialogueScript.Line line) {
        if (line.slide() != null) slide = line.slide();
        entries.add(new Entry(line, slide));
    }

    List<Entry> entries() { return List.copyOf(entries); }

    /**
     * The first of {@code rows} visible lines in a log of {@code size} lines that keeps
     * {@code selected} in view, scrolling only as far as needed (the newest line at the bottom).
     */
    static int firstVisible(int size, int selected, int rows) {
        if (size <= rows) return 0;
        return Math.max(0, Math.min(selected - rows + 1, size - rows));
    }
}
