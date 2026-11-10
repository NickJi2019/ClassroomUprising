package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ui.scene.AssetStore;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Plays the window's music and interface sounds. Music is chosen by cue (each screen names
 * one, see {@link Screen#music()}): asking for the cue that is already playing changes nothing,
 * so a track carries on across screens; a new cue fades the old track out while the new one
 * fades in. Files come from the asset manifest ({@code bgm.<cue>}, {@code sfx.<name>}); one that
 * is not delivered, or cannot be decoded, is silent and listed with the missing assets.
 * Sound never decides anything in the game. Call from the JavaFX thread.
 */
final class AudioDirector {
    private static final System.Logger LOG = System.getLogger("classroomuprising.audio");
    private static final Duration FADE_IN = Duration.millis(900);
    private static final Duration FADE_OUT = Duration.millis(500);
    /** A track that is not ready this long after loading is treated as unplayable (JavaFX reports some bad files never). */
    private static final Duration READY_LIMIT = Duration.seconds(5);

    private final AssetStore assets;
    private final Path settingsFile;
    private AudioSettings settings;
    private String cue;
    private Track track;
    private final Map<Sfx, Optional<AudioClip>> clips = new EnumMap<>(Sfx.class);

    /**
     * @param assets       where music and sounds are found, or null for a silent window
     * @param settingsFile where volume changes are kept, or null to keep them for this run only
     */
    AudioDirector(AssetStore assets, Path settingsFile) {
        this.assets = assets;
        this.settingsFile = settingsFile;
        this.settings = AudioSettings.load(settingsFile);
    }

    AudioSettings settings() { return settings; }

    /** The music cue asked for last (it may be silent if its file is missing), or null. */
    String cue() { return cue; }

    /** Whether a music track is loaded for the current cue. */
    boolean hasTrack() { return track != null; }

    /** Switches to a music cue; null keeps whatever is playing. */
    void music(String next) {
        if (next == null || next.equals(cue)) return;
        cue = next;
        if (track != null) {
            Track old = track;
            track = null;
            old.fadeTo(0, FADE_OUT, old::dispose);
        }
        if (assets == null) return;
        assets.bgm(next).ifPresent(url -> {
            try {
                Media media = new Media(url.toExternalForm());
                MediaPlayer player = new MediaPlayer(media);
                player.setCycleCount(MediaPlayer.INDEFINITE);
                Track started = new Track(player);
                Runnable failed = () -> {
                    if (started.disposed) return;
                    LOG.log(System.Logger.Level.WARNING, "Music could not be played: {0}", next);
                    assets.soundFailed(url);
                    started.dispose();
                    if (track == started) track = null;
                };
                media.setOnError(failed);
                player.setOnError(failed);
                PauseTransition watchdog = new PauseTransition(READY_LIMIT);
                watchdog.setOnFinished(e -> {
                    MediaPlayer.Status status = player.getStatus();
                    if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) failed.run();
                });
                watchdog.play();
                track = started;
                player.play();
                started.fadeTo(1, FADE_IN, null);
            } catch (RuntimeException e) {
                assets.soundFailed(url);
                LOG.log(System.Logger.Level.WARNING, "Music could not start: {0} ({1})", next, e.getMessage());
            }
        });
    }

    /** Plays an interface sound at the effects volume. */
    void play(Sfx sound) {
        if (assets == null || settings.effects() == 0) return;
        clips.computeIfAbsent(sound, s -> assets.sfx(s.key()).flatMap(url -> {
            try {
                return Optional.of(new AudioClip(url.toExternalForm()));
            } catch (RuntimeException e) {
                assets.soundFailed(url);
                return Optional.empty();
            }
        })).ifPresent(clip -> clip.play(AudioSettings.gain(settings.effects())));
    }

    void setMusic(int level) { change(settings.withMusic(level)); }

    void setEffects(int level) { change(settings.withEffects(level)); }

    private void change(AudioSettings next) {
        if (next.equals(settings)) return;
        settings = next;
        if (track != null) track.apply();
        if (settingsFile == null) return;
        try {
            settings.save(settingsFile);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Volume settings could not be saved: {0}", e.getMessage());
        }
    }

    /** One playing track and its fade; the heard volume is fade × the music volume. */
    private final class Track {
        private final MediaPlayer player;
        private final DoubleProperty fade = new SimpleDoubleProperty(0);
        private Timeline ramp;
        private boolean disposed;

        Track(MediaPlayer player) {
            this.player = player;
            fade.addListener((obs, a, b) -> apply());
            apply();
        }

        void apply() { player.setVolume(fade.get() * AudioSettings.gain(settings.music())); }

        void fadeTo(double target, Duration time, Runnable then) {
            if (ramp != null) ramp.stop();
            ramp = new Timeline(new KeyFrame(time, new KeyValue(fade, target)));
            if (then != null) ramp.setOnFinished(e -> then.run());
            ramp.play();
        }

        void dispose() {
            if (disposed) return;
            disposed = true;
            if (ramp != null) ramp.stop();
            player.dispose();
        }
    }
}
