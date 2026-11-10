package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatRules;
import com.aphs.classroomuprising.CombatSession;
import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameController;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.LessonDecision;
import com.aphs.classroomuprising.SchoolCalendar;
import com.aphs.classroomuprising.StatChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The combat part of the frontend controller against the real core (handoff acceptance checks
 * 1-7). The Demo ships no assessment bank or combat policy, so the bank, the rules and the
 * evaluator results here are TEST FIXTURES, not game values.
 */
class GameSessionCombatTest {
    /** Fixture policy only: the step, HP curve and constant mark are not approved numbers. */
    private static final CombatRules FIXTURE = new CombatRules(.1, CombatRules.TimeoutDasher.KEEP_DOUBLE,
            CombatRules.AnswerTiming.PLAYER_SUBMISSION, true, Duration.ofSeconds(15),
            elapsed -> 2 + 1.0 / (elapsed.toSeconds() + 1), summary -> 80);

    @TempDir Path temp;
    private final AtomicLong clock = new AtomicLong(1_000);
    private DemoProfile profile;
    private GameSession session;

    @BeforeEach void toDay5() throws IOException {
        profile = DemoProfile.load();
        session = fixtureSession(FIXTURE);
    }

    @Test void cardsKeepTheirDifficultyWeightsWhateverTheAnswerOrder() {
        assertTrue(session.startCombatAssessment().ok());
        CombatSession.View start = view();
        assertEquals(List.of("easy", "mid", "hard"), ids(start), "cards follow authored difficulty, not bank order");
        List<Double> weights = weights(start);

        for (String id : List.of("hard", "easy", "mid")) {
            answer(id, 5);
            assertEquals(weights, weights(view()), "answering in another order never moves initial weights");
        }
        List<CombatSession.Answer> answers = session.combatSummary().value().answers();
        assertEquals(List.of("hard", "easy", "mid"), answers.stream().map(CombatSession.Answer::questionId).toList());
        assertEquals(List.of(false, false, true), answers.stream().map(CombatSession.Answer::dasher).toList(),
                "the Dasher goes to whichever question was answered last");
    }

    @Test void reopeningTheOpenQuestionKeepsItsClockAndOvertimeStillAnswers() {
        session.startCombatAssessment();
        assertTrue(session.startCombatQuestion("easy").ok());
        clock.addAndGet(Duration.ofSeconds(30).toNanos());
        assertTrue(session.startCombatQuestion("easy").ok(), "a repeated start for the open question is accepted");
        assertEquals(Duration.ofSeconds(30), view().elapsed(), "and does not restart the clock");
        assertFalse(view().timedOut(), "exactly 30 s is still on time");

        clock.addAndGet(1);
        assertTrue(view().timedOut());
        assertTrue(session.submitCombatAnswer().ok(), "passing 30 s does not disable answering");
    }

    @Test void aSubmittedAnswerIsLockedAndOtherQuestionsWait() {
        session.startCombatAssessment();
        session.startCombatQuestion("easy");
        assertTrue(session.submitCombatAnswer().ok());
        assertInstanceOf(Outcome.Rejected.class, session.submitCombatAnswer().failure());
        Outcome.Pending switching = assertInstanceOf(Outcome.Pending.class, session.startCombatQuestion("mid"));
        assertEquals("T07", switching.ruleId());
        assertEquals(CombatSession.QuestionStatus.SUBMITTED, status(view(), "easy"));
        assertEquals(CombatSession.QuestionStatus.AVAILABLE, status(view(), "mid"));
    }

    @Test void repeatedAndOtherRunTicketsChangeNothing() throws IOException {
        session.startCombatAssessment();
        session.startCombatQuestion("easy");
        CombatSession.AnswerTicket ticket = session.submitCombatAnswer().value();
        assertTrue(session.recordCombatEvaluation(ticket, 1, "fixture evaluator").ok());
        String after = signature(session.view());
        assertInstanceOf(Outcome.Rejected.class, session.recordCombatEvaluation(ticket, 1, "fixture evaluator"));
        assertEquals(after, signature(session.view()), "a duplicate result is not applied twice");

        GameSession other = fixtureSession(FIXTURE);
        other.startCombatAssessment();
        other.startCombatQuestion("mid");
        CombatSession.AnswerTicket foreign = other.submitCombatAnswer().value();
        session.startCombatQuestion("mid");
        session.submitCombatAnswer();
        assertInstanceOf(Outcome.Rejected.class, session.recordCombatEvaluation(foreign, 1, "fixture evaluator"));
        assertEquals(CombatSession.QuestionStatus.SUBMITTED, status(view(), "mid"));
    }

