package com.aphs.classroomuprising.ui;

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

    /** Starts a new Day 1 run. Nothing is written to disk until the first day ends. */
    public static GameSession newGame(DemoProfile profile, String name, RandomGenerator random, Path savePath) {
        return new GameSession(profile, profile.newGame(name, random), savePath);
    }

    /** Restores the saved closed day as it was. It does not start the next day or re-apply rewards. */
    public static GameSession load(DemoProfile profile, Path savePath, RandomGenerator random) throws IOException {
        GameState state = new SaveManager().load(savePath);
        return new GameSession(profile, new GameController(state, profile.rules(), profile.content(), random), savePath);
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

    public Outcome completeReview() { return change(game::completeReview); }

    public Outcome otherClasses(boolean reviseIcs) { return change(() -> game.otherClasses(reviseIcs)); }

    public Outcome hubActivity(String activityId) { return change(() -> game.hubActivity(activityId)); }

    public Outcome sleepAfterSchool() { return change(game::sleepImmediatelyAfterSchool); }

    public Outcome sleepNormally() { return change(game::sleepNormally); }

    public Outcome beginNextDay() { return change(game::beginNextDay); }

    public Fetch<GameController.AssessmentView> startAssessment() {
        try {
            return Fetch.of(game.startAssessment());
        } catch (RuntimeException e) {
            return Fetch.failed(failure(e));
        }
    }

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
