package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.LessonDecision;
import com.aphs.classroomuprising.StudentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two-level classroom decision through the frontend controller, against the real core and
 * the Demo's authored lessons. Labels, effects and feedback are read from the profile.
 */
class LessonDecisionFlowTest {
    @TempDir Path temp;
    private DemoProfile profile;

    @BeforeEach void load() throws IOException { profile = DemoProfile.load(); }

    private GameSession fresh() {
        return GameSession.newGame(profile, "Tester", new Random(7), temp.resolve("save-" + System.nanoTime() + ".properties"));
    }

    private ContentCatalog.Lesson lesson(int day) { return profile.content().lesson(day); }

    private List<LessonDecision.Option> options(int day, LessonDecision.Outcome kind) {
        return lesson(day).decision().options().stream().filter(o -> o.outcome() == kind).toList();
    }

    /** A session in ICS class on {@code day} with the lecture shown, earlier days spent quietly. */
    private GameSession inClass(int day) {
        GameSession session = fresh();
        assertTrue(session.completeOpening().ok());
        for (int d = 1; d < day; d++) {
            assertTrue(session.openLesson().ok());
            assertTrue(session.lessonChoice(d, lesson(d).decision().skip().id()).ok());
            assertTrue(session.otherClasses(false).ok());
            assertTrue(session.hubActivity("rest").ok());
            assertTrue(session.sleepNormally().ok());
            assertTrue(session.beginNextDay().ok());
        }
        assertEquals(day, session.view().day());
        assertTrue(session.openLesson().ok());
        return session;
    }

    @Test void dayOneAnswersMrRewardAndDayTwoAsksHim() {
        List<LessonMenu.Entry> one = LessonMenu.first(lesson(1));
        assertEquals("RAISE YOUR HAND", one.get(0).label());
        assertTrue(one.get(0).opensResponses(), "taking part only opens the responses");
        assertEquals(lesson(1).decision().skip(), one.get(1).option());
        assertEquals("ASK A QUESTION", LessonMenu.first(lesson(2)).get(0).label());

        assertTrue(LessonMenu.spoken(lesson(1).decision()), "Mr. Reward asks the class: his line, in the dialogue box");
        assertEquals(UiText.responsePrompt(ContentCatalog.Interaction.RAISE_HAND), LessonMenu.responsePrompt(lesson(1)));
        assertFalse(LessonMenu.spoken(lesson(2).decision()));
        assertEquals(lesson(2).decision().prompt(), LessonMenu.responsePrompt(lesson(2)));
    }

    @Test void eachResponseMenuShowsTheThreeAuthoredLabelsInOrder() {
        for (int day : profile.content().lessonDays()) {
            LessonDecision decision = lesson(day).decision();
            List<LessonMenu.Entry> menu = LessonMenu.responses(decision);
            assertEquals(3, menu.size(), "day " + day);
            assertEquals(decision.responses().stream().map(LessonDecision.Option::label).toList(),
                    menu.stream().map(LessonMenu.Entry::label).toList(), "authored order and wording, day " + day);
            for (LessonMenu.Entry entry : menu) {
                assertNotEquals(entry.option().id(), entry.label(), "an ID is never shown");
                assertFalse(entry.opensResponses());
            }
        }
    }

    @Test void openingAndLeavingTheMenusChangesNothing() {
        GameSession session = inClass(1);
        String before = signature(session.view());
        for (int i = 0; i < 3; i++) {   // what the screen builds as the player opens and backs out
            LessonMenu.first(lesson(1));
            LessonMenu.responses(lesson(1).decision());
            LessonMenu.responsePrompt(lesson(1));
        }
        assertEquals(before, signature(session.view()));
        assertEquals(DayPeriod.ICS_CLASS, session.view().period(), "no time is spent");
    }

    @Test void theAppropriateResponseGrantsItsBonusOnce() {
        GameSession session = inClass(1);
        LessonDecision.Option right = options(1, LessonDecision.Outcome.APPROPRIATE).get(0);
        GameState before = session.view();
        assertEquals(1.0, before.xpMultiplier(), 1e-9, "the first day has no XP modifier");

        Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.lessonChoice(1, right.id()));
        StudentState a = before.student(), b = done.after().student();
        assertEquals(right.effect().codingVibe(), b.codingVibe() - a.codingVibe(), 1e-9);
        for (int u = 0; u < a.unitKnowledge().size(); u++)
            assertEquals(right.effect().unitKnowledge().get(u), b.unitKnowledge().get(u) - a.unitKnowledge().get(u), 1e-9);
        assertEquals(a.san(), b.san(), 1e-9, "Day 1 is tutorial: its negative part only warns");
        assertEquals(right.feedback(), done.message());
        assertEquals(DayPeriod.OTHER_CLASSES, done.after().period());

