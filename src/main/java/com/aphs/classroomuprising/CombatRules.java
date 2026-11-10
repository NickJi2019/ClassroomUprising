package com.aphs.classroomuprising;

import java.time.Duration;
import java.util.Objects;

/** Unresolved combat decisions are explicit inputs, never production defaults. */
public record CombatRules(Double weightStep, TimeoutDasher timeoutDasher, AnswerTiming answerTiming,
                          Boolean rewardTimeCounts, Duration riskyRewardDuration,
                          HpCost hpCost, ScoreConverter scoreConverter) {
    public static final Duration QUESTION_LIMIT = Duration.ofSeconds(30);
    public static final Duration SAFE_REWARD_DURATION = Duration.ofSeconds(5);
    public static final Duration MAX_RISKY_REWARD_DURATION = Duration.ofSeconds(20);
    public static final int COMBO_BONUS = 5;
    public enum TimeoutDasher { UNDECIDED, KEEP_DOUBLE, CANCEL_DOUBLE }
    public enum AnswerTiming { UNDECIDED, PLAYER_SUBMISSION, EVALUATION_FINISH }
    /** Implementations must be deterministic, side-effect-free and charge more for faster answers. */
    @FunctionalInterface public interface HpCost { double forAnswer(Duration elapsed); }
    /** Converts weighted credits plus the separate Combo award into an approved 0..100 course mark. */
    @FunctionalInterface public interface ScoreConverter { double toPercentage(CombatSession.Summary summary); }
    public CombatRules {
        Objects.requireNonNull(timeoutDasher); Objects.requireNonNull(answerTiming);
        if (weightStep != null && (!Double.isFinite(weightStep) || weightStep <= 0))
            throw new IllegalArgumentException("Weight step must be finite and positive.");
        if (riskyRewardDuration != null && (riskyRewardDuration.isNegative() || riskyRewardDuration.isZero()
                || riskyRewardDuration.compareTo(MAX_RISKY_REWARD_DURATION) > 0))
            throw new IllegalArgumentException("Risky reward duration must be positive and at most 20 seconds.");
    }
    public static CombatRules pending() {
        return new CombatRules(null, TimeoutDasher.UNDECIDED, AnswerTiming.UNDECIDED, null, null, null, null);
    }
    double initialWeight(int index) {
        double step = RuleValues.require(weightStep, "T07", "Combat weight step: text says .01, example says .1");
        double value = 1 + step * index; StatChange.requireFinite(value); return value;
    }
    void requireTiming() {
        if (answerTiming == AnswerTiming.UNDECIDED)
            throw new PendingRuleException("T07", "Does question timing stop on player submission or evaluated completion?");
    }
    double weight(double initial, boolean timedOut, boolean dasher) {
        double value = timedOut ? 1 : initial;
        if (dasher) {
            if (timedOut && timeoutDasher == TimeoutDasher.UNDECIDED)
                throw new PendingRuleException("T07", "Does Dasher survive a timeout?");
            if (!timedOut || timeoutDasher == TimeoutDasher.KEEP_DOUBLE) value *= 2;
        }
        StatChange.requireFinite(value); return value;
    }
}
