package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.GameState;

import java.util.List;

/**
 * What one player action did, in a form the screens can present.
 * Only {@link GameSession} creates these; screens never change game state themselves.
 */
public sealed interface Outcome {
    /** The core accepted the action. The two snapshots are for feedback display only. */
    record Done(String message, List<String> warnings, GameState before, GameState after) implements Outcome {
        public Done { warnings = List.copyOf(warnings); }
    }

    /** The core reached a rule the team has not decided yet (PendingRuleException). Nothing changed. */
    record Pending(String ruleId, String detail) implements Outcome {}

    /** The core refused the action in the current state. Nothing changed. */
    record Rejected(String reason) implements Outcome {}

    default boolean ok() { return this instanceof Done; }
}