    @Test void hpAfterAnEvaluationIsTheCoresOwnValue() {
        session.startCombatAssessment();
        double before = session.view().student().hp();
        session.startCombatQuestion("mid");
        clock.addAndGet(Duration.ofSeconds(4).toNanos());
        CombatSession.AnswerTicket ticket = session.submitCombatAnswer().value();
        Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.recordCombatEvaluation(ticket, 0.5, "fixture"));
        CombatSession.Answer answer = view().answers().getFirst();
        assertEquals(session.view().student().hp(), done.after().student().hp());
        assertEquals(before - answer.appliedHpCost(), session.view().student().hp(), 1e-9);
        assertTrue(answer.appliedHpCost() > 0);
    }

    @Test void rewardDelaysRunOnTheBackendClockAndRefuseEarlyOrOverlappingUse() {
        session.startCombatAssessment();
        session.startCombatQuestion("easy");
        CombatSession.RewardTicket safe = session.beginRewardDelay(CombatSession.RewardKind.SAFE).value();
        assertEquals(CombatRules.SAFE_REWARD_DURATION, safe.duration());
        assertInstanceOf(Outcome.Rejected.class, session.beginRewardDelay(CombatSession.RewardKind.RISKY).failure());
        assertInstanceOf(Outcome.Rejected.class, session.submitCombatAnswer().failure());

        clock.addAndGet(Duration.ofSeconds(4).toNanos());
        assertEquals(Duration.ofSeconds(1), view().rewardRemaining());
        assertInstanceOf(Outcome.Rejected.class, session.finishRewardDelay(safe), "early completion is refused");
        clock.addAndGet(Duration.ofSeconds(1).toNanos());
        assertTrue(session.finishRewardDelay(safe).ok());
        assertInstanceOf(Outcome.Rejected.class, session.finishRewardDelay(safe), "an old ticket cannot finish again");

        CombatSession.RewardTicket risky = session.beginRewardDelay(CombatSession.RewardKind.RISKY).value();
        assertEquals(FIXTURE.riskyRewardDuration(), risky.duration());
    }

    @Test void pendingCombatPolicyIsReportedWithItsRuleAndNothingChanges() throws IOException {
        GameSession pending = fixtureSession(CombatRules.pending());
        String before = signature(pending.view());
        Outcome.Pending start = assertInstanceOf(Outcome.Pending.class, pending.startCombatAssessment().failure());
        assertEquals("T07", start.ruleId());
        assertEquals(before, signature(pending.view()));
        assertFalse(pending.view().hasActiveCombat());

        GameSession noHp = fixtureSession(new CombatRules(.1, CombatRules.TimeoutDasher.KEEP_DOUBLE,
                CombatRules.AnswerTiming.PLAYER_SUBMISSION, null, null, null, null));
        noHp.startCombatAssessment();
        noHp.startCombatQuestion("easy");
        Outcome.Pending reward = assertInstanceOf(Outcome.Pending.class,
                noHp.beginRewardDelay(CombatSession.RewardKind.SAFE).failure());
        assertEquals("T09", reward.ruleId());
        CombatSession.AnswerTicket ticket = noHp.submitCombatAnswer().value();
        double hp = noHp.view().student().hp();
        Outcome.Pending damage = assertInstanceOf(Outcome.Pending.class, noHp.recordCombatEvaluation(ticket, 1, "fixture"));
        assertEquals("T01", damage.ruleId());
        assertEquals(hp, noHp.view().student().hp());
        assertEquals(ticket, noHp.combatView().value().pendingAnswer(), "the ticket stays pending for a retry");
    }

    @Test void missingConversionLeavesTheCombatOpen() throws IOException {
        GameSession noConverter = fixtureSession(new CombatRules(.1, CombatRules.TimeoutDasher.KEEP_DOUBLE,
                CombatRules.AnswerTiming.PLAYER_SUBMISSION, true, null, FIXTURE.hpCost(), null));
        noConverter.startCombatAssessment();
        for (String id : List.of("easy", "mid", "hard")) {
            noConverter.startCombatQuestion(id);
            noConverter.recordCombatEvaluation(noConverter.submitCombatAnswer().value(), 1, "fixture");
        }
        assertTrue(noConverter.combatSummary().ok());
        Outcome.Pending settle = assertInstanceOf(Outcome.Pending.class, noConverter.finishCombatAssessment());
        assertEquals("T06", settle.ruleId());
        assertTrue(noConverter.view().hasActiveCombat());
    }

    @Test void ordinarySettlementReturnsInThreeDaysAndOnlyTheDayEndSaves() throws IOException {
        session.startCombatAssessment();
        assertInstanceOf(Outcome.Rejected.class, session.combatSummary().failure(), "no summary before all answers");
        for (String id : List.of("easy", "mid", "hard")) answer(id, 3);
        assertInstanceOf(Outcome.Rejected.class, session.saveDayEnd(), "no mid-combat save");

        assertTrue(session.finishCombatAssessment().ok());
        GameState settled = session.view();
        assertFalse(settled.hasActiveCombat());
        assertEquals(DayPeriod.OTHER_CLASSES, settled.period());
        var record = settled.grades().records().getFirst();
        assertEquals(settled.day() + 3, record.returnDay());
        assertTrue(record.officialScore().isEmpty());
        assertInstanceOf(Outcome.Rejected.class, session.combatView().failure(), "the combat is closed");

        session.otherClasses(false);
        session.sleepAfterSchool();
        assertTrue(session.saveDayEnd().ok());
        GameSession loaded = GameSession.load(profile, session.savePath(), new Random(1), FIXTURE);
        assertFalse(loaded.view().hasActiveCombat());
        assertEquals(signature(session.view()), signature(loaded.view()));
    }

    // ---- fixtures -------------------------------------------------------------------

    /** A run at Day 5 (the first Lab) with a fixture bank, given in shuffled difficulty order. */
    private GameSession fixtureSession(CombatRules rules) throws IOException {
        var lessons = new LinkedHashMap<Integer, ContentCatalog.Lesson>();
        for (int day = 1; day <= 4; day++) lessons.put(day, profile.content().lesson(day));
        var hub = new LinkedHashMap<String, StatChange>();
        for (String id : profile.content().hubActivityIds()) hub.put(id, profile.content().hub(id));
        var bank = List.of(new ContentCatalog.Question("mid", "Fixture question B", 2),
                new ContentCatalog.Question("hard", "Fixture question C", 3),
                new ContentCatalog.Question("easy", "Fixture question A", 1));
        var content = new ContentCatalog(lessons, hub, Map.of(), Map.of(SchoolCalendar.day(5).assessmentId(), bank),
                Map.of(), profile.content().otherRevision());
        var game = new GameController(new GameState("Tester", profile.initialStudent()), profile.rules(), content,
                new Random(3), rules, clock::get);
        GameSession fixture = GameSession.over(profile, game, temp.resolve("slot-" + System.nanoTime() + ".properties"));
        assertTrue(fixture.completeOpening().ok());
        for (int day = 1; day <= 4; day++) {
            assertTrue(fixture.openLesson().ok());
            // The lesson's authored appropriate response: the learning the old "ask" fixture step stood for.
            String response = lessons.get(day).decision().responses().stream()
                    .filter(o -> o.outcome() == LessonDecision.Outcome.APPROPRIATE).findFirst().orElseThrow().id();
            assertTrue(fixture.lessonChoice(day, response).ok());
            assertTrue(fixture.otherClasses(true).ok());
            assertTrue(fixture.hubActivity("study").ok());
            assertTrue(fixture.sleepNormally().ok());
            assertTrue(fixture.beginNextDay().ok());
        }
        assertEquals(5, fixture.view().day());
        assertEquals(DayPeriod.ICS_CLASS, fixture.view().period());
        return fixture;
    }

    private void answer(String id, int seconds) {
        assertTrue(session.startCombatQuestion(id).ok());
        clock.addAndGet(Duration.ofSeconds(seconds).toNanos());
        CombatSession.AnswerTicket ticket = session.submitCombatAnswer().value();
        assertTrue(session.recordCombatEvaluation(ticket, 1, "fixture evaluator").ok());
    }

    private CombatSession.View view() { return session.combatView().value(); }

    private static List<String> ids(CombatSession.View view) {
        return view.questions().stream().map(CombatSession.QuestionView::id).toList();
    }

    private static List<Double> weights(CombatSession.View view) {
        return view.questions().stream().map(CombatSession.QuestionView::initialWeight).toList();
    }

    private static CombatSession.QuestionStatus status(CombatSession.View view, String id) {
        return view.questions().stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow().status();
    }

    private static String signature(GameState s) {
        return String.join("|", List.of(String.valueOf(s.day()), s.period().name(), s.student().toString(),
                String.valueOf(s.effectiveSan()), s.effects().toString(), String.valueOf(s.hasActiveAssessment()),
                String.valueOf(s.hasActiveCombat()), records(s), s.status().name()));
    }

    private static String records(GameState s) {
        return s.grades().records().stream().map(r -> r.id() + "/" + r.type() + "/" + r.submittedDay() + "/"
                + r.returnDay() + "/" + r.questionIds() + "/" + r.makeup() + "/" + r.officialScore()).toList().toString();
    }
}
