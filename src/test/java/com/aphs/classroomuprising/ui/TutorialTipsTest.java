package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.SchoolCalendar;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Tutorial tips: game notices for the first two days, without restated numbers. */
class TutorialTipsTest {
    @Test void tipsAppearOnlyDuringTheTutorialAfterTheOpening() throws IOException {
        GameSession session = GameSession.newGame(DemoProfile.load(), "Tester", new Random(1), Path.of("unused"));
        assertTrue(TutorialTips.at(TutorialTips.Moment.LESSON_CHOICE, session.view()).isEmpty(), "not before the opening");
        OpeningPlay.enterDayOne(session);
        assertTrue(TutorialTips.at(TutorialTips.Moment.LESSON_CHOICE, session.view()).isPresent());

        for (int day = 1; day <= 2; day++) {
            session.openLesson(); session.lessonChoice("quiet"); session.otherClasses(false);
            session.hubActivity("rest"); session.sleepNormally(); session.beginNextDay();
        }
        GameState day3 = session.view();
        assertFalse(day3.isTutorial());
        for (TutorialTips.Moment moment : TutorialTips.Moment.values())
            assertTrue(TutorialTips.at(moment, day3).isEmpty(), moment + " after the tutorial");
    }

    @Test void everyMomentHasItsOwnTipAndNoAmounts() throws IOException {
        GameSession session = GameSession.newGame(DemoProfile.load(), "Tester", new Random(1), Path.of("unused"));
        OpeningPlay.enterDayOne(session);
        var ids = new HashSet<String>();
        for (TutorialTips.Moment moment : TutorialTips.Moment.values()) {
            TutorialTips.Tip tip = TutorialTips.at(moment, session.view()).orElseThrow();
            assertTrue(ids.add(tip.id()), "tip ids are distinct");
            assertFalse((tip.title() + tip.body()).matches(".*\\d.*"), "amounts come from the backend previews: " + tip.id());
            assertFalse(tip.body().contains("Mr. Reward"), "tips are the game's voice, not a character's");
        }
    }

    @Test void theTutorialDaysAreLabelledAsTheGrade11Review() {
        assertEquals("LESSON · GR11 REVIEW", UiText.courseLine(SchoolCalendar.day(1)));
        assertEquals("LESSON · GR11 REVIEW", UiText.courseLine(SchoolCalendar.day(2)));
        assertFalse(UiText.courseLine(SchoolCalendar.day(3)).contains("GR11"));
    }
}
