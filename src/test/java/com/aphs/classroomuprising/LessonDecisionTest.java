package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LessonDecisionTest {
    private LessonDecision decision() throws IOException { return DemoProfile.load().content().lesson(1).decision(); }
    private LessonDecision.Option replace(LessonDecision.Option o, String id, LessonDecision.Outcome outcome, StatChange effect) {
        return new LessonDecision.Option(id, o.label(), outcome, effect, o.feedback());
    }
    private String source() throws IOException {
        try (var in = DemoProfile.class.getResourceAsStream("/demo/demo.properties")) {
            assertNotNull(in); return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    @Test void authoredOrderAndViewsAreImmutable() throws Exception {
        var options = new ArrayList<>(decision().options()); var d = new LessonDecision("Question?", options);
        options.clear(); assertEquals(4, d.options().size());
        assertEquals(List.of("d1-equality", "d1-assignment", "d1-increment"), d.responses().stream().map(LessonDecision.Option::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> d.options().clear());
        assertThrows(UnsupportedOperationException.class, () -> d.responses().clear());
        assertThrows(UnsupportedOperationException.class, () -> d.effects().clear());
        assertThrows(IllegalArgumentException.class, () -> d.option("ask"));
        assertEquals(LessonDecision.Outcome.SKIP, d.skip().outcome());
    }
    @Test void malformedDecisionsCannotGrantBonusesForWrongOrSilentChoices() throws Exception {
        var d = decision(); var good = d.responses().getFirst(); var bad = d.responses().get(1); var skip = d.skip();
        assertThrows(IllegalArgumentException.class, () -> replace(bad, bad.id(), bad.outcome(), good.effect()));
        assertThrows(IllegalArgumentException.class, () -> replace(bad, bad.id(), bad.outcome(), StatChange.none()));
        assertThrows(IllegalArgumentException.class, () -> replace(skip, skip.id(), skip.outcome(), good.effect()));
        assertThrows(IllegalArgumentException.class, () -> replace(good, good.id(), good.outcome(), StatChange.none()));
        assertThrows(IllegalArgumentException.class, () -> replace(good, "", good.outcome(), good.effect()));
        assertThrows(IllegalArgumentException.class, () -> new LessonDecision("", d.options()));
        assertThrows(IllegalArgumentException.class, () -> new LessonDecision("?", List.of(good, good, bad, skip)));
        assertThrows(IllegalArgumentException.class, () -> new LessonDecision("?", List.of(good, bad)));
        assertThrows(IllegalArgumentException.class, () -> new LessonDecision("?", List.of(bad, skip)));
        assertThrows(IllegalArgumentException.class, () -> new LessonDecision("?", List.of(good, skip)));
    }
    @Test void lessonEffectsCannotDisagreeWithTheAuthoredDecision() throws Exception {
        var d = decision();
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog.Lesson("text", ContentCatalog.Interaction.RAISE_HAND,
                Map.of("quiet", StatChange.none()), "Title", d));
        assertThrows(IllegalArgumentException.class, () -> new ContentCatalog.Lesson("", "text", ContentCatalog.Interaction.RAISE_HAND, d));
        var legacy = new ContentCatalog.Lesson("fixture", ContentCatalog.Interaction.ASK_TEACHER, Map.of("quiet", StatChange.none()));
        assertNull(legacy.decision()); assertEquals(StatChange.none(), legacy.choices().get("quiet"));
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void everyShippedLessonHasThreeResponsesAndOneNeutralSkip(int day) throws Exception {
        var lesson = DemoProfile.load().content().lesson(day); var d = lesson.decision();
        assertEquals(day == 1 ? ContentCatalog.Interaction.RAISE_HAND : ContentCatalog.Interaction.ASK_TEACHER, lesson.interaction());
        assertFalse(lesson.title().isBlank()); assertFalse(d.prompt().isBlank());
        assertEquals(3, d.responses().size()); assertEquals(1, d.responses().stream().filter(o -> o.outcome() == LessonDecision.Outcome.APPROPRIATE).count());
        assertEquals(StatChange.none(), d.skip().effect()); assertEquals(d.effects(), lesson.choices());
        assertTrue(d.responses().stream().allMatch(o -> o.id().startsWith("d" + day + "-")));
    }
    @Test void loaderRejectsIncompleteOrContradictoryAuthoredOptions() throws Exception {
        String s = source();
        for (String invalid : List.of(
                s.replace("lesson.1.title=Grade 11 Review: Java Basics", ""),
                s.replace("lesson.1.option.d1-assignment.outcome=INAPPROPRIATE", "lesson.1.option.d1-assignment.outcome=MAYBE"),
                s.replace("lesson.1.option.d1-assignment.effect=0,-2,0,0,0,0,0,0,0", "lesson.1.option.d1-assignment.effect=0,-2,4,2,0,0,0,0,0"),
                s.replace("lesson.1.option.quiet.effect=0,0,0,0,0,0,0,0,0", "lesson.1.option.quiet.effect=0,-2,0,0,0,0,0,0,0"),
                s.replace("lesson.1.options=d1-equality,d1-assignment,d1-increment,quiet", "lesson.1.options=d1-equality,d1-equality,quiet"),
                s + "\nlesson.1.option.unknown.label=Orphan\n"))
            assertThrows(IOException.class, () -> DemoProfile.read(new StringReader(invalid)));
    }
    @Test void populatedTemplateLoadsAndCanBeMovedToAnotherScheduledLesson() throws Exception {
        String template = Files.readString(Path.of("docs/templates/lesson.properties"));
        String withoutDayFour = source().lines().filter(l -> !l.startsWith("lesson.4.")).reduce("", (a,b) -> a+b+"\n");
        var same = DemoProfile.read(new StringReader(withoutDayFour + template));
        assertEquals(DemoProfile.load().content().lesson(4), same.content().lesson(4));
        var moved = DemoProfile.read(new StringReader(withoutDayFour.replace("lesson.days=1,2,3,4", "lesson.days=1,2,3,6")
                + template.replace("lesson.4.", "lesson.6.").replace("d4-", "d6-")));
        assertEquals(List.of(1,2,3,6), moved.content().lessonDays());
        assertNotNull(moved.content().lesson(6).decision());
        assertThrows(PendingRuleException.class, () -> moved.content().lesson(4));
        assertThrows(UnsupportedOperationException.class, () -> moved.content().lessonDays().clear());
        assertEquals(40, SchoolCalendar.days().size());
    }
}
