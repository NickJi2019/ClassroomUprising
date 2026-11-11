package com.aphs.classroomuprising.contract;

import com.aphs.classroomuprising.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Headless consumer of the public Claude handoff API, not a UI/animation test. */
class OpeningFrontendContractTest {
    @Test void publicOperationsCanDriveIntroQuestionsItemEndingAndDayOne() throws Exception {
        var demo = DemoProfile.load(); var clock = new AtomicLong();
        var game = new GameController(new GameState("Player", demo.initialStudent()), demo.rules(), demo.content(),
                new Random(42), CombatRules.pending(), clock::get);
        assertFalse(game.openingIntro().isEmpty()); game.startOpeningBattle();
        var answers = Map.of("csa2014-q15","A","csa2014-q22","B","csa2014-q23","B","csa2014-q39","D",
                "csa2014-q40","C","csa2015-q20","E","csa2015-q26","E","csa2015-q31","E","csa2015-q37","D","csa2015-q38","E");
        var questions = game.openingBattle().availableQuestions();
        Duration expectedLeft = Duration.ofMinutes(3);
        for (var q : questions) {
            double hpBefore = game.openingBattle().student().hp();
            game.hitOpeningQuestion(q.id());
            assertEquals(hpBefore - 1, game.openingBattle().student().hp());
            assertEquals(OpeningBattle.Phase.QUESTION_REVEAL, game.openingBattle().phase());
            clock.addAndGet(Duration.ofMillis(760).toNanos());
            game.advanceOpeningBattle();
            expectedLeft = expectedLeft.minusMillis(760);
            assertEquals(expectedLeft, game.openingBattle().remaining());
            assertTrue(game.openingBattle().clockRunning());
            game.startOpeningQuestion(q.id());
            assertEquals(Duration.ofMinutes(3), game.openingBattle().timeLimit());
            assertEquals(expectedLeft, game.openingBattle().remaining());
            assertTrue(game.openingBattle().clockRunning());
            if (game.openingBattle().questionOrdinal() == 1) {
                game.useOpeningDuck(); clock.addAndGet(Duration.ofSeconds(5).toNanos()); game.advanceOpeningBattle();
                assertEquals(11, game.openingBattle().student().hp());
                expectedLeft = expectedLeft.minusSeconds(5);
            }
            clock.addAndGet(Duration.ofSeconds(10).toNanos());
            expectedLeft = expectedLeft.minusSeconds(10);
            double hpAfterHit = game.openingBattle().student().hp();
            game.answerOpeningQuestion(q.id(), answers.get(q.id()));
            assertEquals(hpAfterHit, game.openingBattle().student().hp());
            assertEquals(expectedLeft, game.openingBattle().remaining());
            assertEquals(!game.openingBattle().finished(), game.openingBattle().clockRunning());
            var feedback = game.openingBattle().answers().getLast(); assertTrue(feedback.correct());
            assertEquals(q.id(), feedback.questionId()); assertFalse(feedback.explanation().isBlank());
        }
        assertEquals(Duration.ofMillis(67400), expectedLeft);
        var ending = game.openingBattle().ending();
        assertEquals(OpeningBattle.AnimationCue.VICTORY, ending.animation()); assertEquals(89, ending.grade());
        assertFalse(ending.dialogue().isEmpty()); assertFalse(game.snapshot().openingCompleted());
        // A real frontend plays ending.animation/dialogue here, then acknowledges it once.
        game.completeOpening(); assertTrue(game.snapshot().openingCompleted()); assertEquals(1, game.snapshot().day());
        assertEquals(demo.initialStudent(), game.snapshot().student()); assertTrue(game.showLesson().title().contains("Review"));
    }
}
