package com.aphs.classroomuprising;

import java.time.Duration;
import java.util.*;

/** Per-assessment working state. Only GameController owns mutations; public records are detached views. */
public final class CombatSession {
    public enum QuestionStatus { AVAILABLE, ANSWERING, SUBMITTED, ANSWERED }
    public enum ComboStatus { IN_PROGRESS, LOST, EARNED }
    public enum RewardKind { SAFE, RISKY }
    public record AnswerTicket(UUID sessionId, UUID submissionId, String questionId) {}
    public record RewardTicket(UUID sessionId, UUID useId, String questionId, RewardKind kind, Duration duration) {}
    public record QuestionView(String id, String prompt, int difficultyRank, double initialWeight,
                               QuestionStatus status) {}
    public record Answer(String questionId, Duration elapsed, boolean timedOut, boolean dasher,
                         double initialWeight, double effectiveWeight, double earnedFraction,
                         double weightedCredit, double requestedHpCost, double appliedHpCost, String evidence) {}
    /** Weighted credits are not percentages; the five-point Combo award stays separate until conversion. */
    public record Summary(List<Answer> answers, double weightedCredits, ComboStatus combo, int comboBonus) {
        public Summary { answers = List.copyOf(answers); }
    }
    public record View(UUID sessionId, List<QuestionView> questions, String currentQuestionId,
                       Duration elapsed, boolean timedOut, boolean dasherReady, ComboStatus combo,
                       boolean complete, List<Answer> answers, AnswerTicket pendingAnswer,
                       RewardTicket pendingReward, Duration rewardRemaining) {
        public View { questions = List.copyOf(questions); answers = List.copyOf(answers); }
    }
    private record Entry(ContentCatalog.Question question, double initialWeight) {}
    private final UUID id;
    private final List<Entry> entries;
    private final List<Answer> answers;
    private String current;
    private long startedAt;
    private Duration excludedRewardTime = Duration.ZERO;
    private AnswerTicket submitted;
    private Duration submittedElapsed;
    private RewardTicket reward;
    private long rewardStartedAt;

