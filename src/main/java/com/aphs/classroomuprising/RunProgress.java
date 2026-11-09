package com.aphs.classroomuprising;

import java.util.Set;

/** Confirmed carryover boundaries, separate from the still-pending complete reset/ending policy. */
public record RunProgress(int retakesUsed) {
    public RunProgress {
        if (retakesUsed < 0 || retakesUsed > 2) throw new IllegalArgumentException("Retakes used must be 0..2.");
    }
    public int remainingRetakes() { return 2 - retakesUsed; }
    public RunProgress consumeRetake() {
        if (remainingRetakes() == 0) throw new IllegalStateException("Both Retake opportunities were used.");
        return new RunProgress(retakesUsed + 1);
    }
    public static Set<String> rewardsForNewPlaythrough(Set<String> owned, Set<String> selected) {
        if (!owned.containsAll(selected)) throw new IllegalArgumentException("Selected reward is not owned.");
        return Set.copyOf(selected);
    }
}
