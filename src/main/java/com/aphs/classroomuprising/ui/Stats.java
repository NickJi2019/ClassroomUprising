package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.StudentState;

import java.util.ArrayList;
import java.util.List;

/**
 * How the player's stats are presented: HP as a number, SAN as a bar (a share of a full bar),
 * and Coding Vibe plus the six units' knowledge together as one XP total that can be opened to
 * show its parts. Display only: the core keeps and changes the separate values.
 */
final class Stats {
    private Stats() {}

    /**
     * The SAN value of a full bar. A display scale: the core has no SAN maximum yet (requested
     * from the backend), so a value above it shows a full bar.
     */
    static final double SAN_FULL = 100;

    /** One part of the XP total. */
    record Part(String name, double value) {}

    /** How full the SAN bar is, from 0 to 1. */
    static double sanFraction(double san) {
        return Math.max(0, Math.min(1, san / SAN_FULL));
    }

    /** A SAN amount as a share of the full bar, in percent (for changes: "−4%"). */
    static double sanPercent(double san) { return san / SAN_FULL * 100; }

    /** The XP total: Coding Vibe and every unit's knowledge. */
    static double xp(StudentState student) {
        return xp(student.codingVibe(), student.unitKnowledge());
    }

    static double xp(double codingVibe, List<Double> unitKnowledge) {
        double total = codingVibe;
        for (double value : unitKnowledge) total += value;
        return total;
    }

    /** What the XP total is made of, in a fixed order: Coding Vibe, then units 1 to 6. */
    static List<Part> xpParts(StudentState student) {
        var parts = new ArrayList<Part>();
        parts.add(new Part("CODING VIBE", student.codingVibe()));
        for (int i = 0; i < student.unitKnowledge().size(); i++)
            parts.add(new Part("U" + (i + 1), student.unitKnowledge().get(i)));
        return List.copyOf(parts);
    }
}
