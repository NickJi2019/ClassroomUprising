package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.OpeningBattle;
import com.aphs.classroomuprising.OpeningContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The opening exam through the frontend adapter ({@link GameSession}), the way
 * {@link OpeningScreen} drives it: the screen sends question and option IDs and the duck, calls
 * the advance pulse, and reads the view. The rules themselves are the backend's (its own tests
 * cover them); these check that the adapter exposes them and that both endings reach Day 1.
 */
class OpeningFlowTest {
    @TempDir static Path temp;
    private static DemoProfile profile;
    private static Map<String, String> keys;

    @BeforeAll static void load() throws IOException {
        profile = DemoProfile.load();
        keys = OpeningPlay.keys(profile, temp);
    }

    private GameSession fresh() {
        return GameSession.newGame(profile, "Tester", new Random(7), temp.resolve("save-" + System.nanoTime() + ".properties"));
    }

    private static String wrong(String questionId) {
        return List.of("A", "B", "C", "D", "E").stream().filter(o -> !o.equals(keys.get(questionId))).findFirst().orElseThrow();
    }

    /** The next question in authored order hits, opens and is answered correctly (false: the hit was lethal). */
    private static boolean right(GameSession session) {
        String id = OpeningPlay.view(session).availableQuestions().getFirst().id();
        if (!OpeningPlay.hitAndStart(session, id)) return false;
        assertTrue(session.answerOpeningQuestion(id, keys.get(id)).ok());
        return true;
    }

    @Test void theExamComesBeforeDayOneAndStartsFromItsOwnStats() {
        GameSession session = OpeningPlay.withClock(profile, new AtomicLong(), temp.resolve("initial.properties"));
        assertTrue(session.hasOpeningExam());
        assertFalse(session.openingIntro().isEmpty());
        assertFalse(session.openingBattle().ok(), "no exam before the intro has been shown");
        assertInstanceOf(Outcome.Rejected.class, session.completeOpening(), "no Day 1 before an ending");

        assertTrue(session.startOpeningBattle().ok());
        OpeningBattle.View view = OpeningPlay.view(session);
        OpeningContent content = session.openingContent();
        assertEquals(OpeningBattle.Phase.SELECT_QUESTION, view.phase());
        assertEquals(content.initialStudent(), view.student(), "the HUD shows the exam's student, not Day 1's");
        assertEquals(10, view.availableQuestions().size());
        assertEquals(1, view.ducksRemaining());
        assertEquals(Duration.ofSeconds(180), view.timeLimit(), "one exam time for all ten questions");
        assertEquals(Duration.ofSeconds(180), view.remaining(), "all of it left, shown before any question");
        assertTrue(view.clockRunning(), "the exam clock starts with the battle");
        for (OpeningContent.QuestionView q : view.availableQuestions()) {
            assertEquals(List.of("A", "B", "C", "D", "E"), q.options().stream().map(OpeningContent.Option::id).toList());
            assertFalse(q.code().isBlank(), q.id() + " has its code");
        }
        assertInstanceOf(Outcome.Rejected.class, session.startOpeningBattle(), "only once");
    }

