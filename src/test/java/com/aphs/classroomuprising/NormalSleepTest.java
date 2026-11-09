package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Path;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class NormalSleepTest {
    @TempDir Path temp;
    private RuleValues rules(Double recovery) {
        return new RuleValues(4.0, .5, 3.0, 7.0, 12.0, RuleValues.TutorialAbsences.IGNORE, recovery);
    }
    private GameController game(GameState state, Double recovery) {
        return new GameController(state, rules(recovery), Fixtures.catalog(), new Random(42));
    }

    @ParameterizedTest @EnumSource(value=DayPeriod.class, names={"EVENING","LATE_NIGHT"})
    void closesDayWithoutEarlyRewardOrLatePenalty(DayPeriod period) {
        var game = game(Fixtures.state(3, period), 6.0);
        var before = game.snapshot();
        var result = game.sleepNormally();
        var after = game.snapshot();
        assertEquals("Normal sleep completed.", result.message());
        assertEquals(DayPeriod.DAY_END, after.period());
        assertEquals(66, after.student().san());
        assertEquals(before.student().hp(), after.student().hp());
        assertEquals(before.student().codingVibe(), after.student().codingVibe());
        assertEquals(before.student().unitKnowledge(), after.student().unitKnowledge());
        assertFalse(after.hasMorningBonus()); assertTrue(after.effects().isEmpty()); assertTrue(after.canSave());
        String closed = Fixtures.signature(after);
        assertThrows(IllegalStateException.class, game::sleepNormally);
        assertEquals(closed, Fixtures.signature(game.snapshot()));
        game.beginNextDay();
        assertEquals(DayPeriod.ICS_CLASS, game.snapshot().period());
        assertEquals(66, game.snapshot().effectiveSan()); assertEquals(1, game.snapshot().xpMultiplier());
    }

    @ParameterizedTest @EnumSource(value=DayPeriod.class, names={"EVENING","LATE_NIGHT"}, mode=EnumSource.Mode.EXCLUDE)
    void rejectsOtherPeriodsWithoutMutation(DayPeriod period) {
        var game = game(Fixtures.state(3, period), 6.0);
        String before = Fixtures.signature(game.snapshot());
        assertThrows(IllegalStateException.class, game::sleepNormally);
        assertEquals(before, Fixtures.signature(game.snapshot()));
    }

    @Test void pendingRecoveryRemainsExplicitAndAtomic() {
        var game = game(Fixtures.state(3, DayPeriod.EVENING), null);
        String before = Fixtures.signature(game.snapshot());
        assertEquals("T04", assertThrows(PendingRuleException.class, game::sleepNormally).ruleId());
        assertEquals(before, Fixtures.signature(game.snapshot()));
        assertNull(Fixtures.rules().normalSleepSanRecovery()); assertNull(RuleValues.pending().normalSleepSanRecovery());
    }

    @ParameterizedTest @ValueSource(doubles={-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidRecovery(double recovery) { assertThrows(IllegalArgumentException.class, () -> rules(recovery)); }

    @Test void zeroIsAnExplicitValidRecovery() {
        var game = game(Fixtures.state(3, DayPeriod.EVENING), 0.0);
        game.sleepNormally(); assertEquals(60, game.snapshot().student().san());
    }

    @Test void tutorialAllowsConfiguredRecoveryWithoutDeferredPenalty() {
        var game = game(Fixtures.state(2, DayPeriod.LATE_NIGHT), 6.0);
        game.sleepNormally(); game.beginNextDay();
        assertEquals(66, game.snapshot().effectiveSan()); assertTrue(game.snapshot().effects().isEmpty());
    }

    @Test void sleepDoesNotEraseUnrelatedExistingEffects() {
        var state = Fixtures.state(3, DayPeriod.EVENING);
        state.effects.add(new TimedEffect("late-night:2", 3, 3, -4, .5));
        state.effects.add(new TimedEffect("other", 3, 4, -2, .8));
        var game = game(state, 6.0);
        game.sleepNormally();
        assertEquals(state.effects(), game.snapshot().effects()); assertEquals(60, game.snapshot().effectiveSan());
        assertEquals(.4, game.snapshot().xpMultiplier());
        game.beginNextDay();
        assertEquals(64, game.snapshot().effectiveSan()); assertEquals(.8, game.snapshot().xpMultiplier());
    }

    @Test void checkpointOrUnacknowledgedOpeningStillBlocksSleep() {
        var state = Fixtures.state(3, DayPeriod.EVENING);
        state.status = GameState.Status.MIDTERM_DECISION;
        var game = game(state, 6.0);
        String before = Fixtures.signature(game.snapshot());
        assertThrows(IllegalStateException.class, game::sleepNormally);
        assertEquals(before, Fixtures.signature(game.snapshot()));
        state.status = GameState.Status.ACTIVE; state.openingCompleted = false;
        var unopened = game(state, 6.0);
        assertThrows(IllegalStateException.class, unopened::sleepNormally);
    }

    @Test void saveLoadDoesNotReapplyRecovery() throws Exception {
        var game = game(Fixtures.state(3, DayPeriod.LATE_NIGHT), 6.0);
        game.sleepNormally();
        Path file = temp.resolve("normal.properties");
        var saves = new SaveManager(); saves.saveAtDayEnd(game.snapshot(), file);
        var loaded = game(saves.load(file), 6.0);
        assertEquals(Fixtures.signature(game.snapshot()), Fixtures.signature(loaded.snapshot()));
        assertThrows(IllegalStateException.class, loaded::sleepNormally);
        loaded.beginNextDay(); assertEquals(66, loaded.snapshot().student().san());
        assertEquals(DayPeriod.ICS_CLASS, loaded.snapshot().period());
    }

    @Test void overflowRejectsWithoutClosingTheDay() {
        var state = Fixtures.state(3, DayPeriod.EVENING);
        state.student = new StudentState(50, Double.MAX_VALUE, 0, state.student().unitKnowledge());
        var game = game(state, Double.MAX_VALUE);
        String before = Fixtures.signature(game.snapshot());
        assertThrows(IllegalArgumentException.class, game::sleepNormally);
        assertEquals(before, Fixtures.signature(game.snapshot()));
    }
}