    CombatSession(List<ContentCatalog.Question> questions, CombatRules rules) {
        if (questions.isEmpty()) throw new IllegalArgumentException("Combat needs questions.");
        if (questions.stream().anyMatch(q -> q.difficultyRank() == null))
            throw new PendingRuleException("T07", "Combat questions need authored difficulty ranks.");
        if (questions.stream().map(ContentCatalog.Question::difficultyRank).distinct().count() != questions.size())
            throw new IllegalArgumentException("Combat difficulty ranks must be distinct.");
        var sorted = questions.stream().sorted(Comparator.comparing(ContentCatalog.Question::difficultyRank)).toList();
        var weighted = new ArrayList<Entry>();
        for (int i = 0; i < sorted.size(); i++) weighted.add(new Entry(sorted.get(i), rules.initialWeight(i)));
        entries = List.copyOf(weighted); answers = new ArrayList<>(); id = UUID.randomUUID();
    }
    private CombatSession(CombatSession original) {
        id = original.id; entries = original.entries; answers = new ArrayList<>(original.answers);
        current = original.current; startedAt = original.startedAt; excludedRewardTime = original.excludedRewardTime;
        submitted = original.submitted; submittedElapsed = original.submittedElapsed;
        reward = original.reward; rewardStartedAt = original.rewardStartedAt;
    }
    CombatSession copy() { return new CombatSession(this); }
    private Entry entry(String questionId) {
        return entries.stream().filter(e -> e.question.id().equals(questionId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown combat question."));
    }
    private boolean answered(String questionId) { return answers.stream().anyMatch(a -> a.questionId.equals(questionId)); }
    private static Duration since(long now, long start) {
        long nanos = now - start;
        if (nanos < 0) throw new IllegalStateException("Combat requires a monotonic clock.");
        return Duration.ofNanos(nanos);
    }
    private Duration elapsed(long now, CombatRules rules) {
        if (current == null) return Duration.ZERO;
        if (submitted != null && rules.answerTiming() == CombatRules.AnswerTiming.PLAYER_SUBMISSION)
            return submittedElapsed;
        Duration excluded = excludedRewardTime;
        if (reward != null && Boolean.FALSE.equals(rules.rewardTimeCounts())) {
            Duration passed = since(now, rewardStartedAt);
            excluded = excluded.plus(passed.compareTo(reward.duration) < 0 ? passed : reward.duration);
        }
        Duration result = since(now, startedAt).minus(excluded);
        if (result.isNegative()) throw new IllegalStateException("Invalid question timing.");
        return result;
    }
    void startQuestion(String questionId, long now, CombatRules rules) {
        entry(questionId);
        if (answered(questionId)) throw new IllegalStateException("Question already answered.");
        if (current != null) {
            if (current.equals(questionId) && submitted == null) return; // repeated display never resets the clock
            throw new PendingRuleException("T07", "Switching an unfinished question or revising a submitted answer is undecided.");
        }
        rules.requireTiming();
        current = questionId; startedAt = now; excludedRewardTime = Duration.ZERO;
    }
    AnswerTicket submit(long now, CombatRules rules) {
        if (current == null || submitted != null || reward != null)
            throw new IllegalStateException("An answering question without a pending reward is required.");
        submittedElapsed = elapsed(now, rules);
        submitted = new AnswerTicket(id, UUID.randomUUID(), current);
        return submitted;
    }
    Answer evaluate(AnswerTicket ticket, double fraction, String evidence, long now, CombatRules rules, GameState state) {
        if (submitted == null || !submitted.equals(ticket)) throw new IllegalStateException("Stale or duplicate answer result.");
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1 || evidence == null || evidence.isBlank())
            throw new IllegalArgumentException("Trusted evaluation needs a 0..1 fraction and evidence.");
        Duration duration = elapsed(now, rules);
        boolean late = duration.compareTo(CombatRules.QUESTION_LIMIT) > 0;
        boolean dasher = answers.size() == entries.size() - 1;
        double initial = entry(current).initialWeight;
        double weight = rules.weight(initial, late, dasher);
        if (rules.hpCost() == null) throw new PendingRuleException("T01", "Combat HP cost formula is not configured.");
        double cost = rules.hpCost().forAnswer(duration);
        if (!Double.isFinite(cost) || cost <= 0) throw new IllegalArgumentException("Answer HP cost must be finite and positive.");
        double appliedCost = state.isTutorial() ? 0 : cost;
        if (!state.isTutorial() && state.student.hp() - appliedCost <= 0)
            throw new PendingRuleException("T01", "HP depletion behavior is not approved.");
        var answer = new Answer(current, duration, late, dasher, initial, weight, fraction,
                fraction * weight, cost, appliedCost, evidence);
        answers.add(answer); current = null; submitted = null; submittedElapsed = null;
        return answer;
    }
    RewardTicket beginReward(RewardKind kind, long now, CombatRules rules) {
        Objects.requireNonNull(kind);
        if (current == null || submitted != null || reward != null)
            throw new IllegalStateException("Reward timing requires an answering question and no pending action.");
        if (rules.rewardTimeCounts() == null)
            throw new PendingRuleException("T09", "Whether reward duration counts toward the question clock is undecided.");
        Duration duration = kind == RewardKind.SAFE ? CombatRules.SAFE_REWARD_DURATION : rules.riskyRewardDuration();
        if (duration == null) throw new PendingRuleException("T09", "Risky reward duration (at most 20s) is not configured.");
        reward = new RewardTicket(id, UUID.randomUUID(), current, kind, duration); rewardStartedAt = now;
        return reward;
    }
    void finishReward(RewardTicket ticket, long now, CombatRules rules) {
        if (reward == null || !reward.equals(ticket)) throw new IllegalStateException("Stale or duplicate reward result.");
        if (since(now, rewardStartedAt).compareTo(reward.duration) < 0)
            throw new IllegalStateException("Reward duration has not elapsed.");
        if (Boolean.FALSE.equals(rules.rewardTimeCounts())) excludedRewardTime = excludedRewardTime.plus(reward.duration);
        reward = null;
    }
    AnswerTicket pendingAnswer() { return submitted; }
    RewardTicket pendingReward() { return reward; }
    boolean complete() { return answers.size() == entries.size(); }
    Summary summary() {
        if (!complete()) throw new IllegalStateException("Finish all combat questions first.");
        boolean combo = answers.stream().noneMatch(Answer::timedOut);
        double credits = answers.stream().mapToDouble(Answer::weightedCredit).sum(); StatChange.requireFinite(credits);
        return new Summary(answers, credits, combo ? ComboStatus.EARNED : ComboStatus.LOST, combo ? CombatRules.COMBO_BONUS : 0);
    }
    View view(long now, CombatRules rules) {
        Duration duration = elapsed(now, rules);
        boolean late = duration.compareTo(CombatRules.QUESTION_LIMIT) > 0;
        boolean lost = late || answers.stream().anyMatch(Answer::timedOut);
        var combo = lost ? ComboStatus.LOST : complete() ? ComboStatus.EARNED : ComboStatus.IN_PROGRESS;
        var questions = entries.stream().map(e -> new QuestionView(e.question.id(), e.question.prompt(),
                e.question.difficultyRank(), e.initialWeight, answered(e.question.id()) ? QuestionStatus.ANSWERED
                : e.question.id().equals(current) ? submitted == null ? QuestionStatus.ANSWERING : QuestionStatus.SUBMITTED
                : QuestionStatus.AVAILABLE)).toList();
        Duration remaining = reward == null ? Duration.ZERO : reward.duration.minus(since(now, rewardStartedAt));
        if (remaining.isNegative()) remaining = Duration.ZERO;
        return new View(id, questions, current, duration, late, !complete() && answers.size() == entries.size() - 1,
                combo, complete(), answers, submitted, reward, remaining);
    }
}
