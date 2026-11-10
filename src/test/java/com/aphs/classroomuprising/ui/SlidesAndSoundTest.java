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

/** Lecture slides follow the lecture; every interface sound and music cue is listed; volume settings persist. */
class SlidesAndSoundTest {
    /** The cues the screens ask for (keep in sync with each Screen#music()). */
    private static final List<String> CUES = List.of("title", "opening", "school", "after-school", "night", "combat");

    @Test void aSlideStaysUpUntilTheNextOneStarts() {
        var slides = List.of(new AssetManifest.Slide(1, 2, "a.png"), new AssetManifest.Slide(1, 5, "b.png"),
                new AssetManifest.Slide(1, 9, "c.png"));
        assertEquals(-1, SlideProjector.indexFor(slides, 1), "no slide before the first one starts");
        assertEquals(0, SlideProjector.indexFor(slides, 2));
        assertEquals(0, SlideProjector.indexFor(slides, 4));
        assertEquals(1, SlideProjector.indexFor(slides, 5));
        assertEquals(2, SlideProjector.indexFor(slides, 9));
        assertEquals(2, SlideProjector.indexFor(slides, 40), "the last slide stays to the end");
        assertEquals(-1, SlideProjector.indexFor(List.of(), 3));
    }

    @Test void bundledSlidesFitTheirLectures() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        DemoProfile profile = DemoProfile.load();
        assertEquals(List.of(1, 2), manifest.slideDays(), "the Grade 11 review lectures have slides");
        for (int day : manifest.slideDays()) {
            int lines = DialogueScript.parse(profile.content().lesson(day).text(), "Mr. Reward").size();
            List<AssetManifest.Slide> slides = manifest.slides(day);
            assertEquals(1, slides.get(0).fromLine(), "day " + day + " shows a slide from its first line");
            for (AssetManifest.Slide slide : slides)
                assertTrue(slide.fromLine() <= lines, "day " + day + " has no line " + slide.fromLine() + " (it has " + lines + ")");
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
