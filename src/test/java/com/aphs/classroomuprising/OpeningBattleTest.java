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
    void correct(int index) {
        String id = id(index); game.startOpeningQuestion(id);
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
    @Test void wrongAnswerCostsOneHpAndImmediatelyEndsAt67WithMocking() {
        start(); game.startOpeningQuestion(id(0)); game.answerOpeningQuestion(id(0), "B");
        var v = game.openingBattle(); assertEquals(8, v.student().hp());
        assertFalse(v.answers().getFirst().correct()); assertEquals("A", v.answers().getFirst().correctOption());
        assertEquals(OpeningBattle.DefeatReason.WRONG_ANSWER, v.ending().reason()); assertEquals(67, v.ending().grade());
        assertTrue(v.ending().dialogue().stream().anyMatch(s -> s.contains("WOHAHAHAHA")));
        assertEquals(OpeningBattle.AnimationCue.DEFEAT, v.ending().animation());
    }
    @Test void ninthUnhealedAnswerPassesOutEvenWhenCorrectAndTutorialWaiverDoesNotApply() {
        start(); for (int i = 0; i < 9; i++) correct(i);
        var v = game.openingBattle(); assertEquals(0, v.student().hp()); assertEquals(9, v.answers().size());
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
        else { game.startOpeningQuestion(id(0)); game.answerOpeningQuestion(id(0), "B"); }
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
    void everyOrdinalHasExactNinetyPercentLimitAndHardDeadline(int ordinal) {
        long[] limits = {20_000_000_000L,18_000_000_000L,16_200_000_000L,14_580_000_000L,
                13_122_000_000L,11_809_800_000L,10_628_820_000L,9_565_938_000L,8_609_344_200L,7_748_409_780L};
        start(); duck();
        for (int i = 0; i < ordinal - 1; i++) correct(i);
        game.startOpeningQuestion(id(ordinal - 1));
        assertEquals(Duration.ofNanos(limits[ordinal - 1]), game.openingBattle().timeLimit());
        clock.addAndGet(limits[ordinal - 1] - 1); game.advanceOpeningBattle(); assertFalse(game.openingBattle().finished());
        clock.incrementAndGet(); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
        assertEquals(ordinal - 1, game.openingBattle().answers().size()); // Timeout is not a submitted answer.
        assertEquals(ordinal, game.openingBattle().questionOrdinal());
    }
    @Test void answerOneNanosecondBeforeDeadlineSucceedsAndFeedbackReadingStartsNoNextTimer() {
        start(); game.startOpeningQuestion(id(0)); clock.set(19_999_999_999L);
        game.answerOpeningQuestion(id(0), "A"); assertTrue(game.openingBattle().answers().getFirst().correct());
        clock.addAndGet(Duration.ofDays(1).toNanos()); game.advanceOpeningBattle();
        assertEquals(OpeningBattle.Phase.SELECT_QUESTION, game.openingBattle().phase());
        game.startOpeningQuestion(id(1)); assertEquals(Duration.ofSeconds(18), game.openingBattle().remaining());
    }
    @Test void lateSubmissionCommitsTimeoutInsteadOfThrowingAndRollingItBack() {
        start(); game.startOpeningQuestion(id(0)); clock.set(20_000_000_000L);
        game.answerOpeningQuestion(id(0), "A"); var v = game.openingBattle();
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, v.ending().reason()); assertEquals(9, v.student().hp());
        assertTrue(v.answers().isEmpty());
    }
    @Test void playerMayChooseOrderAndCannotReturnToAnAnsweredQuestion() {
        start(); correct(9); game.startOpeningQuestion(id(0));
        assertEquals(2, game.openingBattle().questionOrdinal()); assertEquals(Duration.ofSeconds(18), game.openingBattle().timeLimit());
        game.answerOpeningQuestion(id(0), "A");
        var before = game.openingBattle(); assertThrows(IllegalArgumentException.class, () -> game.startOpeningQuestion(id(9)));
        assertEquals(before, game.openingBattle());
    }
    @Test void duckConsumesInventoryImmediatelyAndRecoversOnlyAfterFiveSecondsWithNoNineHpCap() {
        start(); game.startOpeningQuestion(id(0)); game.useOpeningDuck();
        assertEquals(0, game.openingBattle().ducksRemaining()); assertEquals(9, game.openingBattle().student().hp());
        var before = game.openingBattle();
        assertThrows(IllegalStateException.class, game::useOpeningDuck); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(0), "A"));
        clock.set(4_999_999_999L); game.advanceOpeningBattle(); assertEquals(9, game.openingBattle().student().hp());
        clock.incrementAndGet(); game.advanceOpeningBattle(); var v = game.openingBattle();
        assertEquals(12, v.student().hp()); assertEquals(Duration.ofSeconds(15), v.remaining());
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
    void hpZeroOnTenthAnswerBeatsVictoryAndWrongAnswerStillHasFeedback(boolean wrong) throws Exception {
        String raw;
        try (var in = getClass().getResourceAsStream("/demo/opening.properties")) { raw = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        var content = OpeningContent.read(new StringReader(raw.replace("duck.hp=3", "duck.hp=1")));
        var b = new OpeningBattle(content, 0); b.useDuck(0); b.tick(5_000_000_000L);
        for (int i = 0; i < 10; i++) {
            var q = content.question(id(i)); b.select(id(i), 5_000_000_000L);
            b.answer(id(i), wrong && i == 9 ? "A" : q.answer(), 5_000_000_000L);
        }
        var v = b.view(5_000_000_000L); assertEquals(0, v.student().hp());
        assertEquals(67, v.ending().grade()); assertEquals(OpeningBattle.DefeatReason.PASS_OUT, v.ending().reason());
        assertEquals(!wrong, v.answers().getLast().correct());
    }
    @ParameterizedTest @ValueSource(longs={15_000_000_000L,16_000_000_000L})
    void timeoutAtOrBeforeRecoveryWinsAndCannotBeResurrected(long useAt) {
        start(); game.startOpeningQuestion(id(0)); clock.set(useAt); game.useOpeningDuck();
        clock.set(25_000_000_000L); game.advanceOpeningBattle();
        var v = game.openingBattle(); assertEquals(9, v.student().hp()); assertFalse(v.usingDuck());
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, v.ending().reason());
        game.advanceOpeningBattle(); assertEquals(v, game.openingBattle());
        assertThrows(IllegalStateException.class, game::useOpeningDuck);
    }
    @Test void missedTimerPulseStillAppliesHealingThatFinishedBeforeTimeout() {
        start(); game.startOpeningQuestion(id(0)); clock.set(14_000_000_000L); game.useOpeningDuck();
        clock.set(25_000_000_000L); game.advanceOpeningBattle();
        assertEquals(12, game.openingBattle().student().hp());
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, game.openingBattle().ending().reason());
    }
    @Test void duckClickAtDeadlineCommitsTimeoutAndDoesNotConsumeUnusedItem() {
        start(); game.startOpeningQuestion(id(0)); clock.set(20_000_000_000L); game.useOpeningDuck();
        assertEquals(1, game.openingBattle().ducksRemaining()); assertTrue(game.openingBattle().finished());
    }
    @Test void invalidStaleAndRepeatedOperationsAreAtomicAndEndingDoesNotRepeat() {
        start(); var before = game.openingBattle();
        assertThrows(IllegalArgumentException.class, () -> game.startOpeningQuestion("missing")); assertEquals(before, game.openingBattle());
        game.startOpeningQuestion(id(0)); before = game.openingBattle();
        assertThrows(IllegalArgumentException.class, () -> game.answerOpeningQuestion(id(0), "F")); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(1), "B")); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(1))); assertEquals(before, game.openingBattle());
        game.answerOpeningQuestion(id(0), "B"); before = game.openingBattle();
        assertThrows(IllegalStateException.class, () -> game.answerOpeningQuestion(id(0), "A")); assertEquals(before, game.openingBattle());
        assertThrows(IllegalStateException.class, () -> game.startOpeningQuestion(id(1)));
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
        clock.set(Long.MAX_VALUE - 10); start(); game.startOpeningQuestion(id(0));
        clock.addAndGet(100); game.advanceOpeningBattle(); // wraps the signed long, but elapsed is still 100 ns
        var before = game.openingBattle(); clock.decrementAndGet();
        assertThrows(IllegalStateException.class, game::advanceOpeningBattle);
        clock.incrementAndGet(); assertEquals(before, game.openingBattle());
    }
}
