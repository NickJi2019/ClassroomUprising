package com.aphs.classroomuprising;

/**
 * Explicitly supplied numeric policy inputs. Null means TBD, not zero.
 * Fixture values in tests/demo are not an approved balancing configuration.
 */
public record RuleValues(Double lateNightSanReduction, Double lateNightXpMultiplier,
                         Double earlySleepSanBonus, Double makeupSanReduction,
                         Double caughtCheatingSanReduction, TutorialAbsences tutorialAbsences,
                         Double normalSleepSanRecovery) {
    public enum TutorialAbsences { UNDECIDED, COUNT, IGNORE }
    public RuleValues {
        positiveOrNull(lateNightSanReduction); positiveOrNull(earlySleepSanBonus);
        positiveOrNull(makeupSanReduction); positiveOrNull(caughtCheatingSanReduction);
        if (normalSleepSanRecovery != null &&
                (!Double.isFinite(normalSleepSanRecovery) || normalSleepSanRecovery < 0))
            throw new IllegalArgumentException("Normal-sleep SAN recovery must be non-negative.");
        if (lateNightXpMultiplier != null &&
                (!Double.isFinite(lateNightXpMultiplier) || lateNightXpMultiplier <= 0 || lateNightXpMultiplier >= 1))
            throw new IllegalArgumentException("A reduced XP multiplier must be between zero and one.");
        java.util.Objects.requireNonNull(tutorialAbsences);
    }
    /** Existing callers retain pending normal-sleep values, not a silently chosen default. */
    public RuleValues(Double lateNightSanReduction, Double lateNightXpMultiplier,
                      Double earlySleepSanBonus, Double makeupSanReduction,
                      Double caughtCheatingSanReduction, TutorialAbsences tutorialAbsences) {
        this(lateNightSanReduction, lateNightXpMultiplier, earlySleepSanBonus,
                makeupSanReduction, caughtCheatingSanReduction, tutorialAbsences, null);
    }
    private static void positiveOrNull(Double v) {
        if (v != null && (!Double.isFinite(v) || v <= 0))
            throw new IllegalArgumentException("A configured bonus/reduction must be positive.");
    }
    public static RuleValues pending() {
        return new RuleValues(null, null, null, null, null, TutorialAbsences.UNDECIDED);
    }
    static double require(Double value, String id, String name) {
        if (value == null) throw new PendingRuleException(id, name + " is not configured.");
        return value;
    }
}
