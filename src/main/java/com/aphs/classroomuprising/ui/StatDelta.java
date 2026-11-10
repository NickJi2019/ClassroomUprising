package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.StatChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Display-only differences between two snapshots, or the stated values of a configured effect.
 * It never calculates game rules; the core already applied them. Shown as {@link Stats} presents
 * the stats: HP as a number, SAN as a share of the bar ("SAN −2%"), and Coding Vibe with the
 * units' knowledge as one XP change.
 */
record StatDelta(String stat, double change) {
    private static final double EPSILON = 1e-9;

    /** What actually changed between two snapshots (SAN uses the effective value the HUD shows). */
    static List<StatDelta> between(GameState before, GameState after) {
        var out = new ArrayList<StatDelta>();
        add(out, "HP", after.student().hp() - before.student().hp());
        add(out, "SAN", after.effectiveSan() - before.effectiveSan());
        add(out, "XP", Stats.xp(after.student()) - Stats.xp(before.student()));
        return out;
    }

    /** The configured values of an effect, as written in the Demo profile (before core modifiers). */
    static List<StatDelta> of(StatChange effect, double scale) {
        var out = new ArrayList<StatDelta>();
        add(out, "HP", effect.hp() * scale);
        add(out, "SAN", effect.san() * scale);
        add(out, "XP", Stats.xp(effect.codingVibe(), effect.unitKnowledge()) * scale);
        return out;
    }

    boolean positive() { return change > 0; }

    String text() { return stat + " " + (stat.equals("SAN") ? san(change) : signed(change)); }

    /** A SAN change as a share of the bar: "+6%", "−4%". */
    static String san(double change) { return signed(Stats.sanPercent(change)) + "%"; }

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
