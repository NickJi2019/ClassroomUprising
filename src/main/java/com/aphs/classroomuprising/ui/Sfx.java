package com.aphs.classroomuprising.ui;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;

import java.util.Locale;

/**
 * The interface sounds. Each is listed in the asset manifest as {@code sfx.<key>}; a sound whose
 * file is not delivered is simply silent. Controls ask for a sound with {@link #play(Node)}: the
 * request travels up to the scene, where the window's {@link AudioDirector} plays it, so a
 * control never needs to know about audio settings.
 */
enum Sfx {
    /** The cursor moved to another option. */
    CURSOR,
    /** An option was chosen. */
    CONFIRM,
    /** The Esc menu opened. */
    MENU_OPEN,
    /** The Esc menu closed. */
    MENU_CLOSE,
    /** The dialogue moved on to its next line. */
    DIALOGUE,
    /** A lecture slide changed. */
    SLIDE,
    /** A tutorial tip appeared. */
    TIP,
    /** A screen change began. */
    TRANSITION,
    /** The clock started turning to the next time of day. */
    CLOCK,
    /** A combat call-out banner snapped in. */
    CUTIN,
    /** An answer cost HP. */
    HIT;

    /** The manifest name, e.g. {@code menu-open} for {@link #MENU_OPEN}. */
    String key() { return name().toLowerCase(Locale.ROOT).replace('_', '-'); }

    /** Asks the window to play this sound; does nothing for a node that is not on screen. */
    void play(Node from) {
        if (from.getScene() != null) from.fireEvent(new Request(this));
    }

    /** The request a control sends up to the scene. */
    static final class Request extends Event {
        static final EventType<Request> PLAY = new EventType<>(Event.ANY, "SFX_PLAY");
        private final Sfx sound;

        Request(Sfx sound) {
            super(PLAY);
            this.sound = sound;
        }

        Sfx sound() { return sound; }
    }
}
