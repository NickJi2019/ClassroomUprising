package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CourseType;

/**
 * The trusted evaluator that turns one captured combat answer into an earned fraction and
 * evidence. It runs off the UI thread.
 *
 * <p>No implementation ships with the game: Lab evidence-to-credit is T06 and question answer
 * keys are T07. Until the backend supplies one, the combat screen refuses graded submission
 * and shows that rule ID instead of a made-up mark.</p>
 */
@FunctionalInterface
interface CombatEvaluator {
    /** What the player submitted, captured before the ticket was dispatched. */
    record Work(String assessmentId, CourseType type, String questionId, String prompt, String answer) {}

    /** The evaluator's verdict: a fraction in [0, 1] and non-blank evidence (the core validates both). */
    record Evaluation(double earnedFraction, String evidence) {}

    /** Blocking evaluation. Throwing means an infrastructure failure, not a wrong answer. */
    Evaluation evaluate(Work work) throws Exception;

    /** The rule that leaves this kind of assessment without an evaluator. */
    static String pendingRule(CourseType type) { return type == CourseType.LAB ? "T06" : "T07"; }
}
