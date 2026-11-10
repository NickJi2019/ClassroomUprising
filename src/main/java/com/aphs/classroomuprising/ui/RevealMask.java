package com.aphs.classroomuprising.ui;

/**
 * The shape a new screen opens through: a slanted bar (the game's "//" angle) growing from an
 * origin. At progress 1 it is guaranteed to cover the whole 1280x720 canvas from any origin.
 * Pure maths, so coverage can be unit-tested.
 */
final class RevealMask {
    static final double W = 1280, H = 720;
    /** Base tilt of the mask, matching the slant of the rest of the UI. */
    static final double TILT = -14;

    private RevealMask() {}

    /** Distance from the origin to the farthest canvas corner. */
    static double reach(double ox, double oy) {
        return Math.max(Math.max(Math.hypot(ox, oy), Math.hypot(W - ox, oy)),
                Math.max(Math.hypot(ox, H - oy), Math.hypot(W - ox, H - oy)));
    }

    /**
     * Corner points of the mask. The square slanted bar has half-size 1.5 x reach at progress 1;
     * its inscribed circle (radius about 0.97 x half-size) then exceeds the reach, so every
     * canvas point is inside whatever the rotation.
     *
     * @param turn extra rotation in degrees, applied in proportion to progress
     */
    static double[] polygon(double ox, double oy, double progress, double turn) {
        double half = 1.5 * reach(ox, oy) * Math.max(0, progress);
        var pose = new CursorGeometry.Pose(ox, oy, 2 * half, 2 * half, TILT + turn * progress);
        return CursorGeometry.corners(pose, Slant.SLOPE);
    }

    /** Even-odd point-in-polygon test (used by tests and for hit checks). */
    static boolean contains(double[] polygon, double x, double y) {
        boolean inside = false;
        int n = polygon.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon[2 * i], yi = polygon[2 * i + 1], xj = polygon[2 * j], yj = polygon[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside;
        }
        return inside;
    }
}
