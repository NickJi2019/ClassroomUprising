package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatRules;
import com.aphs.classroomuprising.CombatSession;
import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameController;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.PendingRuleException;
import com.aphs.classroomuprising.RuleValues;
import com.aphs.classroomuprising.SaveManager;
import com.aphs.classroomuprising.StatChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Frontend controller for one playthrough. It forwards each player action to the
 * {@link GameController} exactly once, turns the core's refusals into {@link Outcome}s,
 * and owns where the day-end save file lives.
 *
 * <p>It keeps no copy of rules or game state: every read goes through
 * {@link GameController#snapshot()}, and every value shown to the player comes from the
 * Demo configuration supplied by the backend.</p>
 */
public final class GameSession {
    private final DemoProfile profile;
    private final GameController game;
    private final Path savePath;
    private final SaveManager saves = new SaveManager();

    private GameSession(DemoProfile profile, GameController game, Path savePath) {
        this.profile = profile;
        this.game = game;
        this.savePath = savePath;
    }

    /** Starts a new Day 1 run with combat policy still pending. Nothing is written until the first day ends. */
    public static GameSession newGame(DemoProfile profile, String name, RandomGenerator random, Path savePath) {
        return newGame(profile, name, random, savePath, CombatRules.pending());
    }

    /** Starts a new Day 1 run with the combat policy the backend supplies (pending values stay pending). */
    public static GameSession newGame(DemoProfile profile, String name, RandomGenerator random, Path savePath,
                                      CombatRules combatRules) {
        return new GameSession(profile, profile.newGame(name, random, combatRules), savePath);
    }

    /** Restores the saved closed day as it was. It does not start the next day or re-apply rewards. */
    public static GameSession load(DemoProfile profile, Path savePath, RandomGenerator random) throws IOException {
        return load(profile, savePath, random, CombatRules.pending());
    }

    /** As {@link #load(DemoProfile, Path, RandomGenerator)}, with the same combat policy as a new game. */
    public static GameSession load(DemoProfile profile, Path savePath, RandomGenerator random,
                                   CombatRules combatRules) throws IOException {
        GameState state = new SaveManager().load(savePath);
        return new GameSession(profile,
                new GameController(state, profile.rules(), profile.content(), random, combatRules), savePath);
    }

    /**
     * Wraps a controller built elsewhere, for tests and checks that need content the Demo does
     * not ship (an assessment bank, fixture combat rules, a fake clock). Not used by the game.
     */
    static GameSession over(DemoProfile profile, GameController game, Path savePath) {
        return new GameSession(profile, game, savePath);
    }

    public static boolean saveExists(Path savePath) { return Files.isRegularFile(savePath); }

    /** The single save slot used by the game window. */
    public static Path defaultSavePath() {
        return Path.of(System.getProperty("user.home"), ".classroom-uprising", "day-end-save.properties");
    }

    /** A fresh copy of the authoritative state; call again after every action. */
    public GameState view() { return game.snapshot(); }

    public DemoProfile profile() { return profile; }

    public RuleValues rules() { return profile.rules(); }

    public Path savePath() { return savePath; }

    // ---- player actions -------------------------------------------------------------

    public Outcome completeOpening() { return change(game::completeOpening); }

    /**
     * Returns today's Lesson. {@code showLesson()} records that the content was shown, so it
     * is called only the first time; later calls in the same class re-read the content.
     */
    public Fetch<ContentCatalog.Lesson> openLesson() {
        try {
            GameState now = game.snapshot();
            if (now.lessonShown()) return Fetch.of(profile.content().lesson(now.day()));
            return Fetch.of(game.showLesson());
        } catch (RuntimeException e) {
            return Fetch.failed(failure(e));
        }
    }

    public Outcome lessonChoice(String choiceId) { return change(() -> game.lessonChoice(choiceId)); }

    /**
     * Settles the lesson of {@code lessonDay} with one final option (a response or staying quiet).
     * The day is the one captured when that lesson was opened, so a late callback cannot settle a
     * different class: a stale day, an unknown option or a repeat changes nothing.
     */
    public Outcome lessonChoice(int lessonDay, String optionId) {
        return change(() -> game.lessonChoice(lessonDay, optionId));
    }

    public Outcome completeReview() { return change(game::completeReview); }

    public Outcome otherClasses(boolean reviseIcs) { return change(() -> game.otherClasses(reviseIcs)); }

    public Outcome hubActivity(String activityId) { return change(() -> game.hubActivity(activityId)); }

    public Outcome sleepAfterSchool() { return change(game::sleepImmediatelyAfterSchool); }

    public Outcome sleepNormally() { return change(game::sleepNormally); }

    public Outcome beginNextDay() { return change(game::beginNextDay); }

    // ---- assessment combat ----------------------------------------------------------
    // Timing, weights, HP, Dasher, Combo and the final mark are the backend's. These methods
    // forward one call each; the screen only displays what combatView() reports.

    /** Opens today's assessment as a combat. A second call is refused by the core. */
    public Fetch<GameController.AssessmentView> startCombatAssessment() { return fetch(game::startCombatAssessment); }

    /** A fresh, detached view of the running combat (questions in difficulty order, backend clock). */
    public Fetch<CombatSession.View> combatView() { return fetch(game::combatSnapshot); }

    /** Starts the clock on a question; repeating it for the open question never restarts the clock. */
    public Outcome startCombatQuestion(String questionId) { return change(() -> game.startCombatQuestion(questionId)); }

    /** Locks the open question's work for evaluation and returns the ticket its result must carry. */
    public Fetch<CombatSession.AnswerTicket> submitCombatAnswer() { return fetch(game::submitCombatAnswer); }

    /** Passes on a trusted evaluator's result for this ticket; stale or repeated tickets are refused. */
    public Outcome recordCombatEvaluation(CombatSession.AnswerTicket ticket, double earnedFraction, String evidence) {
        return change(() -> game.recordCombatEvaluation(ticket, earnedFraction, evidence));
    }

    /** Reserves reward time on the backend clock. It does not own, use up or apply any item (T09). */
    public Fetch<CombatSession.RewardTicket> beginRewardDelay(CombatSession.RewardKind kind) {
        return fetch(() -> game.beginCombatRewardDelay(kind));
    }

    /** Ends a reward delay; refused before its time has passed or for an old ticket. */
    public Outcome finishRewardDelay(CombatSession.RewardTicket ticket) {
        return change(() -> game.finishCombatRewardDelay(ticket));
    }

    /** The breakdown of a finished combat; refused until every question has been evaluated. */
    public Fetch<CombatSession.Summary> combatSummary() { return fetch(game::combatSummary); }

    /** Converts and records the whole assessment through the original result flow. */
    public Outcome finishCombatAssessment() { return change(game::finishCombatAssessment); }

    /** Writes the day-end snapshot. The core accepts this only at a legal day end. */
    public Outcome saveDayEnd() {
        GameState now = game.snapshot();
        try {
            saves.saveAtDayEnd(now, savePath);
            return new Outcome.Done("Day-end snapshot saved.", List.of(), now, now);
        } catch (IOException e) {
            return new Outcome.Rejected("Save failed: " + e.getMessage());
        } catch (RuntimeException e) {
            return failure(e);
        }
    }

    // ---- read-only configuration for previews ---------------------------------------

    public List<String> hubActivityIds() { return profile.content().hubActivityIds(); }

    public Fetch<StatChange> hubEffect(String activityId) {
        try {
            return Fetch.of(profile.content().hub(activityId));
        } catch (RuntimeException e) {
            return Fetch.failed(failure(e));
        }
    }

    public Fetch<StatChange> revisionEffect() {
        try {
            return Fetch.of(profile.content().otherRevision());
        } catch (RuntimeException e) {
            return Fetch.failed(failure(e));
        }
    }

    // ---- helpers --------------------------------------------------------------------

    private static <T> Fetch<T> fetch(Supplier<T> request) {
        try {
            return Fetch.of(request.get());
        } catch (RuntimeException e) {
            return Fetch.failed(failure(e));
        }
    }

    private Outcome change(Supplier<GameController.Result> action) {
        GameState before = game.snapshot();
        try {
            GameController.Result result = action.get();
            return new Outcome.Done(result.message(), result.warnings(), before, game.snapshot());
        } catch (RuntimeException e) {
            return failure(e);
        }
    }

    /** Maps the core's deliberate refusals; any other exception is a bug and is not hidden. */
    private static Outcome failure(RuntimeException e) {
        if (e instanceof PendingRuleException pending) return new Outcome.Pending(pending.ruleId(), detail(pending));
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException)
            return new Outcome.Rejected(e.getMessage());
        throw e;
    }

    private static String detail(PendingRuleException e) {
        String message = e.getMessage();
        String prefix = e.ruleId() + ": ";
        return message.startsWith(prefix) ? message.substring(prefix.length()) : message;
    }
}
