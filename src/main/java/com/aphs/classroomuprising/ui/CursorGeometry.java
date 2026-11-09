package com.aphs.classroomuprising.ui;

import java.util.List;

/**
 * Pure maths for the selection cursor: its slanted shape, and the stepped paths it follows
 * when it moves or first appears. No JavaFX here, so it can be unit-tested.
 */
final class CursorGeometry {
    /** Where the cursor sits: centre, size and tilt in degrees. */
    record Pose(double cx, double cy, double w, double h, double angle) {
        Pose plus(double dx, double dy, double scale, double dAngle) {
            return new Pose(cx + dx, cy + dy, w * scale, h * scale, angle + dAngle);
        }
    }

    /** One key pose of a path, reached at {@code millis} after the move starts. */
    record Step(double millis, Pose pose) {}

    /** Largest overshoot past the target, in canvas pixels. */
    static final double MAX_OVERSHOOT = 14;

    private CursorGeometry() {}

    /**
     * The four corners (x0, y0, ... x3, y3) of a parallelogram centred on the pose, whose
     * top edge is shifted right by {@code slope * height}, rotated by the pose angle.
     */
    static double[] corners(Pose p, double slope) {
        double hw = p.w() / 2, hh = p.h() / 2, o = p.h() * slope / 2;
        double[] local = {-hw + o, -hh, hw + o, -hh, hw - o, hh, -hw - o, hh};
        double rad = Math.toRadians(p.angle()), cos = Math.cos(rad), sin = Math.sin(rad);
        double[] out = new double[8];
        for (int i = 0; i < 8; i += 2) {
            out[i] = p.cx() + local[i] * cos - local[i + 1] * sin;
            out[i + 1] = p.cy() + local[i] * sin + local[i + 1] * cos;
        }
        return out;
    }

    /**
     * Travel from one option to another: overshoot past the target in the direction of travel,
     * pull back a little, then settle exactly on it. A move of a pixel or less just settles.
     */
    static List<Step> travel(Pose from, Pose to) {
        double dx = to.cx() - from.cx(), dy = to.cy() - from.cy();
        double distance = Math.hypot(dx, dy);
        if (distance <= 1) return List.of(new Step(160, to));
        double size = Math.min(MAX_OVERSHOOT, distance * 0.12);
        double ox = dx / distance * size, oy = dy / distance * size;
        double tilt = Math.signum(dy == 0 ? dx : dy) * 2.5;
        return List.of(
                new Step(150, to.plus(ox, oy, 1.04, tilt)),
                new Step(240, to.plus(-ox * 0.35, -oy * 0.35, 0.99, -tilt * 0.4)),
                new Step(320, to));
    }

    /** First appearance: arrive along the slant diagonal, swing past, and settle. */
    static List<Step> spawn(Pose at) {
        return List.of(
                new Step(0, at.plus(40, -40, 0.85, 6)),
                new Step(200, at.plus(-20, 20, 1.03, -3)),
                new Step(300, at.plus(10, -10, 0.99, 1)),
                new Step(400, at));
    }
}
