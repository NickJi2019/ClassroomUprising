package com.aphs.classroomuprising.ui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The reveal mask must fully cover the canvas at the end, from any origin and rotation. */
class RevealMaskTest {
    @ParameterizedTest
    @CsvSource({"640, 360, 0", "0, 0, 0", "1280, 720, 8", "1180, 640, -8", "100, 90, 8", "640, 0, 20"})
    void fullProgressCoversEveryCanvasPoint(double ox, double oy, double turn) {
        double[] mask = RevealMask.polygon(ox, oy, 1, turn);
        for (double x = 0; x <= 1280; x += 64)
            for (double y = 0; y <= 720; y += 40)
                assertTrue(RevealMask.contains(mask, x, y), "uncovered at " + x + "," + y + " from " + ox + "," + oy);
    }

    @Test void zeroProgressCoversNothing() {
        double[] mask = RevealMask.polygon(640, 360, 0, 8);
        assertFalse(RevealMask.contains(mask, 640, 360 + 1));
        assertFalse(RevealMask.contains(mask, 0, 0));
    }

    @Test void halfwayCoversTheOriginButNotTheFarCorner() {
        double[] mask = RevealMask.polygon(100, 100, 0.3, 0);
        assertTrue(RevealMask.contains(mask, 100, 100));
        assertFalse(RevealMask.contains(mask, 1280, 720));
    }

    @Test void stepsJumpBetweenEvenValues() {
        var steps = MotionTokens.steps(4);
        assertEquals(0.0, steps.interpolate(0.0, 1.0, 0.1), 1e-9);
        assertEquals(0.25, steps.interpolate(0.0, 1.0, 0.3), 1e-9);
        assertEquals(0.75, steps.interpolate(0.0, 1.0, 0.99), 1e-9);
        assertEquals(1.0, steps.interpolate(0.0, 1.0, 1.0), 1e-9);
    }
}
