package com.aphs.classroomuprising;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class OpeningBattleTest {
    @TempDir Path temp;
    AtomicLong clock = new AtomicLong();
    DemoProfile demo;
    GameController game;
    @BeforeEach void setup() throws Exception {
        demo = DemoProfile.load();
        game = new GameController(new GameState("Student", demo.initialStudent()), demo.rules(), demo.content(),
                new Random(42), CombatRules.pending(), clock::get);
    }
    String id(int index) { return demo.content().opening().questions().get(index).id(); }
    void start() { game.startOpeningBattle(); }
    void reveal(String id) { game.hitOpeningQuestion(id); game.startOpeningQuestion(id); }
    void correct(int index) {
        String id = id(index); reveal(id);
        game.answerOpeningQuestion(id, demo.content().opening().question(id).answer());
    }
    void duck() { game.useOpeningDuck(); clock.addAndGet(5_000_000_000L); game.advanceOpeningBattle(); }
    @Test void initialOpeningIsIsolatedAndCannotBeSkippedOrSaved() throws Exception {
        var before = game.snapshot().student();
        assertThrows(IllegalStateException.class, game::completeOpening);
        assertThrows(IllegalStateException.class, game::openingBattle);
        start(); var v = game.openingBattle();
        assertEquals(9, v.student().hp()); assertEquals(1, v.student().san());
        assertEquals(0, v.student().codingVibe()); assertEquals(Collections.nCopies(6, 0.0), v.student().unitKnowledge());
        assertEquals(1, v.ducksRemaining()); assertEquals(before, game.snapshot().student());
        assertEquals(10, v.availableQuestions().size()); assertEquals(5, v.availableQuestions().getFirst().options().size());
        assertThrows(IllegalStateException.class, game::startOpeningBattle);
        assertThrows(IllegalStateException.class, game::showLesson);
        assertThrows(IllegalStateException.class, () -> new SaveManager().saveAtDayEnd(game.snapshot(), temp.resolve("save")));
    }
    @Test void wrongAnswerKeepsTheImpactCostAndImmediatelyEndsAt67WithMocking() {
        start(); reveal(id(0)); assertEquals(8, game.openingBattle().student().hp());
        game.answerOpeningQuestion(id(0), "B");
        var v = game.openingBattle(); assertEquals(8, v.student().hp());
        assertFalse(v.answers().getFirst().correct()); assertEquals("A", v.answers().getFirst().correctOption());
        assertEquals(OpeningBattle.DefeatReason.WRONG_ANSWER, v.ending().reason()); assertEquals(67, v.ending().grade());
        assertTrue(v.ending().dialogue().stream().anyMatch(s -> s.contains("WOHAHAHAHA")));
        assertEquals(OpeningBattle.AnimationCue.DEFEAT, v.ending().animation());
    }
    @Test void ninthUnhealedImpactPassesOutBeforeAnswerAndTutorialWaiverDoesNotApply() {
        start(); for (int i = 0; i < 8; i++) correct(i);
        game.hitOpeningQuestion(id(8));
        var v = game.openingBattle(); assertEquals(0, v.student().hp()); assertEquals(8, v.answers().size());
        assertEquals(id(8), v.activeQuestionId()); assertEquals(9, v.questionOrdinal());
        assertEquals(Duration.ofMinutes(3), v.timeLimit()); assertEquals(Duration.ofMinutes(3), v.remaining());
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(8), "D"));
        assertThrows(IllegalStateException.class, game::useOpeningDuck);
        assertEquals(v, game.openingBattle());
        assertEquals(OpeningBattle.DefeatReason.PASS_OUT, v.ending().reason());
        assertEquals(OpeningBattle.AnimationCue.PASS_OUT, v.ending().animation());
        assertEquals(67, v.ending().grade()); assertEquals(1, v.student().san());
    }
    @Test void oneDuckEnablesAllTenCorrectAndVictoryAt89WithoutNormalCombatPolicies() {
        start(); for (int i = 0; i < 8; i++) correct(i);
        duck(); correct(8); correct(9);
        var v = game.openingBattle(); assertEquals(2, v.student().hp()); assertEquals(0, v.ducksRemaining());
        assertEquals(10, v.answers().size()); assertEquals(OpeningBattle.Phase.VICTORY, v.phase());
        assertEquals(89, v.ending().grade()); assertNull(v.ending().reason());
        assertTrue(v.ending().dialogue().stream().anyMatch(s -> s.contains("wasn't good enough")));
        assertTrue(game.snapshot().grades().records().isEmpty());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void bothEndingsEnterFreshDayOneAndDayEndSaveRestoresWithoutReplay(boolean win) throws Exception {
        start();
        if (win) { duck(); for (int i = 0; i < 10; i++) correct(i); }
        else { reveal(id(0)); game.answerOpeningQuestion(id(0), "B"); }
        game.completeOpening(); var s = game.snapshot();
        assertEquals(demo.initialStudent(), s.student()); assertEquals(1, s.day());
        assertTrue(s.openingCompleted()); assertEquals(DayPeriod.ICS_CLASS, s.period());
        assertTrue(s.grades().records().isEmpty()); assertEquals(0, s.grades().unexcusedAbsences());
        assertThrows(IllegalStateException.class, game::completeOpening);
        assertThrows(IllegalStateException.class, game::openingBattle);
        game.showLesson(); game.lessonChoice("quiet"); game.otherClasses(false); game.sleepImmediatelyAfterSchool();
        var file = temp.resolve("day1.properties"); new SaveManager().saveAtDayEnd(game.snapshot(), file);
        var loaded = new SaveManager().load(file); assertTrue(loaded.openingCompleted()); assertNull(loaded.openingBattle);
        var resumed = new GameController(loaded, demo.rules(), demo.content(), new Random(42));
        assertThrows(IllegalStateException.class, resumed::startOpeningBattle);
        resumed.beginNextDay(); assertEquals(2, resumed.snapshot().day());
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6,7,8,9,10})
    void anyQuestionExpiresAtTheSharedBudgetBoundaryNotItsOrdinal(int ordinal) {
        start(); duck();
        for (int i = 0; i < ordinal - 1; i++) {
            reveal(id(i)); clock.addAndGet(7_000_000_000L);
            game.answerOpeningQuestion(id(i), demo.content().opening().question(id(i)).answer());
        }
        reveal(id(ordinal - 1));
        long remaining = (180L - 7L * (ordinal - 1)) * 1_000_000_000L;
        assertEquals(Duration.ofMinutes(3), OpeningBattle.totalTimeLimit());
        assertEquals(Duration.ofMinutes(3), OpeningBattle.timeLimit(ordinal));
        assertEquals(Duration.ofMinutes(3), game.openingBattle().timeLimit());
        assertEquals(Duration.ofNanos(remaining), game.openingBattle().remaining());
        clock.addAndGet(remaining - 1); game.advanceOpeningBattle(); assertFalse(game.openingBattle().finished());
        clock.incrementAndGet(); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
        assertEquals(Duration.ZERO, game.openingBattle().remaining()); assertFalse(game.openingBattle().clockRunning());
        assertEquals(ordinal - 1, game.openingBattle().answers().size());
        assertEquals(ordinal, game.openingBattle().questionOrdinal());
    }
    @Test void oneRemainingNanosecondCarriesAcrossFeedbackAndTheNextReveal() {
        start(); reveal(id(0)); clock.set(179_999_999_999L);
        game.answerOpeningQuestion(id(0), "A"); assertTrue(game.openingBattle().answers().getFirst().correct());
        assertEquals(Duration.ofNanos(1), game.openingBattle().remaining());
        clock.addAndGet(Duration.ofDays(1).toNanos()); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.Phase.SELECT_QUESTION, game.openingBattle().phase());
        assertEquals(Duration.ofNanos(1), game.openingBattle().remaining());
        reveal(id(1)); assertEquals(Duration.ofNanos(1), game.openingBattle().remaining());
        clock.incrementAndGet(); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
    }
    @Test void lateSubmissionCommitsTimeoutInsteadOfThrowingAndRollingItBack() {
        start(); reveal(id(0)); clock.set(180_000_000_000L);
        game.answerOpeningQuestion(id(0), "A"); var v = game.openingBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, v.ending().reason()); assertEquals(8, v.student().hp());
        assertTrue(v.answers().isEmpty());
    }
    @Test void coreRetainsExplicitQuestionIdCompatibilityWithoutRequiringAPlayerPicker() {
        start(); correct(9); reveal(id(0));
        assertEquals(2, game.openingBattle().questionOrdinal()); assertEquals(Duration.ofMinutes(3), game.openingBattle().timeLimit());
        game.answerOpeningQuestion(id(0), "A");
        var before = game.openingBattle(); assertThrows(IllegalArgumentException.class, () -> reveal(id(9)));
        assertEquals(before, game.openingBattle());
    }
    @Test void duckConsumesInventoryImmediatelyAndRecoversOnlyAfterFiveSecondsWithNoNineHpCap() {
        start(); reveal(id(0)); game.useOpeningDuck();
        assertEquals(0, game.openingBattle().ducksRemaining()); assertEquals(8, game.openingBattle().student().hp());
        var before = game.openingBattle();
        assertThrows(IllegalStateException.class, game::useOpeningDuck); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(0), "A"));
        clock.set(4_999_999_999L); game.advanceOpeningBattle(); assertEquals(8, game.openingBattle().student().hp());
        clock.incrementAndGet(); game.advanceOpeningBattle(); var v = game.openingBattle();
        assertEquals(11, v.student().hp()); assertEquals(Duration.ofSeconds(175), v.remaining());
        assertFalse(v.usingDuck()); assertEquals(1, v.student().san()); assertEquals(0, v.student().codingVibe());
        assertEquals(Collections.nCopies(6, 0.0), v.student().unitKnowledge());
        assertThrows(IllegalStateException.class, game::useOpeningDuck);
    }
    @Test void hpHasNoHundredCapEither() throws Exception {
        String raw;
        try (var in = getClass().getResourceAsStream("/demo/opening.properties")) { raw = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        var content = OpeningContent.read(new StringReader(raw.replace("duck.hp=3", "duck.hp=200")));
        var b = new OpeningBattle(content, 0); b.useDuck(0); b.tick(5_000_000_000L);
        assertEquals(209, b.view(5_000_000_000L).student().hp()); // Explicit test configuration, not the shipped duck.
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void hpZeroOnTenthImpactPreventsEitherAnswerAndVictory(boolean wrong) throws Exception {
        String raw;
        try (var in = getClass().getResourceAsStream("/demo/opening.properties")) { raw = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        var content = OpeningContent.read(new StringReader(raw.replace("duck.hp=3", "duck.hp=1")));
        var b = new OpeningBattle(content, 0); b.useDuck(0); b.tick(5_000_000_000L);
        for (int i = 0; i < 9; i++) {
            var q = content.question(id(i)); b.hit(id(i), 5_000_000_000L); b.start(id(i), 5_000_000_000L);
            b.answer(id(i), q.answer(), 5_000_000_000L);
        }
        b.hit(id(9), 5_000_000_000L);
        var v = b.view(5_000_000_000L); assertEquals(0, v.student().hp());
        assertEquals(67, v.ending().grade()); assertEquals(OpeningBattle.DefeatReason.PASS_OUT, v.ending().reason());
        assertEquals(10, v.questionOrdinal()); assertEquals(9, v.answers().size());
        assertThrows(IllegalStateException.class, () -> b.answer(id(9), wrong ? "A" : "E", 5_000_000_000L));
        assertEquals(v, b.view(5_000_000_000L));
    }
    @ParameterizedTest @ValueSource(longs={175_000_000_000L,176_000_000_000L})
    void timeoutAtOrBeforeRecoveryWinsAndCannotBeResurrected(long useAt) {
        start(); reveal(id(0)); clock.set(useAt); game.useOpeningDuck();
        clock.set(185_000_000_000L); game.advanceOpeningBattle();
        var v = game.openingBattle(); assertEquals(8, v.student().hp()); assertFalse(v.usingDuck());
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, v.ending().reason());
        game.advanceOpeningBattle(); assertEquals(v, game.openingBattle());
        assertThrows(IllegalStateException.class, game::useOpeningDuck);
    }
    @Test void missedTimerPulseStillAppliesHealingThatFinishedBeforeTimeout() {
        start(); reveal(id(0)); clock.set(174_000_000_000L); game.useOpeningDuck();
        clock.set(185_000_000_000L); game.advanceOpeningBattle();
        assertEquals(11, game.openingBattle().student().hp());
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
    }
    @Test void duckClickAtDeadlineCommitsTimeoutAndDoesNotConsumeUnusedItem() {
        start(); reveal(id(0)); clock.set(180_000_000_000L); game.useOpeningDuck();
        assertEquals(1, game.openingBattle().ducksRemaining()); assertTrue(game.openingBattle().finished());
    }
    @Test void invalidStaleAndRepeatedOperationsAreAtomicAndEndingDoesNotRepeat() {
        start(); var before = game.openingBattle();
        assertThrows(IllegalArgumentException.class, () -> reveal("missing")); assertEquals(before, game.openingBattle());
        reveal(id(0)); before = game.openingBattle();
        assertThrows(IllegalArgumentException.class, () -> game.answerOpeningQuestion(id(0), "F")); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(1), "B")); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> reveal(id(1))); assertEquals(before, game.openingBattle());
        game.answerOpeningQuestion(id(0), "B"); before = game.openingBattle();
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(0), "A")); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> reveal(id(1)));
        game.advanceOpeningBattle(); assertEquals(before, game.openingBattle());
    }
    @Test void snapshotsViewsAndIndependentControllersDoNotShareMutableBattleState() {
        start(); var snapshot = game.snapshot(); var v = game.openingBattle();
        var other = new GameController(snapshot, demo.rules(), demo.content(), new Random(42), CombatRules.pending(), clock::get);
        assertThrows(UnsupportedOperationException.class, () -> v.availableQuestions().clear());
        assertThrows(UnsupportedOperationException.class, () -> v.availableQuestions().getFirst().options().clear());
        assertThrows(UnsupportedOperationException.class, () -> v.answers().clear());
        correct(0); assertEquals(9, other.openingBattle().student().hp()); assertEquals(9, snapshot.openingBattle.view(0).student().hp());
        assertTrue(v.answers().isEmpty());
    }
    @Test void monotonicClockRegressionRejectsTransactionAndNanoTimeWrapIsHandled() {
        clock.set(Long.MAX_VALUE - 10); start(); reveal(id(0));
        clock.addAndGet(100); game.advanceOpeningBattle(); // wraps the signed long, but elapsed is still 100 ns
        var before = game.openingBattle(); clock.decrementAndGet();
        assertThrows(IllegalStateException.class, game::advanceOpeningBattle);
        clock.incrementAndGet(); assertEquals(before, game.openingBattle());
    }
    @ParameterizedTest @ValueSource(longs={760_000_000L,60_000_000_000L})
    void revealTimeDoesNotConsumeAnyAnswerTimeOrChargeExtraHp(long revealNanos) {
        start(); game.hitOpeningQuestion(id(0));
        var impact = game.openingBattle();
        assertEquals(8, impact.student().hp()); assertEquals(OpeningBattle.Phase.QUESTION_REVEAL, impact.phase());
        assertEquals(Duration.ofMinutes(3), impact.remaining()); assertEquals(Duration.ofMinutes(3), impact.timeLimit());
        assertFalse(impact.clockRunning());
        assertTrue(impact.answers().isEmpty());
        clock.addAndGet(revealNanos); game.advanceOpeningBattle();
        assertFalse(game.openingBattle().finished()); assertEquals(8, game.openingBattle().student().hp());
        assertEquals(Duration.ofMinutes(3), game.openingBattle().remaining());
        game.startOpeningQuestion(id(0));
        assertEquals(Duration.ofMinutes(3), game.openingBattle().remaining());
        clock.addAndGet(179_999_999_999L); game.answerOpeningQuestion(id(0), "A");
        assertEquals(8, game.openingBattle().student().hp()); assertTrue(game.openingBattle().answers().getLast().correct());
    }
    @Test void noStartWithoutImpactAndNoAnswerDuckOrDuplicateHitDuringReveal() {
        start(); var before = game.openingBattle();
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(0)));
        assertEquals(before, game.openingBattle());
        game.hitOpeningQuestion(id(0)); before = game.openingBattle();
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(0), "A"));
        assertThrows(IllegalStateException.class, game::useOpeningDuck);
        assertThrows(IllegalStateException.class, () -> game.hitOpeningQuestion(id(0)));
        assertThrows(IllegalStateException.class, () -> game.hitOpeningQuestion(id(1)));
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(1)));
        assertEquals(before, game.openingBattle());
        game.startOpeningQuestion(id(0));
        assertEquals(8, game.openingBattle().student().hp());
    }
    @Test void repeatedPresentationCompletionCannotRestartClockOrApplyDamage() {
        start(); game.hitOpeningQuestion(id(0)); clock.set(760_000_000L); game.startOpeningQuestion(id(0));
        clock.addAndGet(3_000_000_000L); var before = game.openingBattle();
        assertEquals(Duration.ofSeconds(177), before.remaining());
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(0)));
        assertThrows(IllegalStateException.class, () -> game.hitOpeningQuestion(id(0)));
        assertEquals(before, game.openingBattle());
        clock.addAndGet(177_000_000_000L); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
        assertEquals(8, game.openingBattle().student().hp()); assertTrue(game.openingBattle().answers().isEmpty());
    }
    @Test void firstDeadlineIsMeasuredFromRevealCompletionNotImpact() {
        start(); game.hitOpeningQuestion(id(0)); clock.set(760_000_000L); game.startOpeningQuestion(id(0));
        clock.set(180_000_000_000L); game.advanceOpeningBattle();
        assertFalse(game.openingBattle().finished()); assertEquals(Duration.ofMillis(760), game.openingBattle().remaining());
        clock.set(180_760_000_000L); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
    }
    @Test void pendingRevealCopiesKeepTheirQuestionButNotSharedMutation() {
        start(); game.hitOpeningQuestion(id(0));
        var other = new GameController(game.snapshot(), demo.rules(), demo.content(), new Random(42), CombatRules.pending(), clock::get);
        game.startOpeningQuestion(id(0));
        assertEquals(OpeningBattle.Phase.QUESTION_REVEAL, other.openingBattle().phase());
        clock.set(760_000_000L); other.startOpeningQuestion(id(0));
        assertEquals(Duration.ofMinutes(3), other.openingBattle().remaining());
        assertEquals(Duration.ofMillis(179240), game.openingBattle().remaining());
        assertEquals(8, other.openingBattle().student().hp());
    }
    @Test void invalidImpactAndRegressedClockNeverSpendHp() {
        start(); var before = game.openingBattle();
        assertThrows(IllegalArgumentException.class, () -> game.hitOpeningQuestion("unknown"));
        assertThrows(IllegalArgumentException.class, () -> game.hitOpeningQuestion(null));
        assertEquals(before, game.openingBattle());
        clock.set(-1); assertThrows(IllegalStateException.class, () -> game.hitOpeningQuestion(id(0)));
        clock.set(0); assertEquals(before, game.openingBattle());
        game.hitOpeningQuestion(id(0)); assertEquals(8, game.openingBattle().student().hp());
    }
    @Test void lastQuestionCanUseTheEntireUnspentBudgetRatherThanTheOldShrinkingLimit() {
        start(); duck(); for (int i = 0; i < 9; i++) correct(i);
        game.hitOpeningQuestion(id(9)); assertEquals(2, game.openingBattle().student().hp());
        clock.addAndGet(760_000_000L); game.advanceOpeningBattle(); game.startOpeningQuestion(id(9));
        assertEquals(Duration.ofMinutes(3), game.openingBattle().remaining());
        clock.addAndGet(179_999_999_999L); game.answerOpeningQuestion(id(9), "E");
        assertEquals(89, game.openingBattle().ending().grade()); assertEquals(2, game.openingBattle().student().hp());
    }
    @Test void thirtySecondsUsedOnFirstQuestionLeaves150ThroughTurnsAndReveal() {
        start(); assertEquals(Duration.ofMinutes(3), game.openingBattle().remaining());
        assertFalse(game.openingBattle().clockRunning());
        clock.addAndGet(Duration.ofHours(1).toNanos()); game.advanceOpeningBattle();
        reveal(id(0)); clock.addAndGet(Duration.ofSeconds(30).toNanos());
        game.answerOpeningQuestion(id(0), "A");
        assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining());
        clock.addAndGet(Duration.ofHours(1).toNanos()); game.advanceOpeningBattle();
        game.hitOpeningQuestion(id(1)); clock.addAndGet(Duration.ofMillis(760).toNanos()); game.advanceOpeningBattle();
        assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining()); assertFalse(game.openingBattle().clockRunning());
        game.startOpeningQuestion(id(1)); assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining());
        clock.addAndGet(Duration.ofSeconds(40).toNanos()); game.answerOpeningQuestion(id(1), "B");
        assertEquals(Duration.ofSeconds(110), game.openingBattle().remaining());
    }
    @Test void pulsesAndPureReadsDoNotChargeElapsedTimeMoreThanOnce() {
        start(); reveal(id(0));
        for (int second = 1; second <= 30; second++) {
            clock.set(second * 1_000_000_000L); game.advanceOpeningBattle(); game.advanceOpeningBattle();
            assertEquals(Duration.ofSeconds(180 - second), game.openingBattle().remaining());
            assertEquals(game.openingBattle(), game.openingBattle());
        }
        game.answerOpeningQuestion(id(0), "A"); assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining());
        game.advanceOpeningBattle(); assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining());
    }
    @Test void betweenQuestionDuckDoesNotSpendBudgetButActiveQuestionDuckDoes() {
        start(); reveal(id(0)); clock.addAndGet(30_000_000_000L); game.answerOpeningQuestion(id(0), "A");
        game.useOpeningDuck(); clock.addAndGet(5_000_000_000L); game.advanceOpeningBattle();
        assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining());
        assertEquals(11, game.openingBattle().student().hp());
        // The existing active-question duck test asserts 180 -> 175 during its same five seconds.
        reveal(id(1)); assertEquals(Duration.ofSeconds(150), game.openingBattle().remaining());
    }
    @Test void recoveryExactlyAtRemainingSharedDeadlineLosesEvenOnALaterQuestion() {
        start(); reveal(id(0)); clock.addAndGet(175_000_000_000L); game.answerOpeningQuestion(id(0), "A");
        reveal(id(1)); game.useOpeningDuck(); clock.addAndGet(5_000_000_000L); game.advanceOpeningBattle();
        var v = game.openingBattle(); assertEquals(OpeningBattle.DefeatReason.TIMEOUT, v.ending().reason());
        assertEquals(7, v.student().hp()); assertEquals(Duration.ZERO, v.remaining()); assertEquals(1, v.answers().size());
    }
    @Test void snapshotCopiesRetainSpentBudgetWithoutSharingFutureCharges() {
        start(); reveal(id(0)); clock.addAndGet(30_000_000_000L); game.answerOpeningQuestion(id(0), "A");
        var other = new GameController(game.snapshot(), demo.rules(), demo.content(), new Random(42), CombatRules.pending(), clock::get);
        reveal(id(1)); clock.addAndGet(10_000_000_000L); game.answerOpeningQuestion(id(1), "B");
        assertEquals(Duration.ofSeconds(140), game.openingBattle().remaining());
        assertEquals(Duration.ofSeconds(150), other.openingBattle().remaining());
        other.hitOpeningQuestion(id(1)); other.startOpeningQuestion(id(1));
        assertEquals(Duration.ofSeconds(150), other.openingBattle().remaining());
    }
    @Test void wrongAnswerFreezesUnusedBudgetAndEndingPulsesNeverSpendIt() {
        start(); reveal(id(0)); clock.set(30_000_000_000L); game.answerOpeningQuestion(id(0), "B");
        var ended = game.openingBattle(); assertEquals(Duration.ofSeconds(150), ended.remaining());
        assertFalse(ended.clockRunning());
        clock.addAndGet(Duration.ofDays(1).toNanos()); game.advanceOpeningBattle(); assertEquals(ended, game.openingBattle());
    }
    @Test void staleCallbacksCannotResetASharedBudgetPartlyUsedOnAnotherQuestion() {
        start(); reveal(id(0)); clock.set(30_000_000_000L); game.answerOpeningQuestion(id(0), "A");
        game.hitOpeningQuestion(id(1)); var before = game.openingBattle();
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(0)));
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(0), "A"));
        assertEquals(before, game.openingBattle());
        game.startOpeningQuestion(id(1)); clock.addAndGet(10_000_000_000L); before = game.openingBattle();
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(1)));
        assertEquals(before, game.openingBattle()); assertEquals(Duration.ofSeconds(140), before.remaining());
    }
}
