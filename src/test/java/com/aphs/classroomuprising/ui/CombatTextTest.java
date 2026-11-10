package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/** Combat display strings agree with the backend's numbers and the 30-second soft limit. */
class CombatTextTest {
    @Test void secondsRoundUpSoTheLimitReadsCorrectly() {
        assertEquals("0.0", CombatText.seconds(Duration.ZERO));
        assertEquals("12.4", CombatText.seconds(Duration.ofMillis(12_340)));
        assertEquals("30.0", CombatText.seconds(Duration.ofSeconds(30)), "exactly 30 s is on time");
        assertEquals("30.1", CombatText.seconds(Duration.ofSeconds(30).plusNanos(1)), "later never shows as 30.0");
    }

    @Test void weightsAndCreditsKeepSmallSteps() {
        assertEquals("×1.01", CombatText.weight(1.01));
        assertEquals("×2.40", CombatText.weight(2.4));
        assertEquals("3.10", CombatText.credits(3.1));
        assertEquals("50%", CombatText.percent(0.5));
    }

    @Test void statusesNameWhatThePlayerSees() {
        assertEquals("READY", CombatText.status(CombatSession.QuestionStatus.AVAILABLE));
        assertEquals("EVALUATING", CombatText.status(CombatSession.QuestionStatus.SUBMITTED));
        assertEquals("CLEARED", CombatText.status(CombatSession.QuestionStatus.ANSWERED));
        assertEquals("Q1", CombatText.label(0));
    }

    @Test void excerptsTakeTheFirstLine() {
        assertEquals("Short", CombatText.excerpt("Short\nsecond line", 20));
        assertEquals("abcd…", CombatText.excerpt("abcdefghij", 5));
    }
}
