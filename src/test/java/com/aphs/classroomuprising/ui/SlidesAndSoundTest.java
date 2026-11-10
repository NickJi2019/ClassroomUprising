package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.ui.scene.AssetManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lectures cue their slides; every cued slide, interface sound and music cue is listed; volume settings persist. */
class SlidesAndSoundTest {
    /** The cues the screens ask for (keep in sync with each Screen#music()). */
    private static final List<String> CUES = List.of("title", "opening", "school", "after-school", "night", "combat");

    @Test void aSlideCueAtTheStartOfALineIsTakenOutOfTheText() {
        var lines = DialogueScript.parse("""
                Mr. Reward: [slide:file-structure] Start with the file.
                Mr. Reward: Its name must match the class.
                Mr. Reward[smile]: [slide:example-java]   Here is Example.java.
                Mr. Reward: Inside is main(String[] args), not a [slide:cue].
                """, "Mr. Reward");
        assertEquals("file-structure", lines.get(0).slide());
        assertEquals("Start with the file.", lines.get(0).text(), "the cue is never shown");
        assertNull(lines.get(1).slide(), "a line without a cue keeps the slide that is up");
        assertEquals("example-java", lines.get(2).slide());
        assertEquals("smile", lines.get(2).expression());
        assertEquals("Here is Example.java.", lines.get(2).text());
        assertNull(lines.get(3).slide(), "a cue counts only at the start of the text");
        assertEquals("Inside is main(String[] args), not a [slide:cue].", lines.get(3).text());
    }

    @Test void theProjectorCountsEachCuedSlideOnceInLectureOrder() {
        var lines = DialogueScript.parse("""
                Mr. Reward: [slide:loops] One.
                Mr. Reward: Two.
                Mr. Reward: [slide:methods] Three.
                Mr. Reward: [slide:loops] Back to loops.
                """, "Mr. Reward");
        assertEquals(List.of("loops", "methods"), SlideProjector.cues(lines));
        assertTrue(SlideProjector.cues(DialogueScript.parse("Mr. Reward: No slides today.", "Mr. Reward")).isEmpty());
    }

    @Test void everySlideTheLecturesCueIsListedAndDelivered() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        DemoProfile profile = DemoProfile.load();
        for (int day = 1; day <= 4; day++) {
            for (DialogueScript.Line line : DialogueScript.parse(profile.content().lesson(day).text(), "Mr. Reward")) {
                assertFalse(line.text().contains("[slide:"), "day " + day + ": a cue must start its line: " + line.text());
                if (line.slide() == null) continue;
                String file = manifest.slide(line.slide()).orElseThrow(
                        () -> new AssertionError("day lecture cues slide." + line.slide() + ", which is not listed")).file();
                assertNotNull(getClass().getResource("/com/aphs/classroomuprising/assets/" + file), file);
            }
        }
    }

    @Test void everyListedSlideIsDelivered() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        assertFalse(manifest.slideNames().isEmpty());
        for (String name : manifest.slideNames()) {
            String file = manifest.slide(name).orElseThrow().file();
            assertNotNull(getClass().getResource("/com/aphs/classroomuprising/assets/" + file), file);
        }
    }

    @Test void everyInterfaceSoundIsListedAndDelivered() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        for (Sfx sound : Sfx.values()) {
            String file = manifest.sfx(sound.key()).orElseThrow(() -> new AssertionError("sfx." + sound.key() + " not listed"));
            assertNotNull(getClass().getResource("/com/aphs/classroomuprising/assets/" + file), file);
        }
        assertEquals(Sfx.values().length, manifest.audioNames("sfx").size(), "no sound is listed that the code never plays");
    }

    @Test void everyMusicCueIsListed() throws IOException {
        assertEquals(CUES.stream().sorted().toList(), AssetManifest.bundled().audioNames("bgm"));
    }

    @Test void volumeLevelsStayInRangeAndSoundEven() {
        AudioSettings settings = new AudioSettings(-3, 99);
        assertEquals(0, settings.music());
        assertEquals(AudioSettings.MAX, settings.effects());
        assertEquals(0, AudioSettings.gain(0));
        assertEquals(1, AudioSettings.gain(AudioSettings.MAX));
        assertEquals(0.25, AudioSettings.gain(AudioSettings.MAX / 2), 1e-9);
    }

    @Test void volumeSettingsAreKeptBesideTheSave(@TempDir Path dir) throws IOException {
        Path save = dir.resolve("day-end-save.properties");
        Path file = AudioSettings.fileFor(save);
        assertEquals(dir.resolve("settings.properties"), file);
        assertEquals(AudioSettings.DEFAULT, AudioSettings.load(file), "no file yet: defaults");
        new AudioSettings(3, 0).save(file);
        assertEquals(new AudioSettings(3, 0), AudioSettings.load(file));
        assertFalse(Files.exists(save), "volume is not game progress and never writes the save");
        Files.writeString(file, "music=loud\n");
        assertEquals(AudioSettings.DEFAULT, AudioSettings.load(file), "an unreadable file falls back to defaults");
    }
}
