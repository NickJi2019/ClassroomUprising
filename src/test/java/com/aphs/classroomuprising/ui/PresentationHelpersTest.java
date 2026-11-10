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
        assertEquals(List.of("SAN −1", "VIBE +4", "K1 +2"),
                StatDelta.of(effect, 0.5).stream().map(StatDelta::text).toList());
        assertEquals(List.of(), StatDelta.of(new StatChange(0, 0, 0, Collections.nCopies(6, 0.0)), 1));
    }

    @Test void choicesHaveAStableOrderWithSilenceLast() {
        assertEquals(List.of("ask", "raise", "quiet"), UiText.choiceOrder(Set.of("quiet", "raise", "ask")));
    }
}
