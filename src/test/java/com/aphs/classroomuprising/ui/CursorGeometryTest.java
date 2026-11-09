package com.aphs.classroomuprising.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The cursor's shape and its stepped travel paths. */
class CursorGeometryTest {
    private static final CursorGeometry.Pose A = new CursorGeometry.Pose(100, 100, 200, 40, 0);
    private static final CursorGeometry.Pose B = new CursorGeometry.Pose(120, 180, 240, 40, -2);

    @Test void cornersFormTheSlantedBarAroundTheCentre() {
        double[] c = CursorGeometry.corners(A, 0.25);
        // top edge shifted right by slope*h/2, bottom edge shifted left by the same amount
        assertArrayEquals(new double[] {5, 80, 205, 80, 195, 120, -5, 120}, c, 1e-9);
        double cx = (c[0] + c[2] + c[4] + c[6]) / 4, cy = (c[1] + c[3] + c[5] + c[7]) / 4;
        assertEquals(100, cx, 1e-9);
        assertEquals(100, cy, 1e-9);
    }

    @Test void rotationKeepsTheCentreAndSize() {
        var tilted = new CursorGeometry.Pose(100, 100, 200, 40, 30);
        double[] c = CursorGeometry.corners(tilted, 0.25);
        assertEquals(100, (c[0] + c[2] + c[4] + c[6]) / 4, 1e-9);
        assertEquals(200, Math.hypot(c[2] - c[0], c[3] - c[1]), 1e-9);
    }

    @Test void travelOvershootsInTheDirectionOfTravelThenSettlesExactly() {
        List<CursorGeometry.Step> path = CursorGeometry.travel(A, B);
        assertEquals(B, path.get(path.size() - 1).pose());
        CursorGeometry.Pose overshoot = path.get(0).pose();
        assertTrue(overshoot.cy() > B.cy(), "moving down, the cursor first passes below the target");
        double past = Math.hypot(overshoot.cx() - B.cx(), overshoot.cy() - B.cy());
        assertTrue(past > 0 && past <= CursorGeometry.MAX_OVERSHOOT + 1e-9);
        for (int i = 1; i < path.size(); i++) assertTrue(path.get(i).millis() > path.get(i - 1).millis());
    }

    @Test void longMovesCapTheOvershoot() {
        var far = new CursorGeometry.Pose(100, 900, 200, 40, 0);
        var first = CursorGeometry.travel(A, far).get(0).pose();
        assertEquals(CursorGeometry.MAX_OVERSHOOT, first.cy() - far.cy(), 1e-9);
    }

    @Test void aTinyMoveJustSettles() {
        var nudge = A.plus(0.5, 0, 1, 0);
        assertEquals(List.of(new CursorGeometry.Step(160, nudge)), CursorGeometry.travel(A, nudge));
    }

    @Test void spawnEndsOnTheTarget() {
        List<CursorGeometry.Step> path = CursorGeometry.spawn(B);
        assertEquals(0, path.get(0).millis());
        assertEquals(B, path.get(path.size() - 1).pose());
        assertNotEquals(B, path.get(0).pose());
    }
}