        String settled = signature(session.view());
        assertInstanceOf(Outcome.Rejected.class, session.lessonChoice(1, right.id()), "a second click");
        assertEquals(settled, signature(session.view()));
    }

    @Test void wrongTutorialResponsesOnlyWarn() {
        for (int day = 1; day <= 2; day++) {
            for (LessonDecision.Option wrong : options(day, LessonDecision.Outcome.INAPPROPRIATE)) {
                GameSession session = inClass(day);
                StudentState before = session.view().student();
                Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.lessonChoice(day, wrong.id()));
                assertEquals(before, done.after().student(), "day " + day + " " + wrong.id() + ": no bonus, no SAN loss");
                assertFalse(done.warnings().isEmpty(), "the waived SAN loss is reported as a warning");
                assertEquals(wrong.feedback(), done.message());
                assertEquals(DayPeriod.OTHER_CLASSES, done.after().period(), "the class still ends once");
            }
        }
    }

    @Test void wrongResponsesAfterTheTutorialCostTheirSanAndGrantNothing() {
        LessonDecision.Option wrong = options(3, LessonDecision.Outcome.INAPPROPRIATE).get(0);
        GameSession session = inClass(3);
        StudentState before = session.view().student();
        Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.lessonChoice(3, wrong.id()));
        StudentState after = done.after().student();
        assertEquals(before.san() + wrong.effect().san(), after.san(), 1e-9);
        assertTrue(wrong.effect().san() < 0);
        assertEquals(before.codingVibe(), after.codingVibe(), 1e-9);
        assertEquals(before.unitKnowledge(), after.unitKnowledge());
        assertTrue(done.warnings().isEmpty());
    }

    @Test void stayingQuietChangesNothingAndEndsTheClass() {
        GameSession session = inClass(1);
        LessonDecision.Option quiet = lesson(1).decision().skip();
        StudentState before = session.view().student();
        Outcome.Done done = assertInstanceOf(Outcome.Done.class, session.lessonChoice(1, quiet.id()));
        assertEquals(before, done.after().student());
        assertTrue(done.warnings().isEmpty());
        assertEquals(quiet.feedback(), done.message());
        assertEquals(DayPeriod.OTHER_CLASSES, done.after().period());
    }

    @Test void staleDaysMalformedIdsAndRepeatsNeverSettle() {
        GameSession session = inClass(1);
        String before = signature(session.view());
        String right = options(1, LessonDecision.Outcome.APPROPRIATE).get(0).id();
        String otherDays = options(2, LessonDecision.Outcome.APPROPRIATE).get(0).id();
        for (String bad : Arrays.asList("ask", "raise", "", " ", null, "no-such-option", otherDays))
            assertInstanceOf(Outcome.Rejected.class, session.lessonChoice(1, bad), "id " + bad);
        assertInstanceOf(Outcome.Rejected.class, session.lessonChoice(2, right), "a callback naming another day");
        assertInstanceOf(Outcome.Rejected.class, session.lessonChoice(2, lesson(1).decision().skip().id()));
        assertEquals(before, signature(session.view()), "nothing settled");

        assertTrue(session.lessonChoice(1, right).ok());
        assertInstanceOf(Outcome.Rejected.class, session.lessonChoice(1, lesson(1).decision().skip().id()), "the class is over");

        GameSession tomorrow = inClass(2);
        String dayTwo = signature(tomorrow.view());
        assertInstanceOf(Outcome.Rejected.class, tomorrow.lessonChoice(1, right), "a late callback from yesterday");
        assertInstanceOf(Outcome.Rejected.class, tomorrow.lessonChoice(1, lesson(1).decision().skip().id()));
        assertEquals(dayTwo, signature(tomorrow.view()));
    }

    private static String signature(GameState s) {
        return String.join("|", List.of(String.valueOf(s.day()), s.period().name(), s.student().toString(),
                String.valueOf(s.effectiveSan()), s.effects().toString(), String.valueOf(s.lessonShown()),
                s.grades().records().toString(), s.status().name()));
    }
}
