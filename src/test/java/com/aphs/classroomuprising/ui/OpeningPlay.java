package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatRules;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameController;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.OpeningBattle;
import com.aphs.classroomuprising.OpeningContent;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test-only ways through the opening exam, using only the frontend adapter and real backend
 * transitions (no skip, no answer bypass). Later-day tests need the opening finished first,
 * because the core no longer accepts the acknowledgement before the exam has ended.
 */
final class OpeningPlay {
    private OpeningPlay() {}

    /**
     * Plays the exam to an ending the way the screen does (hit, then start once the paper is open,
     * then answer "A"), in authored order; it ends at the first wrong answer, or at the lethal hit
     * at the latest. Then acknowledges it: the session is on Day 1.
     */
    static Outcome.Done enterDayOne(GameSession session) {
        assertTrue(session.startOpeningBattle().ok());
        for (int i = 0; i < 10 && !view(session).finished(); i++) {
            String id = view(session).availableQuestions().getFirst().id();
            if (!hitAndStart(session, id)) break;
            assertTrue(session.answerOpeningQuestion(id, "A").ok());
        }
        assertTrue(view(session).finished(), "the exam has an ending");
        return assertInstanceOf(Outcome.Done.class, session.completeOpening());
    }

    /** The question's bullet lands, then its paper opens: false when the hit ended the exam. */
    static boolean hitAndStart(GameSession session, String id) {
        assertTrue(session.hitOpeningQuestion(id).ok());
        if (view(session).finished()) return false;
        assertTrue(session.startOpeningQuestion(id).ok());
        return true;
    }

    static OpeningBattle.View view(GameSession session) {
        Fetch<OpeningBattle.View> fetch = session.openingBattle();
        assertTrue(fetch.ok(), () -> "no exam view: " + fetch.failure());
        return fetch.value();
    }

    /**
     * Each question's correct option, learned the way a player could: answer it first in a fresh
     * exam and read the feedback. The keys are never exposed before an answer.
     */
    static Map<String, String> keys(DemoProfile profile, Path dir) {
        var keys = new LinkedHashMap<String, String>();
        for (OpeningContent.QuestionView question : profile.content().opening().questions()) {
            GameSession probe = GameSession.newGame(profile, "Probe", new Random(1), dir.resolve("probe.properties"));
            assertTrue(probe.startOpeningBattle().ok());
            assertTrue(hitAndStart(probe, question.id()));
            assertTrue(probe.answerOpeningQuestion(question.id(), "A").ok());
            keys.put(question.id(), view(probe).answers().getLast().correctOption());
        }
        return keys;
    }

    /** A new game whose core reads this clock (nanoseconds), so a test can move time on. */
    static GameSession withClock(DemoProfile profile, AtomicLong nanos, Path save) {
        GameController game = new GameController(new GameState("Tester", profile.initialStudent()), profile.rules(),
                profile.content(), new Random(1), CombatRules.pending(), nanos::get);
        return GameSession.over(profile, game, save);
    }
}
