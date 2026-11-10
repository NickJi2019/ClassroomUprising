package com.aphs.classroomuprising.ui.scene;

import com.aphs.classroomuprising.DayPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** The art manifest: what the game asks for, and that bad entries fail loudly. */
class AssetManifestTest {
    /** Places the screens show (keep in sync with the screen classes). */
    private static final String[] PLACES = {"title", "opening", "classroom", "hallway", "hub", "dorm", "computer-lab"};

    @Test void bundledManifestCoversEveryPlaceTheScreensUse() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        for (String place : PLACES) assertTrue(manifest.background(place, null).isPresent(), "no background listed for " + place);
    }

    @Test void everyTimeOfDayHasAGrade() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        for (DayPeriod period : DayPeriod.values())
            assertTrue(manifest.grade(period.name().toLowerCase(Locale.ROOT)).isPresent(), "no grade for " + period);
        assertTrue(manifest.grade("title").isPresent());
        assertTrue(manifest.grade("opening").isPresent());
    }

    @Test void dialogueSpeakersMapToCharacters() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        assertEquals("reward", manifest.characterNamed("Mr. Reward").orElseThrow().id());
        assertEquals("student", manifest.characterNamed("You").orElseThrow().id());
        assertTrue(manifest.characterNamed("Narrator").isEmpty());
        assertTrue(manifest.character("reward").orElseThrow().expressions().containsKey("neutral"));
    }

    @Test void bundledTexturesAreDelivered() throws IOException {
        AssetManifest manifest = AssetManifest.bundled();
        for (String name : new String[] {"grain", "motes"}) {
            String file = manifest.texture(name).orElseThrow();
            assertNotNull(AssetManifest.class.getResource("/com/aphs/classroomuprising/assets/" + file), file);
        }
    }

    @Test void aListedVariantWinsOverTheBaseArt() throws IOException {
        AssetManifest manifest = read("""
                background.hub = backgrounds/hub.png
                background.hub.evening = backgrounds/hub-evening.png
                """);
        assertEquals("backgrounds/hub-evening.png", manifest.background("hub", "evening").orElseThrow());
        assertEquals("backgrounds/hub.png", manifest.background("hub", "late_night").orElseThrow());
        assertEquals("backgrounds/hub.png", manifest.background("hub", null).orElseThrow());
        assertTrue(manifest.background("roof", null).isEmpty());
    }

    @Test void gradesParse() throws IOException {
        Grade grade = read("grade.evening = #3B4FB8, multiply, 0.45, -0.25, -0.10").grade("evening").orElseThrow();
        assertEquals(0.45, grade.tintOpacity(), 1e-9);
        assertEquals(javafx.scene.effect.BlendMode.MULTIPLY, grade.mode());
    }

    @Test void sequencesListEveryFrameAndDefaultTheirTiming() throws IOException {
        var spec = read("""
                sequence.menu-open.frames = transitions/menu-open/frame-%02d.png
                sequence.menu-open.count = 3
                """).sequence("menu-open").orElseThrow();
        assertEquals(java.util.List.of("transitions/menu-open/frame-00.png", "transitions/menu-open/frame-01.png",
                "transitions/menu-open/frame-02.png"), spec.files());
        assertEquals(50, spec.frameMillis(), 1e-9);
        assertEquals(1, spec.swapAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sequence.x.frames = t/f-%02d.png",                                         // no count
            "sequence.x.frames = t/f.png\nsequence.x.count = 4",                        // no frame number
            "sequence.x.frames = t/f-%02d.png\nsequence.x.count = 4\nsequence.x.swap-at = 4",  // swap past the end
            "sequence.x.frames = t/f-%02d.png\nsequence.x.count = 4\nsequence.x.frame-ms = 2", // too fast
            "sequence.x.speed = 3",                                                      // unknown field
    })
    void malformedSequencesAreRejected(String text) {
        assertThrows(IOException.class, () -> read(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sprite.hero = a.png",                                  // unknown kind
            "background.hub = ../secret.png",                       // escapes the folder
            "background.hub = Backgrounds/Hub.PNG",                 // not lower-case
            "background.hub = backgrounds/hub.gif",                 // unsupported format
            "texture.grain.extra = textures/grain.png",             // too many parts
            "grade.evening = #3B4FB8, MULTIPLY, 2, 0, 0",           // opacity out of range
            "grade.evening = #3B4FB8, NOT_A_MODE, 0.5, 0, 0",       // unknown blend mode
            "character.x.name = X\ncharacter.x.height = 600\ncharacter.x.anchor = 0.5, 1.0",  // no neutral expression
            "character.x.name = X\ncharacter.x.height = 600\ncharacter.x.anchor = 1.5, 1.0\ncharacter.x.expression.neutral = c/x.png",
            "bgm.title = audio/bgm/title.ogg",                      // JavaFX cannot play Ogg
            "sfx.confirm = audio/sfx/confirm.png",                  // not a sound
            "bgm.title.loop = audio/bgm/title.mp3",                 // too many parts
            "slide.loops.2 = slides/review/13-loops.png",            // too many parts
            "slide.Loops = slides/review/13-loops.png",             // name not lower-case
            "slide.loops = slides/review/13-loops.mp3",             // not an image
    })
    void malformedEntriesAreRejected(String text) {
        assertThrows(IOException.class, () -> read(text));
    }

    @Test void slidesAreListedByTheNameLecturesCue() throws IOException {
        AssetManifest manifest = read("""
                slide.loops = slides/review/13-loops.png
                slide.file-structure = slides/review/07-file-structure.png
                """);
        assertEquals("slides/review/13-loops.png", manifest.slide("loops").orElseThrow().file());
        assertTrue(manifest.slide("arrays").isEmpty());
        assertEquals(java.util.List.of("file-structure", "loops"), manifest.slideNames());
        assertTrue(manifest.allFiles().contains("slides/review/07-file-structure.png"));
    }

    @Test void soundsAreListedByFamily() throws IOException {
        AssetManifest manifest = read("""
                bgm.title = audio/bgm/title.mp3
                bgm.night = audio/bgm/night.m4a
                sfx.confirm = audio/sfx/confirm.wav
                """);
        assertEquals("audio/bgm/night.m4a", manifest.bgm("night").orElseThrow());
        assertEquals("audio/sfx/confirm.wav", manifest.sfx("confirm").orElseThrow());
        assertTrue(manifest.bgm("confirm").isEmpty(), "music and effects are separate lists");
        assertEquals(java.util.List.of("night", "title"), manifest.audioNames("bgm"));
        assertTrue(manifest.allFiles().containsAll(java.util.List.of("audio/bgm/title.mp3", "audio/sfx/confirm.wav")));
    }

    private static AssetManifest read(String text) throws IOException {
        return AssetManifest.read(new StringReader(text));
    }
}
