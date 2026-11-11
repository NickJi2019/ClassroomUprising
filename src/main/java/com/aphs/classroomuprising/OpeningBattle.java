package com.aphs.classroomuprising;

import java.time.Duration;
import java.util.*;

/** Special previous-semester exam. Mutations occur only on a GameController candidate copy. */
public final class OpeningBattle {
    private static final Duration TOTAL_TIME_LIMIT = Duration.ofMinutes(3);
    public enum Phase { SELECT_QUESTION, QUESTION_REVEAL, QUESTION, DEFEAT, VICTORY }
    public enum DefeatReason { WRONG_ANSWER, TIMEOUT, PASS_OUT }
    public enum AnimationCue { DEFEAT, PASS_OUT, VICTORY }
    public record Feedback(String questionId, String selectedOption, boolean correct,
                           String correctOption, String explanation) { }
    public record Ending(int grade, DefeatReason reason, AnimationCue animation, List<String> dialogue) {
        public Ending { dialogue = List.copyOf(dialogue); }
    }
    public record View(Phase phase, StudentState student, int ducksRemaining, boolean usingDuck,
                       Duration duckRemaining, List<OpeningContent.QuestionView> availableQuestions,
                       String activeQuestionId, int questionOrdinal, Duration timeLimit,
                       Duration remaining, List<Feedback> answers, Ending ending) {
        public View { availableQuestions = List.copyOf(availableQuestions); answers = List.copyOf(answers); }
        public boolean finished() { return ending != null; }
        /** The exam clock runs in every phase until an ending has been settled. */
        public boolean clockRunning() { return !finished(); }
    }
    private final OpeningContent content;
    private StudentState student;
    private Phase phase = Phase.SELECT_QUESTION;
    private int ducks = 1;
    private boolean usingDuck;
    private final long battleStarted;
    private long duckStarted, lastObserved, finishedElapsedNanos;
    private String activeQuestion;
    private final List<Feedback> answers = new ArrayList<>();
    private Ending ending;