    @Test void aWrongAnswerIsADefeatAndDayOneStartsFresh() {
        GameSession session = fresh();
        session.startOpeningBattle();
        String id = OpeningPlay.view(session).availableQuestions().getFirst().id();   // authored order
        String other = OpeningPlay.view(session).availableQuestions().get(1).id();
        assertInstanceOf(Outcome.Rejected.class, session.startOpeningQuestion(id), "no answering before the hit");
        assertTrue(session.hitOpeningQuestion(id).ok());
        OpeningBattle.View revealing = OpeningPlay.view(session);
        assertEquals(OpeningBattle.Phase.QUESTION_REVEAL, revealing.phase());
        assertEquals(8, revealing.student().hp(), 1e-9, "the hit costs the HP");
        assertTrue(revealing.clockRunning(), "the exam clock runs while the paper opens");
        assertInstanceOf(Outcome.Rejected.class, session.answerOpeningQuestion(id, keys.get(id)), "nothing to answer before it is open");
        assertInstanceOf(Outcome.Rejected.class, session.hitOpeningQuestion(other), "one question at a time");
        assertInstanceOf(Outcome.Rejected.class, session.startOpeningQuestion(other), "only the impacted question may become answerable");
        assertTrue(session.startOpeningQuestion(id).ok());
        assertInstanceOf(Outcome.Rejected.class, session.startOpeningQuestion(id), "a repeated start does nothing");
        assertTrue(session.answerOpeningQuestion(id, wrong(id)).ok());

        OpeningBattle.View view = OpeningPlay.view(session);
        assertEquals(8, view.student().hp(), 1e-9, "answering costs nothing");
        OpeningBattle.Feedback feedback = view.answers().getLast();
        assertFalse(feedback.correct());
        assertEquals(keys.get(id), feedback.correctOption());
        assertFalse(feedback.explanation().isBlank());
        assertEquals(OpeningBattle.AnimationCue.DEFEAT, view.ending().animation());
        assertEquals(OpeningBattle.DefeatReason.WRONG_ANSWER, view.ending().reason());
        assertEquals(67, view.ending().grade());
        assertFalse(view.ending().dialogue().isEmpty());
        assertInstanceOf(Outcome.Rejected.class, session.answerOpeningQuestion(id, keys.get(id)), "input is over");

        Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.completeOpening());
        assertEquals(1, done.after().day());
        assertEquals(DayPeriod.ICS_CLASS, done.after().period());
        assertEquals(profile.initialStudent(), done.after().student(), "last semester's HP/SAN/XP stay behind");
        assertTrue(done.after().grades().records().isEmpty(), "67 is not this summer's grade");
        assertInstanceOf(Outcome.Rejected.class, session.completeOpening());
    }

    @Test void withoutTheDuckTheNinthHitIsLethalAfterEightRightAnswers() {
        GameSession session = fresh();
        session.startOpeningBattle();
        for (int i = 0; i < 8; i++) assertTrue(right(session), "hit " + (i + 1) + " is survivable");
        assertFalse(right(session), "the ninth hit takes the last HP");
        OpeningBattle.View view = OpeningPlay.view(session);
        assertEquals(0, view.student().hp(), 1e-9);
        assertEquals(8, view.answers().size(), "eight answers; the ninth question was never opened");
        assertTrue(view.answers().stream().allMatch(OpeningBattle.Feedback::correct));
        assertEquals(OpeningBattle.AnimationCue.PASS_OUT, view.ending().animation());
        assertInstanceOf(Outcome.Rejected.class, session.startOpeningQuestion(view.activeQuestionId()), "no answering after a lethal hit");
        assertEquals(67, view.ending().grade());
        assertTrue(session.completeOpening().ok());
    }

    @Test void theDuckHealsOnTheClockAndTenRightAnswersWin() {
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        GameSession session = OpeningPlay.withClock(profile, clock, temp.resolve("duck.properties"));
        session.startOpeningBattle();
        double start = OpeningPlay.view(session).student().hp();
        assertTrue(session.useOpeningDuck().ok());
        OpeningBattle.View working = OpeningPlay.view(session);
        assertTrue(working.usingDuck());
        assertEquals(0, working.ducksRemaining(), "the one duck is used at once");
        String first = working.availableQuestions().getFirst().id();
        assertInstanceOf(Outcome.Rejected.class, session.hitOpeningQuestion(first), "no attack while it works");

        clock.addAndGet(session.openingContent().duckDuration().toNanos() - 1);
        session.advanceOpeningBattle();
        assertEquals(start, OpeningPlay.view(session).student().hp(), 1e-9, "not before its time");
        clock.addAndGet(1);
        session.advanceOpeningBattle();
        OpeningBattle.View healed = OpeningPlay.view(session);
        assertFalse(healed.usingDuck());
        assertEquals(start + session.openingContent().duckRecovery(), healed.student().hp(), 1e-9, "no cap on HP");
        assertInstanceOf(Outcome.Rejected.class, session.useOpeningDuck(), "only one duck");
        assertEquals(Duration.ofSeconds(175), healed.remaining(), "a duck between questions spends exam time too");

        for (int i = 0; i < 10; i++) assertTrue(right(session));
        OpeningBattle.View view = OpeningPlay.view(session);
        assertEquals(OpeningBattle.AnimationCue.VICTORY, view.ending().animation());
        assertEquals(89, view.ending().grade());
        assertEquals(start + session.openingContent().duckRecovery() - 10, view.student().hp(), 1e-9);
        assertTrue(session.completeOpening().ok());
    }

    @Test void theTenQuestionsShareOneExamTimeThatRunsContinuously() {
        AtomicLong clock = new AtomicLong(5_000_000_000L);
        GameSession session = OpeningPlay.withClock(profile, clock, temp.resolve("late.properties"));
        session.startOpeningBattle();
        String q1 = OpeningPlay.view(session).availableQuestions().getFirst().id();
        assertTrue(session.hitOpeningQuestion(q1).ok());
        clock.addAndGet(760_000_000L);   // the paper opening consumes exam time
        assertEquals(Duration.ofMillis(179240), OpeningPlay.view(session).remaining());
        assertTrue(session.startOpeningQuestion(q1).ok());
        assertTrue(OpeningPlay.view(session).clockRunning());
        clock.addAndGet(30_000_000_000L);
        assertTrue(session.answerOpeningQuestion(q1, keys.get(q1)).ok());
        assertEquals(Duration.ofMillis(149240), OpeningPlay.view(session).remaining(), "30 s answering plus 0.76 s revealing");

        clock.addAndGet(12_000_000_000L);   // the turn, the log, the attack: still running
        String q2 = OpeningPlay.view(session).availableQuestions().getFirst().id();
        assertTrue(session.hitOpeningQuestion(q2).ok());
        clock.addAndGet(760_000_000L);
        assertTrue(session.startOpeningQuestion(q2).ok());
        assertEquals(Duration.ofMillis(136480), OpeningPlay.view(session).remaining(), "Q2 includes the turn and reveal time too");

        clock.addAndGet(Duration.ofMillis(136480).toNanos());
        assertEquals(Duration.ZERO, OpeningPlay.view(session).remaining());
        assertFalse(OpeningPlay.view(session).finished(), "reading the view alone settles nothing");
        assertTrue(session.advanceOpeningBattle().ok());
        OpeningBattle.View view = OpeningPlay.view(session);
        assertEquals(OpeningBattle.DefeatReason.TIMEOUT, view.ending().reason());
        assertEquals(1, view.answers().size(), "a timeout adds no answer");
        assertEquals(7, view.student().hp(), 1e-9, "the two hits stay paid");
        assertTrue(session.advanceOpeningBattle().ok(), "later pulses change nothing");
        assertEquals(view.ending(), OpeningPlay.view(session).ending());
    }

    @Test void aDuckDuringAnOpenQuestionSpendsExamTime() {
        AtomicLong clock = new AtomicLong(9_000_000_000L);
        GameSession session = OpeningPlay.withClock(profile, clock, temp.resolve("duck-clock.properties"));
        session.startOpeningBattle();
        String q1 = OpeningPlay.view(session).availableQuestions().getFirst().id();
        assertTrue(session.hitOpeningQuestion(q1).ok());
        assertInstanceOf(Outcome.Rejected.class, session.useOpeningDuck(), "not while the paper opens");
        assertTrue(session.startOpeningQuestion(q1).ok());
        assertTrue(session.useOpeningDuck().ok());
        clock.addAndGet(session.openingContent().duckDuration().toNanos());
        session.advanceOpeningBattle();
        assertTrue(session.answerOpeningQuestion(q1, keys.get(q1)).ok());
        assertEquals(Duration.ofSeconds(175), OpeningPlay.view(session).remaining(), "the duck's 5 s ran on the exam clock");
    }

    @Test void theScreenShowsHonestClocksAndHangsTheBulletsOnAnArc() {
        assertEquals("180.0", OpeningScreen.seconds(OpeningBattle.totalTimeLimit()));
        assertEquals("7.7", OpeningScreen.seconds(Duration.ofMillis(7748)), "rounded down, never more time than there is");
        assertEquals("0.0", OpeningScreen.seconds(Duration.ofMillis(99)));
        for (OpeningBattle.DefeatReason reason : OpeningBattle.DefeatReason.values())
            assertFalse(OpeningScreen.reason(reason).isBlank());
        // Ten bullets hang in front of him (to his left), first at the top, in a gentle arc.
        javafx.geometry.Point2D origin = new javafx.geometry.Point2D(947, 330);
        double lastY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 10; i++) {
            javafx.geometry.Point2D slot = Danmaku.slot(i, 10, origin);
            assertTrue(slot.getX() < origin.getX(), "in front of him: " + slot);
            assertTrue(slot.getY() > lastY, "top to bottom in firing order");
            assertEquals(235, slot.distance(origin), 1e-6);
            lastY = slot.getY();
        }
    }
}
