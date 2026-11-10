package com.aphs.classroomuprising.ui.scene;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Loads the art listed in the {@link AssetManifest} and caches it. When a file has not been
 * delivered yet, it returns a clearly labelled "missing art" plate instead, so screens keep
 * their composition and nobody mistakes the stand-in for final art.
 * Call from the JavaFX thread.
 */
public final class AssetStore {
    private static final System.Logger LOG = System.getLogger("classroomuprising.assets");
    private static final String ROOT = "/com/aphs/classroomuprising/assets/";

    private final AssetManifest manifest;
    private final Function<String, URL> locate;
    private final Map<String, Optional<Image>> cache = new HashMap<>();
    private final Map<String, Optional<URL>> sounds = new HashMap<>();
    private final Set<String> missing = new LinkedHashSet<>();

    /**
     * @param locate turns a manifest file path into a URL, or null when the file is absent
     */
    public AssetStore(AssetManifest manifest, Function<String, URL> locate) {
        this.manifest = manifest;
        this.locate = locate;
    }

    /** The art bundled in the game's resources. */
    public static AssetStore bundled(AssetManifest manifest) {
        return new AssetStore(manifest, path -> AssetStore.class.getResource(ROOT + path));
    }

    public AssetManifest manifest() { return manifest; }

    /** Background art for a place, or a placeholder plate naming what is missing. */
    public Image background(String place, String variant) {
        String key = "background." + place + (variant == null ? "" : " (" + variant + ")");
        return manifest.background(place, variant).flatMap(this::load)
                .orElseGet(() -> Placeholders.background(key, manifest.background(place, variant).orElse("not listed")));
    }

    /** Optional art drawn in front of characters; empty when none is listed or delivered. */
    public Optional<Image> foreground(String place) {
        return manifest.foreground(place).flatMap(this::load);
    }

    /** A character expression; falls back to the neutral one, then to a placeholder plate. */
    public Image character(AssetManifest.CharacterSpec spec, String expression) {
        String wanted = spec.expressions().containsKey(expression) ? expression : "neutral";
        return load(spec.expressions().get(wanted))
                .or(() -> load(spec.expressions().get("neutral")))
                .orElseGet(() -> Placeholders.character(spec, wanted));
    }

    public Optional<Image> texture(String name) {
        return manifest.texture(name).flatMap(this::load);
    }

    /** Interface artwork such as the clock face; empty when not listed or not delivered. */
    public Optional<Image> ui(String name) {
        return manifest.ui(name).flatMap(this::load);
    }

    /**
     * A frame sequence, only when it is listed and every frame is delivered;
     * otherwise empty and the caller uses its built-in transition.
     */
    public Optional<FrameSequence> sequence(String name) {
        Optional<AssetManifest.SequenceSpec> spec = manifest.sequence(name);
        if (spec.isEmpty()) return Optional.empty();
        var frames = new java.util.ArrayList<Image>();
        for (String file : spec.get().files()) {
            Optional<Image> frame = load(file);
            if (frame.isEmpty()) return Optional.empty();
            frames.add(frame.get());
        }
        return Optional.of(new FrameSequence(frames, spec.get().frameMillis(), spec.get().swapAt()));
    }

    /**
     * The slide a lecture cues by name, or a slide-shaped plate that names what is missing:
     * the file when it is listed but not delivered, or the name when it is not listed at all.
     */
    public Image slide(String name) {
        Optional<AssetManifest.Slide> slide = manifest.slide(name);
        if (slide.isEmpty() && missing.add("slide." + name))
            LOG.log(System.Logger.Level.WARNING, "A lecture cues slide {0}, which the manifest does not list", name);
        return slide.flatMap(s -> load(s.file()))
                .orElseGet(() -> Placeholders.slide(name, slide.map(AssetManifest.Slide::file).orElse(null)));
    }

    /** The music file for a cue; empty when none is listed or the file is not delivered. */
    public Optional<URL> bgm(String cue) {
        return manifest.bgm(cue).flatMap(this::sound);
    }

    /** The sound-effect file for a name; empty when none is listed or the file is not delivered. */
    public Optional<URL> sfx(String name) {
        return manifest.sfx(name).flatMap(this::sound);
    }

    /** Records a sound that was found but could not be played (for example an unsupported encoding). */
    public void soundFailed(URL url) {
        sounds.forEach((path, found) -> {
            if (found.filter(url::equals).isPresent() && missing.add(path))
                LOG.log(System.Logger.Level.WARNING, "Sound could not be played: {0}", path);
        });
    }

    public Grade grade(String name) {
        return manifest.grade(name).orElse(Grade.NONE);
    }

    /** Files that were requested but not delivered, for the art checklist. */
    public Set<String> missingFiles() { return Set.copyOf(missing); }

    private Optional<URL> sound(String path) {
        return sounds.computeIfAbsent(path, p -> {
            URL url = locate.apply(p);
            if (url == null && missing.add(p)) LOG.log(System.Logger.Level.INFO, "Sound not delivered yet: {0}", p);
            return Optional.ofNullable(url);
        });
    }

    private Optional<Image> load(String path) {
        return cache.computeIfAbsent(path, p -> {
            URL url = locate.apply(p);
            if (url == null) {
                if (missing.add(p)) LOG.log(System.Logger.Level.INFO, "Art not delivered yet: {0}", p);
                return Optional.empty();
            }
            Image image = new Image(url.toExternalForm(), false);
            if (image.isError()) {
                missing.add(p);
                LOG.log(System.Logger.Level.WARNING, "Art could not be decoded: {0}", p);
                return Optional.empty();
            }
            return Optional.of(image);
        });
    }
}
