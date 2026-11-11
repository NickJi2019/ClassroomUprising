package com.aphs.classroomuprising;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/** Special previous-semester exam. Mutations occur only on a GameController candidate copy. */
public final class OpeningBattle {
    public enum Phase { SELECT_QUESTION, QUESTION, DEFEAT, VICTORY }
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
    }
    private final OpeningContent content;
    private StudentState student;
    private Phase phase = Phase.SELECT_QUESTION;
    private int ducks = 1;
    private boolean usingDuck;
    private long duckStarted, questionStarted, lastObserved;
    private String activeQuestion;
    private final List<Feedback> answers = new ArrayList<>();
    private Ending ending;

    OpeningBattle(OpeningContent content, long now) {
        this.content = Objects.requireNonNull(content); student = content.initialStudent(); lastObserved = now;
    }
    OpeningBattle copy() {
        var b = new OpeningBattle(content, lastObserved);
        b.student = student; b.phase = phase; b.ducks = ducks; b.usingDuck = usingDuck;
        b.duckStarted = duckStarted; b.questionStarted = questionStarted; b.activeQuestion = activeQuestion;
        b.answers.addAll(answers); b.ending = ending; return b;
    }
    /** Ordinal means the order chosen by the player, not the PDF question number. */
    public static Duration timeLimit(int ordinal) {
        if (ordinal < 1 || ordinal > 10) throw new IllegalArgumentException("Opening question ordinal must be 1..10.");
        return Duration.ofNanos(new BigDecimal("20000000000").multiply(new BigDecimal("0.9").pow(ordinal - 1)).longValueExact());
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
        boolean expired = phase == Phase.QUESTION && elapsed(now, questionStarted) >= timeLimit(answers.size() + 1).toNanos();
        // Settle events in their actual order, even if the frontend missed several timer ticks.
        boolean recovered = usingDuck && elapsed(now, duckStarted) >= content.duckDuration().toNanos();
        if (recovered && (!expired || content.duckDuration().toNanos()
                < timeLimit(answers.size() + 1).toNanos() - elapsed(duckStarted, questionStarted))) {
            student = student.apply(new StatChange(content.duckRecovery(), 0, 0, Collections.nCopies(6, 0.0)), 1, 1, false);
            usingDuck = false; // HP has no gameplay cap (neither 9 nor 100).
        }
        if (expired) lose(DefeatReason.TIMEOUT);
    }
    void select(String id, long now) {
        live();
        if (phase != Phase.SELECT_QUESTION || usingDuck) throw new IllegalStateException("Finish the current action first.");
        content.question(id);
        if (answers.stream().anyMatch(a -> a.questionId().equals(id))) throw new IllegalArgumentException("Already answered.");
        tick(now); activeQuestion = id; questionStarted = now; phase = Phase.QUESTION;
    }
    void answer(String id, String option, long now) {
        live();
        if (phase != Phase.QUESTION || !Objects.equals(activeQuestion, id)) throw new IllegalStateException("Stale opening question.");
        var q = content.question(id);
        if (q.view().options().stream().noneMatch(o -> o.id().equals(option))) throw new IllegalArgumentException("Unknown answer option.");
        tick(now);
        if (ending != null) return; // A late answer commits the timeout; it cannot undo it by throwing.
        if (usingDuck) throw new IllegalStateException("Duck use is still in progress.");
        boolean correct = q.answer().equals(option);
        student = student.apply(new StatChange(-1, 0, 0, Collections.nCopies(6, 0.0)), 1, 1, false);
        answers.add(new Feedback(id, option, correct, q.answer(), q.explanation()));
        if (student.hp() <= 0) lose(DefeatReason.PASS_OUT);
        else if (!correct) lose(DefeatReason.WRONG_ANSWER);
        else if (answers.size() == 10) {
            phase = Phase.VICTORY;
            ending = new Ending(89, null, AnimationCue.VICTORY, content.victory());
        } else { phase = Phase.SELECT_QUESTION; activeQuestion = null; }
    }
    void useDuck(long now) {
        live();
        if (ducks == 0 || usingDuck) throw new IllegalStateException("No unused rubber duck remains.");
        tick(now);
        if (ending != null) return;
        ducks--; usingDuck = true; duckStarted = now;
    }
    private void lose(DefeatReason reason) {
        phase = Phase.DEFEAT; usingDuck = false;
        ending = new Ending(67, reason, reason == DefeatReason.PASS_OUT ? AnimationCue.PASS_OUT : AnimationCue.DEFEAT, content.defeat());
    }
    boolean finished() { return ending != null; }
    /** Pure observation. Call advanceOpeningBattle first to settle deadlines/recovery. */
    View view(long now) {
        if (ending == null) elapsed(now, lastObserved);
        int ordinal = Math.min(10, answers.size() + (phase == Phase.QUESTION || phase == Phase.SELECT_QUESTION ? 1 : 0));
        // A timeout may precede submission; preserve the active question's ordinal at the ending.
        if (ending != null && ending.reason() == DefeatReason.TIMEOUT) ordinal = answers.size() + 1;
        Duration limit = timeLimit(Math.max(1, ordinal));
        Duration remaining = phase == Phase.QUESTION ? Duration.ofNanos(Math.max(0, limit.toNanos() - elapsed(now, questionStarted))) : Duration.ZERO;
        Duration duckLeft = usingDuck ? Duration.ofNanos(Math.max(0, content.duckDuration().toNanos() - elapsed(now, duckStarted))) : Duration.ZERO;
        var available = content.questions().stream().filter(q -> answers.stream().noneMatch(a -> a.questionId().equals(q.id()))).toList();
        return new View(phase, student, ducks, usingDuck, duckLeft, available, activeQuestion,
                Math.max(1, ordinal), limit, remaining, answers, ending);
    }
}
