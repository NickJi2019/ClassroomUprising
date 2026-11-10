package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.LessonDecision;
import com.aphs.classroomuprising.ui.scene.AssetManifest;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Mr. Reward's photos: delivered, on one shared canvas, and every expression the screens ask for exists. */
class CharacterArtTest {
    private static final String ROOT = "/com/aphs/classroomuprising/assets/";

    @Test void everyMrRewardExpressionIsDeliveredOnOneCanvas() throws IOException {
        AssetManifest.CharacterSpec reward = AssetManifest.bundled().character("reward").orElseThrow();
        Set<String> sizes = new HashSet<>();
        for (var expression : reward.expressions().entrySet()) {
            URL url = getClass().getResource(ROOT + expression.getValue());
            assertNotNull(url, expression.getKey() + ": " + expression.getValue());
            BufferedImage image = ImageIO.read(url);
            assertTrue(image.getColorModel().hasAlpha(), expression.getKey() + " is a cut-out");
            sizes.add(image.getWidth() + "x" + image.getHeight());
        }
        assertEquals(1, sizes.size(), "one canvas size, so switching expression never moves him: " + sizes);
        assertEquals("right", reward.faces(), "the photos face right; on the right of the screen he is mirrored");
    }

    @Test void theExpressionsTheScreensAskForAreListed() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        Set<String> reward = manifest.character("reward").orElseThrow().expressions().keySet();
        assertTrue(reward.contains(LessonMenu.ASKING));
        for (LessonDecision.Outcome outcome : LessonDecision.Outcome.values())
            assertTrue(reward.contains(LessonMenu.reaction(outcome)), outcome.name());
        for (DialogueScript.Line line : DialogueScript.parse(OpeningScreen.SCRIPT, "Narrator")) {
            if (line.expression() == null) continue;
            var character = manifest.characterNamed(line.speaker()).orElseThrow(() -> new AssertionError(line.speaker()));
            assertTrue(character.expressions().containsKey(line.expression()), line.speaker() + "[" + line.expression() + "]");
        }
    }

    @Test void theTitleCardArtIsDelivered() throws IOException {
        String file = AssetManifest.bundled().ui("reward-logo").orElseThrow();
        assertNotNull(getClass().getResource(ROOT + file), file);
    }

    @Test void anExpressionTheLineAlreadyNamesIsKept() {
        var plain = new DialogueScript.Line("Mr. Reward", "Hello.");
        assertEquals("gotcha", LessonMenu.withExpression(plain, "gotcha").expression());
        var tagged = DialogueScript.parse("Mr. Reward[smile]: Hello.", "Mr. Reward").get(0);
        assertEquals("smile", LessonMenu.withExpression(tagged, "gotcha").expression(), "authored tags win");
    }
}
