package com.aphs.classroomuprising.ui;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Fired (and bubbled to the scene) when an option is activated, carrying where the cursor was,
 * so the next screen can open out of the thing the player chose.
 */
final class RevealOriginEvent extends Event {
    static final EventType<RevealOriginEvent> ACTIVATED = new EventType<>(Event.ANY, "REVEAL_ORIGIN");

    private final double sceneX;
    private final double sceneY;

    RevealOriginEvent(double sceneX, double sceneY) {
        super(ACTIVATED);
        this.sceneX = sceneX;
        this.sceneY = sceneY;
    }

    double sceneX() { return sceneX; }

    double sceneY() { return sceneY; }
}
