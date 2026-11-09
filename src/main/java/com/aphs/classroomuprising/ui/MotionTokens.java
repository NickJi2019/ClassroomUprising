package com.aphs.classroomuprising.ui;

import javafx.animation.Interpolator;
import javafx.util.Duration;

/**
 * The one place for motion timing, so every screen moves with the same rhythm.
 * Tone: crisp and restrained. Things arrive fast and settle softly; nothing floats.
 */
final class MotionTokens {
    private MotionTokens() {}

    /** Press feedback, flashes. */
    static final Duration SNAP = Duration.millis(90);
    /** Small moves: make-room shifts, hover, toasts. */
    static final Duration QUICK = Duration.millis(200);
    /** Entrances and the cursor's travel. */
    static final Duration BASE = Duration.millis(320);
    /** Larger reveals. */
    static final Duration SLOW = Duration.millis(600);
    /** Delay between items of a list entering one after another. */
    static final Duration STAGGER = Duration.millis(45);

    /** Fast out, soft settle: the default for anything arriving. */
    static final Interpolator OUT = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);
    /** Symmetric, for back-and-forth or sweeping motion. */
    static final Interpolator IN_OUT = Interpolator.SPLINE(0.6, 0, 0.4, 1);
    /** Accelerating, for things leaving. */
    static final Interpolator IN = Interpolator.SPLINE(0.5, 0, 0.9, 0.5);

    /** Updates per second for deliberately stepped ambient motion (shimmer, grain, motes). */
    static final int STEP_RATE = 10;
}
