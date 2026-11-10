package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.StatChange;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Display helpers: dialogue splitting, number formatting and stable choice order. */
class PresentationHelpersTest {
    @Test void dialogueSplitsSpeakerLines() {
        var lines = DialogueScript.parse("Mr. Reward: Welcome back.\nYou: Hi.\n\nNo speaker here.", "Narrator");
        assertEquals(List.of(
                new DialogueScript.Line("Mr. Reward", "Welcome back."),
                new DialogueScript.Line("You", "Hi."),
                new DialogueScript.Line("Narrator", "No speaker here.")), lines);
    }

    @Test void expressionTagSelectsArtAndIsNotShown() {
        var line = DialogueScript.parse("Mr. Reward[smile]: Good question.", "Narrator").get(0);
        assertEquals("Mr. Reward", line.speaker());
        assertEquals("smile", line.expression());
        assertEquals("Good question.", line.text());
        assertNull(DialogueScript.parse("Mr. Reward: Hi.", "Narrator").get(0).expression());
    }

    @Test void colonInsideASentenceIsNotASpeaker() {
        var lines = DialogueScript.parse("remember this: private fields stay private", "Mr. Reward");
        assertEquals("Mr. Reward", lines.get(0).speaker());
    }

    @Test void numbersAreShownWithoutFloatingPointNoise() {
        assertEquals("+3", StatDelta.signed(3.0));
        assertEquals("−2.5", StatDelta.signed(-2.5));
        assertEquals("0.3", StatDelta.number(0.1 + 0.2));
    }

    @Test void configuredEffectsListOnlyNonZeroStatsAndScale() {
        var effect = new StatChange(0, -2, 8, List.of(4.0, 0.0, 0.0, 0.0, 0.0, 0.0));
        // SAN as a share of the bar; Coding Vibe and knowledge as one XP change.
        assertEquals(List.of("SAN −1%", "XP +6"),
                StatDelta.of(effect, 0.5).stream().map(StatDelta::text).toList());
        assertEquals(List.of(), StatDelta.of(new StatChange(0, 0, 0, Collections.nCopies(6, 0.0)), 1));
    }

    @Test void sanIsAShareOfAFullBarAndNeverOverflowsIt() {
        assertEquals(0.6, Stats.sanFraction(60), 1e-9);
        assertEquals(0.8, Stats.sanFraction(80), 1e-9);
        assertEquals(1.0, Stats.sanFraction(130), 1e-9, "above the display scale: a full bar");
        assertEquals(0.0, Stats.sanFraction(-5), 1e-9);
        assertEquals("−4%", StatDelta.san(-4));
        assertEquals("+6%", StatDelta.san(6));
    }

    @Test void xpIsCodingVibePlusEveryUnitAndItsPartsKeepThatOrder() {
        var student = new com.aphs.classroomuprising.StudentState(100, 80, 4, List.of(2.0, 0.0, 1.5, 0.0, 0.0, 3.0));
        assertEquals(10.5, Stats.xp(student), 1e-9);
        assertEquals(List.of("CODING VIBE", "U1", "U2", "U3", "U4", "U5", "U6"),
                Stats.xpParts(student).stream().map(Stats.Part::name).toList());
        assertEquals(List.of(4.0, 2.0, 0.0, 1.5, 0.0, 0.0, 3.0),
                Stats.xpParts(student).stream().map(Stats.Part::value).toList());
    }

    @Test void choicesHaveAStableOrderWithSilenceLast() {
        assertEquals(List.of("ask", "raise", "quiet"), UiText.choiceOrder(Set.of("quiet", "raise", "ask")));
    }
}
