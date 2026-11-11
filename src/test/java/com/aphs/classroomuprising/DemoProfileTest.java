package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DemoProfileTest {
    @TempDir Path temp;
    private String source() throws IOException {
        try (var in = DemoProfile.class.getResourceAsStream("/demo/demo.properties")) {
            assertNotNull(in); return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private DemoProfile parse(String text) throws IOException { return DemoProfile.read(new StringReader(text)); }

    @Test void loadsExplicitDemoValuesAndIndependentNewGames() throws Exception {
        var demo = DemoProfile.load();
        assertEquals("DEMO — Days 1–4 / provisional values", demo.label());
        assertEquals(new StudentState(50,60,0,Collections.nCopies(6,0.0)), demo.initialStudent());
        assertEquals(6.0, demo.rules().normalSleepSanRecovery());
        assertEquals(List.of("rest", "study"), demo.content().hubActivityIds());
        var a = demo.newGame("A", new Random(42)); var b = demo.newGame("B", new Random(42));
        assertEquals(1, a.snapshot().day()); assertFalse(a.snapshot().openingCompleted());
        assertThrows(IllegalStateException.class, a::showLesson);
        OpeningTestSupport.enterDayOne(a); assertFalse(b.snapshot().openingCompleted());
        assertEquals(40, SchoolCalendar.days().size());
    }

    @Test void configuredContentHasExactlyOneInteractionAndSilentNoOp() throws Exception {
        var demo = DemoProfile.load();
        for (int day = 1; day <= 4; day++) {
            var lesson = demo.content().lesson(day);
            assertEquals(day == 1 ? ContentCatalog.Interaction.RAISE_HAND : ContentCatalog.Interaction.ASK_TEACHER, lesson.interaction());
            assertEquals(3, lesson.decision().responses().size());
            assertEquals(4, lesson.choices().size());
            assertFalse(lesson.title().isBlank());
            assertEquals(StatChange.none(), lesson.choices().get("quiet"));
            assertTrue(lesson.text().startsWith("Mr. Reward:"));
        }
    }

    @Test void fourDayWalkthroughUsesRealCoreTransitionsAndSaveLoad() throws Exception {
        var demo = DemoProfile.load(); var game = demo.newGame("Demo student", new Random(42));
        OpeningTestSupport.enterDayOne(game);
        for (int day = 1; day <= 4; day++) {
            assertEquals(day, game.snapshot().day());
            if (game.snapshot().period() == DayPeriod.MORNING_BONUS) {
                double before = game.snapshot().student().codingVibe();
                game.hubActivity("study");
                assertEquals(before + 4, game.snapshot().student().codingVibe());
            }
            var lesson = game.showLesson();
            String response = lesson.decision().responses().stream()
                    .filter(o -> o.outcome() == LessonDecision.Outcome.APPROPRIATE).findFirst().orElseThrow().id();
            var result = game.lessonChoice(day, response);
            assertEquals(day <= 2, !result.warnings().isEmpty());
            game.otherClasses(true);
            if (day == 2) game.sleepImmediatelyAfterSchool();
            else {
                game.hubActivity("study");
                if (day == 3) { game.hubActivity("rest"); game.hubActivity("study"); }
                else game.sleepNormally();
            }
            assertTrue(game.snapshot().canSave());
            Path file = temp.resolve("day-" + day + ".properties");
            var saves = new SaveManager(); saves.saveAtDayEnd(game.snapshot(), file);
            var restored = new GameController(saves.load(file), demo.rules(), demo.content(), new Random(42));
            assertEquals(Fixtures.signature(game.snapshot()), Fixtures.signature(restored.snapshot()));
            game = restored; game.beginNextDay();
            if (day == 3) { assertEquals(.5, game.snapshot().xpMultiplier());
                assertEquals(game.snapshot().student().san() - 4, game.snapshot().effectiveSan()); }
        }
        assertEquals(5, game.snapshot().day()); assertEquals(CourseType.LAB, game.snapshot().currentCourse().course());
        String before = Fixtures.signature(game.snapshot());
        assertEquals("T07", assertThrows(PendingRuleException.class, game::startAssessment).ruleId());
        assertEquals(before, Fixtures.signature(game.snapshot()));
        assertEquals(1, game.snapshot().xpMultiplier());
    }

    @Test void pendingContentAndCheckpointRulesAreNotFilledInByDemo() throws Exception {
        var demo = DemoProfile.load();
        assertThrows(PendingRuleException.class, () -> demo.content().lesson(6));
        assertThrows(PendingRuleException.class, () -> demo.content().review(18));
        assertThrows(PendingRuleException.class, () -> demo.content().questions(SchoolCalendar.day(20), new Random(42)));
        var checkpoint = Fixtures.state(20, DayPeriod.ICS_CLASS); checkpoint.status = GameState.Status.MIDTERM_DECISION;
        var game = new GameController(checkpoint, demo.rules(), demo.content(), new Random(42));
        assertEquals("T10", assertThrows(PendingRuleException.class, () -> game.resolveMidterm("continue")).ruleId());
    }

    @Test void changingConfigurationChangesNewStateAndRecoveryWithoutUiConstants() throws Exception {
        var demo = parse(source().replace("initial.san=60", "initial.san=72")
                .replace("rules.normalSleepSanRecovery=6", "rules.normalSleepSanRecovery=0"));
        var game = demo.newGame("Tuned Demo", new Random(42));
        OpeningTestSupport.enterDayOne(game); game.showLesson(); game.lessonChoice("quiet"); game.otherClasses(false);
        game.hubActivity("study"); game.sleepNormally();
        assertEquals(72, game.snapshot().student().san());
    }

    @ParameterizedTest @ValueSource(strings={"NaN","Infinity","-Infinity","not-a-number"})
    void invalidInitialNumberFailsRatherThanFallingBack(String value) throws Exception {
        String changed = source().replace("initial.hp=50", "initial.hp=" + value);
        assertThrows(IOException.class, () -> parse(changed));
    }

    @Test void missingUnknownAndMalformedValuesAreRejected() throws Exception {
        String source = source();
        for (String bad : List.of(
                source.replace("rules.normalSleepSanRecovery=6", ""),
                source + "\nunknown.setting=1\n",
                source.replace("initial.knowledge=0,0,0,0,0,0", "initial.knowledge=0,0"),
                source.replace("lesson.days=1,2,3,4", "lesson.days=1,2,3,4,5"),
                source.replace("lesson.days=1,2,3,4", "lesson.days=1,1,3,4"),
                source.replace("hub.ids=rest,study", "hub.ids=rest,"),
                source.replace("lesson.3.interaction=ASK_TEACHER", "lesson.3.interaction=BOTH"),
                source.replace("profile.label=DEMO", "profile.label=PRODUCTION")))
            assertThrows(IOException.class, () -> parse(bad));
    }
}
