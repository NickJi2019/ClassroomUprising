package com.aphs.classroomuprising;

import java.util.*;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/** Transactional application operations for the confirmed rules; no UI or filesystem work here. */
public final class GameController {
    public record Result(String message, List<String> warnings) {
        public Result { warnings = List.copyOf(warnings); }
    }
    public record AssessmentView(String id, CourseType type, List<ContentCatalog.Question> questions,
                                 boolean makeup, boolean peekingAllowed, boolean increasedDetectionRisk) {
        public AssessmentView { questions = List.copyOf(questions); }
    }
    private GameState state;
    private final RuleValues rules;
    private final ContentCatalog content;
    private final RandomGenerator random;
    private final CombatRules combatRules;
    private final java.util.function.LongSupplier nanoClock;
    public GameController(GameState initial, RuleValues rules, ContentCatalog content, RandomGenerator random) {
        this(initial, rules, content, random, CombatRules.pending(), System::nanoTime);
    }
    public GameController(GameState initial, RuleValues rules, ContentCatalog content, RandomGenerator random,
                          CombatRules combatRules) {
        this(initial, rules, content, random, combatRules, System::nanoTime);
    }
    /** Clock is backend-owned. Tests inject a monotonic fake; frontend submits no elapsed times. */
    public GameController(GameState initial, RuleValues rules, ContentCatalog content, RandomGenerator random,
                          CombatRules combatRules, java.util.function.LongSupplier nanoClock) {
        this.combatRules = Objects.requireNonNull(combatRules);
        this.nanoClock = Objects.requireNonNull(nanoClock);
        this.state = Objects.requireNonNull(initial).copy();
        this.rules = Objects.requireNonNull(rules); this.content = Objects.requireNonNull(content);
        this.random = Objects.requireNonNull(random);
    }
    public GameState snapshot() { return state.copy(); }
    private Result change(String message, Function<GameState, List<String>> operation) {
        GameState candidate = state.copy();
        List<String> warnings = operation.apply(candidate);
        candidate.effectiveSan(); candidate.xpMultiplier(); // reject arithmetic overflow before commit
        state = candidate;
        return new Result(message, warnings);
    }
    private static void active(GameState s) {
        if (!s.openingCompleted) throw new IllegalStateException("Complete the opening tutorial first.");
        if (s.status != GameState.Status.ACTIVE) throw new IllegalStateException("Resolve the current run checkpoint.");
    }
    private static void phase(GameState s, DayPeriod expected) {
        active(s);
        if (s.period != expected) throw new IllegalStateException("Expected " + expected + ", found " + s.period);
    }
    private static List<String> apply(GameState s, StatChange effect, double scale) {
        boolean waived = s.isTutorial() && (effect.hp() < 0 || effect.san() < 0 || effect.codingVibe() < 0
                || effect.unitKnowledge().stream().anyMatch(x -> x < 0));
        s.student = s.student.apply(effect, scale, s.xpMultiplier(), s.isTutorial());
        return waived ? List.of("Tutorial: negative effects were warnings only.") : List.of();
    }
    public Result completeOpening() {
        return change("Opening ending acknowledged; Day 1 is ready.", s -> {
            if (s.openingCompleted || s.day != 1) throw new IllegalStateException("Opening already completed.");
            if (content.hasOpeningBattle() && (s.openingBattle == null || !s.openingBattle.finished()))
                throw new IllegalStateException("Finish the opening exam and show its ending before Day 1.");
            s.openingBattle = null; // Past-semester HP/SAN/XP and 67/89 never enter this term's state/grades.
            s.openingCompleted = true; return List.of();
        });
    }
    /** Story data only. Intro and ending presentation/animation belong to the frontend. */
    public List<String> openingIntro() { return content.opening().intro(); }
    public Result startOpeningBattle() {
        return change("Opening exam started.", s -> {
            if (s.openingCompleted || s.day != 1 || s.openingBattle != null)
                throw new IllegalStateException("Opening battle already started or completed.");
            s.openingBattle = new OpeningBattle(content.opening(), nanoClock.getAsLong()); return List.of();
        });
    }
    private static OpeningBattle opening(GameState s) {
        if (s.openingCompleted || s.openingBattle == null) throw new IllegalStateException("Start the opening exam first.");
        return s.openingBattle;
    }
    public OpeningBattle.View openingBattle() { return opening(state).view(nanoClock.getAsLong()); }
    public Result startOpeningQuestion(String questionId) {
        return change("Opening question started.", s -> {
            opening(s).select(questionId, nanoClock.getAsLong()); return List.of();
        });
    }
    public Result answerOpeningQuestion(String questionId, String optionId) {
        return change("Opening response resolved; read the battle feedback/ending.", s -> {
            opening(s).answer(questionId, optionId, nanoClock.getAsLong()); return List.of();
        });
    }
    public Result useOpeningDuck() {
        return change("Rubber duck action resolved; read the battle view.", s -> {
            opening(s).useDuck(nanoClock.getAsLong()); return List.of();
        });
    }
    /** Invoke on each frontend timer pulse; timeouts settle even without an answer click. */
    public Result advanceOpeningBattle() {
        return change("Opening clock advanced.", s -> { opening(s).tick(nanoClock.getAsLong()); return List.of(); });
    }
    public ContentCatalog.Lesson showLesson() {
        phase(state, DayPeriod.ICS_CLASS);
        if (state.currentCourse().course() != CourseType.LESSON || state.makeups.containsKey(state.day))
            throw new IllegalStateException("Today's class is not a normal Lesson.");
        var lesson = content.lesson(state.day);
        change("Lesson content shown.", s -> { s.lessonShown = true; return List.of(); });
        return lesson;
    }
    public Result lessonChoice(String choice) {
        return lessonChoice(state.day, choice);
    }
    /** Only final response/skip IDs settle the lesson; menu opening grants nothing. */
    public Result lessonChoice(int lessonDay, String choice) {
        phase(state, DayPeriod.ICS_CLASS);
        if (lessonDay != state.day || !state.lessonShown || state.currentCourse().course() != CourseType.LESSON
                || state.makeups.containsKey(state.day))
            throw new IllegalStateException("Show the current scheduled Lesson before its one choice.");
        if (choice == null || choice.isBlank()) throw new IllegalArgumentException("Classroom option ID required.");
        var lesson = content.lesson(state.day);
        var effect = lesson.choices().get(choice);
        if (effect == null) throw new IllegalArgumentException("Unknown classroom choice.");
        String feedback = lesson.decision() == null ? "Lesson completed." : lesson.decision().option(choice).feedback();
        return change(feedback, s -> {
            phase(s, DayPeriod.ICS_CLASS);
            var warnings = apply(s, effect, 1); s.period = DayPeriod.OTHER_CLASSES; return warnings;
        });
    }
    public Result completeReview() {
        return change("Review completed.", s -> {
            phase(s, DayPeriod.ICS_CLASS);
            if (s.currentCourse().course() != CourseType.REVIEW) throw new IllegalStateException("Not a Review day.");
            var warnings = apply(s, content.review(s.day), 1); s.period = DayPeriod.OTHER_CLASSES; return warnings;
        });
    }
    public Result otherClasses(boolean reviseIcs) {
        return change("OTHER_CLASSES completed.", s -> {
            phase(s, DayPeriod.OTHER_CLASSES);
            var warnings = apply(s, reviseIcs ? content.otherRevision() : StatChange.none(), 1);
            s.period = DayPeriod.AFTER_SCHOOL; return warnings;
        });
    }
    public Result hubActivity(String activityId) {
        return change("Activity completed.", s -> {
            active(s);
            if (!s.period.isHubActivityPeriod()) throw new IllegalStateException("Not a Hub/bonus period.");
            var effect = content.hub(activityId);
            var warnings = new ArrayList<String>();
            if (s.period == DayPeriod.LATE_NIGHT) {
                if (s.isTutorial()) warnings.add("Tutorial: no next-day late-night penalty was scheduled.");
                else {
                    double san = RuleValues.require(rules.lateNightSanReduction(), "T03", "Late-night SAN reduction");
                    double xp = RuleValues.require(rules.lateNightXpMultiplier(), "T03", "Late-night XP multiplier");
                    s.effects.add(new TimedEffect("late-night:" + s.day, s.day + 1, s.day + 1, -san, xp));
                }
            }
            warnings.addAll(apply(s, effect, s.period.activityUnits()));
            s.period = s.period.next(); return warnings;
        });
    }
    public Result sleepImmediatelyAfterSchool() {
        return change("Early sleep recorded.", s -> {
            phase(s, DayPeriod.AFTER_SCHOOL);
            s.nextMorningSanBonus = RuleValues.require(rules.earlySleepSanBonus(), "T04", "Early-sleep SAN bonus");
            s.nextMorningBonus = true; s.period = DayPeriod.DAY_END; return List.of();
        });
    }
    /** Close an evening/late-night slot without using it as an activity or earning early-sleep time. */
    public Result sleepNormally() {
        return change("Normal sleep completed.", s -> {
            active(s);
            if (s.period != DayPeriod.EVENING && s.period != DayPeriod.LATE_NIGHT)
                throw new IllegalStateException("Normal sleep requires EVENING or LATE_NIGHT.");
            double recovery = RuleValues.require(rules.normalSleepSanRecovery(), "T04", "Normal-sleep SAN recovery");
            var warnings = apply(s, new StatChange(0, recovery, 0, Collections.nCopies(6, 0.0)), 1);
            s.period = DayPeriod.DAY_END;
            return warnings;
        });
    }
    public Result beginNextDay() {
        return change("Next day started.", s -> {
            phase(s, DayPeriod.DAY_END);
            if (s.day == 40) throw new IllegalStateException("There is no playable Day 41.");
            s.day++;
            s.effects.removeIf(e -> e.lastDay() < s.day);
            if (s.nextMorningBonus) {
                s.student = s.student.apply(new StatChange(0, s.nextMorningSanBonus, 0, Collections.nCopies(6, 0.0)), 1, 1, false);
                s.period = DayPeriod.MORNING_BONUS;
            } else s.period = DayPeriod.ICS_CLASS;
            s.nextMorningBonus = false; s.nextMorningSanBonus = 0; s.lessonShown = false;
            var due = s.grades.releaseDue(s.day);
            for (String id : due) { s.estimates.remove(id); s.effects.removeIf(e -> e.id().equals("estimate:" + id)); }
            return due.isEmpty() ? List.of() : List.of("Official marks released: " + due + "; T03 grade feedback still requires a supplied effect.");
        });
    }
    public Result skipClass(boolean excused) {
        return change("Ordinary class absence recorded.", s -> {
            phase(s, DayPeriod.ICS_CLASS);
            if (s.currentCourse().course().isMajor()) throw new IllegalStateException("Midterm and Final cannot be skipped.");
            if (s.active != null) throw new PendingRuleException("T05", "Withdrawal from an assessment already in progress.");
            if (s.makeups.containsKey(s.day)) throw new PendingRuleException("T05", "Absence from a makeup.");
            s.grades.recordAbsence(excused, s.isTutorial(), rules.tutorialAbsences());
            if (s.currentCourse().course().isAssessment()) s.missedAssessments.add(s.day);
            s.period = DayPeriod.OTHER_CLASSES;
            return s.isTutorial() ? List.of("Tutorial: absence penalties are warnings only.") : List.of();
        });
    }
    public Result scheduleMakeup(int missedDay, int lessonDay) {
        return change("Makeup assigned to the selected Lesson.", s -> {
            active(s);
            if (!s.missedAssessments.contains(missedDay)) throw new IllegalArgumentException("No unresolved missed assessment.");
            if (s.makeups.containsValue(missedDay)) throw new IllegalStateException("Makeup already assigned.");
            var candidates = SchoolCalendar.makeupCandidates(missedDay);
            if (candidates.isEmpty()) throw new PendingRuleException("T05", "No subsequent Lesson exists.");
            if (!candidates.contains(lessonDay) || lessonDay <= s.day)
                throw new IllegalArgumentException("Select one of the first two future Lessons.");
            if (s.makeups.containsKey(lessonDay)) throw new PendingRuleException("T05", "Makeup date collision.");
            s.makeups.put(lessonDay, missedDay); return List.of();
        });
    }
    public AssessmentView startAssessment() { return startAssessment(false); }
    public AssessmentView startCombatAssessment() { return startAssessment(true); }
    private AssessmentView startAssessment(boolean useCombat) {
        phase(state, DayPeriod.ICS_CLASS);
        if (state.active != null) throw new IllegalStateException("Assessment already active.");
        var original = state.currentCourse();
        if (!original.course().isAssessment()) throw new IllegalStateException("Not an assessment slot.");
        if (state.grades.records.containsKey(original.assessmentId())) throw new IllegalStateException("Already submitted.");
        boolean makeup = state.makeups.containsKey(state.day);
        double penalty = makeup && !state.isTutorial()
                ? RuleValues.require(rules.makeupSanReduction(), "T03", "Makeup SAN reduction") : 0;
        // Validate policy/content before starting the session or applying a penalty.
        var questions = content.questions(original, random);
        var combat = useCombat ? new CombatSession(questions, combatRules) : null;
        change("Assessment started.", s -> {
            s.combat = combat;
            s.active = new GameState.ActiveAssessment(original, questions, makeup);
            return apply(s, new StatChange(0, -penalty, 0, Collections.nCopies(6, 0.0)), 1);
        });
        return new AssessmentView(original.assessmentId(), original.course(), questions, makeup, !makeup, makeup);
    }
    /** Legacy trusted-evaluator entry; combat runs must settle through finishCombatAssessment(). */
    public Result recordEvaluatedSubmission(double mark, String evidence) {
        return change("Evaluated submission recorded.", s -> {
            if (s.combat != null) throw new IllegalStateException("Use finishCombatAssessment for a combat run.");
            return settleAssessment(s, mark, evidence);
        });
    }
    private static List<String> settleAssessment(GameState s, double mark, String evidence) {
        phase(s, DayPeriod.ICS_CLASS);
        if (s.active == null) throw new IllegalStateException("No active assessment.");
        var a = s.active; var type = a.original().course(); String id = a.original().assessmentId();
        if (!Double.isFinite(mark) || mark < 0 || mark > 100) throw new IllegalArgumentException("Invalid evaluated percentage.");
        double actual = s.seriousCheating.contains(id) ? 0 : mark;
        var r = new AssessmentRecord(id, type, s.day, s.day + (type.isMajor() ? 0 : 3),
                a.questions().stream().map(ContentCatalog.Question::id).toList(), a.makeup(), actual, type.isMajor(), evidence);
        s.grades.submit(r); s.active = null; s.combat = null;
        if (type.isMajor()) {
            s.estimates.remove(id); s.effects.removeIf(e -> e.id().equals("estimate:" + id));
            s.status = type == CourseType.MIDTERM ? GameState.Status.MIDTERM_DECISION : GameState.Status.FINAL_DECISION;
        } else s.period = DayPeriod.OTHER_CLASSES;
        s.missedAssessments.remove(a.original().day());
        return type.isMajor() ? List.of("Official result is immediate; T03/T10 feedback and checkpoint policy remain explicit.") : List.of();
    }
    private static CombatSession combat(GameState s) {
        phase(s, DayPeriod.ICS_CLASS);
        if (s.active == null || s.combat == null) throw new IllegalStateException("No active combat assessment.");
        return s.combat;
    }
    public CombatSession.View combatSnapshot() { return combat(state).view(nanoClock.getAsLong(), combatRules); }
    public Result startCombatQuestion(String questionId) {
        return change("Combat question started.", s -> {
            combat(s).startQuestion(questionId, nanoClock.getAsLong(), combatRules); return List.of();
        });
    }
    /** Lock this submission before dispatching any asynchronous evaluator. Tickets reject stale results. */
    public CombatSession.AnswerTicket submitCombatAnswer() {
        change("Combat answer submitted for evaluation.", s -> {
            combat(s).submit(nanoClock.getAsLong(), combatRules); return List.of();
        });
        return state.combat.pendingAnswer();
    }
    /** Fraction/evidence come from a trusted configured evaluator, not a UI-selected mark. */
    public Result recordCombatEvaluation(CombatSession.AnswerTicket ticket, double fraction, String evidence) {
        return change("Combat answer evaluated.", s -> {
            var answer = combat(s).evaluate(ticket, fraction, evidence, nanoClock.getAsLong(), combatRules, s);
            return apply(s, new StatChange(-answer.requestedHpCost(), 0, 0, Collections.nCopies(6, 0.0)), 1);
        });
    }
    /** Timing reservation only. Inventory, item effects and detection remain T09, not inferred here. */
    public CombatSession.RewardTicket beginCombatRewardDelay(CombatSession.RewardKind kind) {
        change("Combat reward delay started.", s -> {
            combat(s).beginReward(kind, nanoClock.getAsLong(), combatRules); return List.of();
        });
        return state.combat.pendingReward();
    }
    public Result finishCombatRewardDelay(CombatSession.RewardTicket ticket) {
        return change("Combat reward delay finished.", s -> {
            combat(s).finishReward(ticket, nanoClock.getAsLong(), combatRules); return List.of();
        });
    }
    public CombatSession.Summary combatSummary() { return combat(state).summary(); }
    public Result finishCombatAssessment() {
        return change("Combat assessment settled.", s -> {
            var summary = combat(s).summary();
            if (combatRules.scoreConverter() == null)
                throw new PendingRuleException("T06", "Combat credits, Combo points and percentage conversion are not configured.");
            double mark = combatRules.scoreConverter().toPercentage(summary);
            return settleAssessment(s, mark, "Combat evaluation: " + summary);
        });
    }
    /** An estimator supplies its result and effects; this method never invents an accuracy model. */
    public Result recordSelfEstimate(double mark, double sanCost, double provisionalSanDelta) {
        return change("Provisional estimate recorded.", s -> {
            phase(s, DayPeriod.ICS_CLASS);
            if (s.active == null) throw new IllegalStateException("No active assessment.");
            if (s.active.original().course().isMajor()) throw new PendingRuleException("T08", "Estimating an immediate-result exam.");
            if (!Double.isFinite(mark) || mark < 0 || mark > 100 || !Double.isFinite(sanCost) || sanCost < 0)
                throw new IllegalArgumentException("Invalid estimator output/cost.");
            StatChange.requireFinite(provisionalSanDelta);
            String id = s.active.original().assessmentId();
            if (s.estimates.containsKey(id)) throw new PendingRuleException("T08", "Repeated self-estimates.");
            var warnings = apply(s, new StatChange(0, -sanCost, 0, Collections.nCopies(6, 0.0)), 1);
            double delta = s.isTutorial() && provisionalSanDelta < 0 ? 0 : provisionalSanDelta;
            s.estimates.put(id, mark);
            s.effects.add(new TimedEffect("estimate:" + id, s.day, Integer.MAX_VALUE, delta, 1));
            return warnings;
        });
    }
    public Result caughtCheating(boolean serious) {
        return change("Detected cheating resolved.", s -> {
            phase(s, DayPeriod.ICS_CLASS);
            if (s.active == null) throw new IllegalStateException("No active assessment.");
            if (s.isTutorial()) return List.of("Tutorial: warning only; no SAN loss or zero mark.");
            double penalty = RuleValues.require(rules.caughtCheatingSanReduction(), "T09", "Caught-cheating SAN reduction");
            var warnings = apply(s, new StatChange(0, -penalty, 0, Collections.nCopies(6, 0.0)), 1);
            if (serious) s.seriousCheating.add(s.active.original().assessmentId());
            return warnings;
        });
    }
    public Result applyOfficialFeedback(String id, StatChange policyEffect) {
        return change("Configured official-result feedback applied.", s -> {
            if (s.grades.record(id).officialScore().isEmpty()) throw new IllegalStateException("Result is not official.");
            if (s.feedbackApplied.contains(id)) throw new IllegalStateException("Feedback already applied.");
            var warnings = apply(s, policyEffect, 1); s.feedbackApplied.add(id); return warnings;
        });
    }
    /** No guessed threshold or Retake reset list is silently installed. */
    public Result resolveMidterm(String choice) {
        if (state.status != GameState.Status.MIDTERM_DECISION) throw new IllegalStateException("Not at Midterm checkpoint.");
        if ("drop".equals(choice)) return change("Run dropped.", s -> { s.status = GameState.Status.DROPPED; return List.of(); });
        if ("retake".equals(choice) && state.run.remainingRetakes() == 0) throw new IllegalStateException("No Retake opportunities remain.");
        throw new PendingRuleException("T10", "Continuation thresholds, Retake reset list and tutorial-debuff priority need approval.");
    }
}
