package com.aphs.classroomuprising;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * Checks the small amount of real behaviour in the skeleton: wiring and initial state.
 *
 * @author TeamPike
 * @since 1.0
 */
class SkeletonTest {

    @Test
    void createMainMenuConnectsControllersWithoutError() {
        assertNotNull(Main.createMainMenu(new GameUI()));
    }

    @Test
    void dayPeriodsAreDeclaredInDailyOrder() {
        DayPeriod[] expected = {DayPeriod.ICS_CLASS, DayPeriod.OTHER_CLASSES, DayPeriod.STUDENT_HUB};
        assertArrayEquals(expected, DayPeriod.values());
    }

    @Test
    void newCalendarStartsOnDayOneInIcsClass() {
        SchoolCalendar calendar = new SchoolCalendar(new ArrayList<>());
        assertEquals(1, calendar.getCurrentDay());
        assertEquals(DayPeriod.ICS_CLASS, calendar.getCurrentPeriod());
    }

    @Test
    void newGameStateHasEmptyAssessmentHistory() {
        GameState state = new GameState(
                new StudentState("Test", 0, 0, 0, 0),
                new SchoolCalendar(new ArrayList<>()),
                new ActionBudget(),
                new ContentProgress());
        assertTrue(state.getAssessmentHistory().isEmpty());
    }
}
