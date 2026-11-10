package com.aphs.classroomuprising.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The player's volume levels, from 0 (off) to {@link #MAX} in whole steps. They are window
 * preferences, kept in their own file next to the save; they are not game progress and are
 * never part of the day-end save.
 */
record AudioSettings(int music, int effects) {
    static final int MAX = 10;
    static final AudioSettings DEFAULT = new AudioSettings(7, 8);

    AudioSettings {
        music = clamp(music);
        effects = clamp(effects);
    }

    AudioSettings withMusic(int level) { return new AudioSettings(level, effects); }

    AudioSettings withEffects(int level) { return new AudioSettings(music, level); }

    /** Playback volume for a level: squared, so each step sounds about as big as the last. */
    static double gain(int level) {
        double fraction = clamp(level) / (double) MAX;
        return fraction * fraction;
    }

    /** The file that holds the settings for a save file. */
    static Path fileFor(Path savePath) {
        return savePath.resolveSibling("settings.properties");
    }

    /** Reads the settings; a missing or unreadable file gives the defaults. */
    static AudioSettings load(Path file) {
        if (file == null || !Files.isRegularFile(file)) return DEFAULT;
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(reader);
            return new AudioSettings(level(p, "music", DEFAULT.music), level(p, "effects", DEFAULT.effects));
        } catch (IOException | IllegalArgumentException e) {
            return DEFAULT;
        }
    }

    /** Writes the settings, replacing the file only once the new one is complete. */
    void save(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Properties p = new Properties();
        p.setProperty("music", Integer.toString(music));
        p.setProperty("effects", Integer.toString(effects));
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            p.store(writer, "Classroom Uprising window settings (volume 0-" + MAX + ")");
        }
        Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private static int level(Properties p, String key, int fallback) {
        String value = p.getProperty(key);
        return value == null ? fallback : Integer.parseInt(value.strip());
    }

    private static int clamp(int level) { return Math.max(0, Math.min(MAX, level)); }
}
