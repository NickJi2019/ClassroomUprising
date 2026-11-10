package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatSession;

import java.time.Duration;
import java.util.Locale;

/** Display strings for the combat screen. Formatting only: every number comes from the backend. */
final class CombatText {
    private CombatText() {}

    /**
     * Seconds with one decimal, rounded up, so the display agrees with the soft limit: exactly
     * 30 s shows "30.0" (still on time) and anything later shows at least "30.1".
     */
    static String seconds(Duration elapsed) {
        long tenths = Math.max(0, (elapsed.toNanos() + 99_999_999) / 100_000_000);
        return String.format(Locale.ROOT, "%d.%d", tenths / 10, tenths % 10);
    }

    /** "×1.10". Two decimals so a .01 step stays visible. */
    static String weight(double value) { return String.format(Locale.ROOT, "×%.2f", value); }

    /** Weighted credits are not a percentage, so they are shown as a plain number. */
    static String credits(double value) { return String.format(Locale.ROOT, "%.2f", value); }

    static String percent(double fraction) { return String.format(Locale.ROOT, "%.0f%%", fraction * 100); }

    static String status(CombatSession.QuestionStatus status) {
        return switch (status) {
            case AVAILABLE -> "READY";
            case ANSWERING -> "ANSWERING";
            case SUBMITTED -> "EVALUATING";
            case ANSWERED -> "CLEARED";
        };
    }

    static String combo(CombatSession.ComboStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "ALL ON TIME SO FAR";
            case LOST -> "LOST";
            case EARNED -> "EARNED";
        };
    }

    /** "Q2" for the second question in difficulty order. */
    static String label(int position) { return "Q" + (position + 1); }

    /** The first line of a prompt, cut to {@code max} characters. */
    static String excerpt(String text, int max) {
        String line = text.strip().lines().findFirst().orElse("");
        return line.length() <= max ? line : line.substring(0, Math.max(0, max - 1)).stripTrailing() + "…";
    }
}
