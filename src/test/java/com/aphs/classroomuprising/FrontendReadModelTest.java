package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FrontendReadModelTest {
    @TempDir Path temp;

    @Test void activityIdsAreSortedImmutableAndPreserveCatalog() {
        var catalog = Fixtures.catalog();
        var ids = catalog.hubActivityIds();
        assertEquals(List.of("rest", "study"), ids);
        assertThrows(UnsupportedOperationException.class, () -> ids.add("invented"));
        assertEquals(ids, catalog.hubActivityIds());
        assertNotNull(catalog.hub("rest"));
        assertNotNull(catalog.hub("study"));
    }

    @Test void pendingCatalogDoesNotInventActivities() {
        assertEquals(List.of(), ContentCatalog.pending().hubActivityIds());
    }

    @Test void lessonFlagFollowsExistingTransitionAndSnapshotsStayDetached() {
        var game = Fixtures.game(3, DayPeriod.ICS_CLASS);
        var before = game.snapshot();
        assertFalse(before.lessonShown());
        game.showLesson();
        assertTrue(game.snapshot().lessonShown());
        assertFalse(before.lessonShown());
        game.lessonChoice("learn");
        assertTrue(game.snapshot().lessonShown());
        game.otherClasses(false);
        game.sleepImmediatelyAfterSchool();
        assertTrue(game.snapshot().lessonShown());
        game.beginNextDay();
        assertFalse(game.snapshot().lessonShown());
    }

    @Test void assessmentFlagSurvivesRejectedSubmissionAndClearsOnlyOnSuccess() {
        var game = Fixtures.game(5, DayPeriod.ICS_CLASS);
        var before = game.snapshot();
        assertFalse(before.hasActiveAssessment());
        game.startAssessment();
        var active = game.snapshot();
        assertTrue(active.hasActiveAssessment());
        assertFalse(before.hasActiveAssessment());
        assertThrows(IllegalStateException.class, game::startAssessment);
        assertThrows(IllegalArgumentException.class, () -> game.recordEvaluatedSubmission(Double.NaN, "fixture"));
        assertTrue(game.snapshot().hasActiveAssessment());
        game.recordEvaluatedSubmission(80, "fixture evaluator");
        assertFalse(game.snapshot().hasActiveAssessment());
        assertTrue(active.hasActiveAssessment());
    }

    @Test void repeatedReadsDoNotChangeStateOrPermitInvalidActions() {
        var game = Fixtures.game(3, DayPeriod.ICS_CLASS);
        String before = Fixtures.signature(game.snapshot());
        for (int i = 0; i < 5; i++) {
            assertFalse(game.snapshot().lessonShown());
            assertFalse(game.snapshot().hasActiveAssessment());
            assertEquals(List.of("rest", "study"), Fixtures.catalog().hubActivityIds());
        }
        assertEquals(before, Fixtures.signature(game.snapshot()));
        assertThrows(IllegalStateException.class, () -> game.hubActivity("rest"));
        assertEquals(before, Fixtures.signature(game.snapshot()));
    }

    @Test void existingDayEndSavePreservesLessonFlagAndNextDayResetsIt() throws Exception {
        var game = Fixtures.game(3, DayPeriod.ICS_CLASS);
        game.showLesson(); game.lessonChoice("quiet"); game.otherClasses(false); game.sleepImmediatelyAfterSchool();
        Path file = temp.resolve("day-end.properties");
        var saves = new SaveManager(); saves.saveAtDayEnd(game.snapshot(), file);
        var restored = Fixtures.game(saves.load(file));
        assertTrue(restored.snapshot().lessonShown());
        assertFalse(restored.snapshot().hasActiveAssessment());
        assertEquals(DayPeriod.DAY_END, restored.snapshot().period());
        restored.beginNextDay();
        assertFalse(restored.snapshot().lessonShown());
        assertFalse(restored.snapshot().hasActiveAssessment());
    }
}
