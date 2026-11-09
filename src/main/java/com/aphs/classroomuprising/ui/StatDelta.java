package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.StatChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Display-only differences between two snapshots, or the stated values of a configured effect.
 * It never calculates game rules; the core already applied them.
 */
record StatDelta(String stat, double change) {
    private static final double EPSILON = 1e-9;

    /** What actually changed between two snapshots (SAN uses the effective value the HUD shows). */
    static List<StatDelta> between(GameState before, GameState after) {
        var out = new ArrayList<StatDelta>();
        add(out, "HP", after.student().hp() - before.student().hp());
        add(out, "SAN", after.effectiveSan() - before.effectiveSan());
        add(out, "VIBE", after.student().codingVibe() - before.student().codingVibe());
        for (int i = 0; i < 6; i++)
            add(out, "K" + (i + 1), after.student().unitKnowledge().get(i) - before.student().unitKnowledge().get(i));
        return out;
    }

    /** The configured values of an effect, as written in the Demo profile (before core modifiers). */
    static List<StatDelta> of(StatChange effect, double scale) {
        var out = new ArrayList<StatDelta>();
        add(out, "HP", effect.hp() * scale);
        add(out, "SAN", effect.san() * scale);
        add(out, "VIBE", effect.codingVibe() * scale);
        for (int i = 0; i < 6; i++) add(out, "K" + (i + 1), effect.unitKnowledge().get(i) * scale);
        return out;
    }

    boolean positive() { return change > 0; }

    String text() { return stat + " " + signed(change); }

    static String signed(double value) {
        return (value >= 0 ? "+" : "−") + number(Math.abs(value));
    }

    static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < EPSILON) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void add(List<StatDelta> out, String stat, double change) {
        if (Math.abs(change) > EPSILON) out.add(new StatDelta(stat, change));
    }
}