    OpeningBattle(OpeningContent content, long now) {
        this.content = Objects.requireNonNull(content); student = content.initialStudent();
        battleStarted = now; lastObserved = now;
    }
    OpeningBattle copy() {
        var b = new OpeningBattle(content, battleStarted);
        b.student = student; b.phase = phase; b.ducks = ducks; b.usingDuck = usingDuck;
        b.duckStarted = duckStarted; b.lastObserved = lastObserved; b.activeQuestion = activeQuestion;
        b.finishedElapsedNanos = finishedElapsedNanos;
        b.answers.addAll(answers); b.ending = ending; return b;
    }
    public static Duration totalTimeLimit() { return TOTAL_TIME_LIMIT; }
    /** Compatibility accessor: every ordinal refers to the same total budget, not a fresh allowance. */
    public static Duration timeLimit(int ordinal) {
        if (ordinal < 1 || ordinal > 10) throw new IllegalArgumentException("Opening question ordinal must be 1..10.");
        return totalTimeLimit();
    }
    private long elapsed(long now, long start) {
        long value = now - start; // nanoTime subtraction remains correct across signed wraparound
        if (value < 0) throw new IllegalStateException("Monotonic opening clock moved backwards.");
        return value;
    }
    private void live() { if (ending != null) throw new IllegalStateException("Opening battle already ended."); }
    void tick(long now) {
        if (ending != null) return;
        elapsed(now, lastObserved); lastObserved = now;
        boolean expired = elapsed(now, battleStarted) >= TOTAL_TIME_LIMIT.toNanos();
        // Settle events in their actual order, even if the frontend missed several timer ticks.
        boolean recovered = usingDuck && elapsed(now, duckStarted) >= content.duckDuration().toNanos();
        if (recovered && (!expired || content.duckDuration().toNanos()
                < TOTAL_TIME_LIMIT.toNanos() - elapsed(duckStarted, battleStarted))) {
            student = student.apply(new StatChange(content.duckRecovery(), 0, 0, Collections.nCopies(6, 0.0)), 1, 1, false);
            usingDuck = false; // HP has no gameplay cap (neither 9 nor 100).
        }
        if (expired) lose(DefeatReason.TIMEOUT);
    }
    /** Expiration takes priority even over stale/invalid callbacks, so a rejection cannot roll it back. */
    private boolean beginAction(long now) {
        tick(now);
        if (ending != null && ending.reason() == DefeatReason.TIMEOUT) return false;
        live();
        return true;
    }
    void hit(String id, long now) {
        if (!beginAction(now)) return;
        if (phase != Phase.SELECT_QUESTION || usingDuck) throw new IllegalStateException("Finish the current action first.");
        content.question(id);
        if (answers.stream().anyMatch(a -> a.questionId().equals(id))) throw new IllegalArgumentException("Already answered.");
        activeQuestion = id; phase = Phase.QUESTION_REVEAL;
        // This call is the bullet's impact. A submitted answer must never charge this HP again.
        student = student.apply(new StatChange(-1, 0, 0, Collections.nCopies(6, 0.0)), 1, 1, false);
        if (student.hp() <= 0) lose(DefeatReason.PASS_OUT);
    }
    /** Presentation completion permits answering; it never starts, pauses or resets the exam clock. */
    void start(String id, long now) {
        if (!beginAction(now)) return;
        if (phase != Phase.QUESTION_REVEAL || !Objects.equals(activeQuestion, id))
            throw new IllegalStateException("Reveal the impacted question before allowing answers.");
        phase = Phase.QUESTION;
    }
    void answer(String id, String option, long now) {
        if (!beginAction(now)) return;
        if (phase != Phase.QUESTION || !Objects.equals(activeQuestion, id)) throw new IllegalStateException("Stale opening question.");
        var q = content.question(id);
        if (q.view().options().stream().noneMatch(o -> o.id().equals(option))) throw new IllegalArgumentException("Unknown answer option.");
        if (usingDuck) throw new IllegalStateException("Duck use is still in progress.");
        boolean correct = q.answer().equals(option);
        answers.add(new Feedback(id, option, correct, q.answer(), q.explanation()));
        if (!correct) lose(DefeatReason.WRONG_ANSWER);
        else if (answers.size() == 10) {
            phase = Phase.VICTORY; finishedElapsedNanos = elapsed(now, battleStarted);
            ending = new Ending(89, null, AnimationCue.VICTORY, content.victory());
        } else { phase = Phase.SELECT_QUESTION; activeQuestion = null; }
    }
    void useDuck(long now) {
        if (!beginAction(now)) return;
        if (phase == Phase.QUESTION_REVEAL) throw new IllegalStateException("Wait until the question finishes revealing.");
        if (ducks == 0 || usingDuck) throw new IllegalStateException("No unused rubber duck remains.");
        ducks--; usingDuck = true; duckStarted = now;
    }
    private void lose(DefeatReason reason) {
        phase = Phase.DEFEAT; usingDuck = false;
        finishedElapsedNanos = Math.min(TOTAL_TIME_LIMIT.toNanos(), elapsed(lastObserved, battleStarted));
        ending = new Ending(67, reason, reason == DefeatReason.PASS_OUT ? AnimationCue.PASS_OUT : AnimationCue.DEFEAT, content.defeat());
    }
    boolean finished() { return ending != null; }
    /** Pure observation. Call advanceOpeningBattle first to settle deadlines/recovery. */
    View view(long now) {
        if (ending == null) elapsed(now, lastObserved);
        int ordinal = Math.min(10, answers.size() + (ending == null ? 1 : 0));
        // Timeout and a lethal impact precede submission; neither fabricates answer feedback.
        if (ending != null && (ending.reason() == DefeatReason.TIMEOUT || ending.reason() == DefeatReason.PASS_OUT))
            ordinal = answers.size() + 1;
        Duration limit = totalTimeLimit();
        long spent = ending == null ? elapsed(now, battleStarted) : finishedElapsedNanos;
        Duration remaining = Duration.ofNanos(Math.max(0, limit.toNanos() - spent)); // frozen only at an ending
        Duration duckLeft = usingDuck ? Duration.ofNanos(Math.max(0, content.duckDuration().toNanos() - elapsed(now, duckStarted))) : Duration.ZERO;
        var available = content.questions().stream().filter(q -> answers.stream().noneMatch(a -> a.questionId().equals(q.id()))).toList();
        return new View(phase, student, ducks, usingDuck, duckLeft, available, activeQuestion,
                Math.max(1, ordinal), limit, remaining, answers, ending);
    }
}
