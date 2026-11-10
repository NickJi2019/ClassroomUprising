package com.aphs.classroomuprising.ui.scene;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The list of art the game asks for, read from {@code assets/manifest.properties}.
 * Code refers to art only by key (for example {@code background.classroom}); the manifest
 * says which file holds it. A key whose file is not delivered yet is still valid: the scene
 * shows a labelled "missing art" plate for it.
 *
 * <pre>
 * background.&lt;place&gt;                = backgrounds/classroom.png
 * background.&lt;place&gt;.&lt;variant&gt;      = backgrounds/classroom-evening.png   (optional, e.g. a time of day)
 * foreground.&lt;place&gt;                = backgrounds/classroom-desks.png     (optional, drawn in front of characters)
 * character.&lt;id&gt;.name               = Mr. Reward
 * character.&lt;id&gt;.height             = 640        (display height on the 1280x720 canvas)
 * character.&lt;id&gt;.anchor             = 0.5, 1.0   (point in the image that stands on the slot, 0..1)
 * character.&lt;id&gt;.expression.&lt;name&gt;  = characters/reward/neutral.png
 * texture.&lt;name&gt;                    = textures/grain.png
 * grade.&lt;name&gt;                      = #3B4FB8, MULTIPLY, 0.45, -0.25, -0.10
 * slide.&lt;name&gt;                      = slides/gr11-review/07-file-structure.png  (a lecture cues it by name)
 * bgm.&lt;cue&gt;                         = audio/bgm/title.m4a   (looping music; .wav .mp3 .m4a .aiff)
 * sfx.&lt;name&gt;                        = audio/sfx/confirm.wav (short sound effect)
 * </pre>
 */
public final class AssetManifest {
    /** One character's display settings and its expressions (name to file). */
    public record CharacterSpec(String id, String name, double height, double anchorX, double anchorY,
                                Map<String, String> expressions) {
        public CharacterSpec {
            expressions = Map.copyOf(expressions);
        }
    }

    /**
     * A hand-drawn frame sequence (e.g. a screen transition). {@code pattern} contains one
     * {@code %02d} (or %03d) replaced by the frame number from 0; the screen behind is swapped
     * when frame {@code swapAt} shows.
     */
    public record SequenceSpec(String name, String pattern, int count, double frameMillis, int swapAt) {
        public List<String> files() {
            var files = new ArrayList<String>();
            for (int i = 0; i < count; i++) files.add(String.format(java.util.Locale.ROOT, pattern, i));
            return List.copyOf(files);
        }
    }

    /**
     * A lecture slide. Lecture text turns to it with the cue {@code [slide:name]} at the start
     * of a line (see the dialogue format); the manifest only says which image it is.
     */
    public record Slide(String name, String file) {}

    public static final String RESOURCE = "/com/aphs/classroomuprising/assets/manifest.properties";
    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Pattern FILE = Pattern.compile("[a-z0-9_-]+(/[a-z0-9_-]+)*\\.(png|jpg|jpeg)");
    private static final Pattern AUDIO = Pattern.compile("[a-z0-9_-]+(/[a-z0-9_-]+)*\\.(wav|mp3|m4a|aif|aiff)");

    private final Map<String, String> files;
    private final Map<String, CharacterSpec> characters;
    private final Map<String, Grade> grades;
    private final Map<String, SequenceSpec> sequences;
    private final Map<String, String> audio;
    private final Map<String, Slide> slides;

    private AssetManifest(Map<String, String> files, Map<String, CharacterSpec> characters, Map<String, Grade> grades,
                          Map<String, SequenceSpec> sequences, Map<String, String> audio, Map<String, Slide> slides) {
        this.files = Map.copyOf(files);
        this.characters = Map.copyOf(characters);
        this.grades = Map.copyOf(grades);
        this.sequences = Map.copyOf(sequences);
        this.audio = Map.copyOf(audio);
        this.slides = Map.copyOf(slides);
    }

    /** Reads the manifest bundled with the game. */
    public static AssetManifest bundled() throws IOException {
        var stream = AssetManifest.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new IOException("Missing " + RESOURCE);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    /** Parses a manifest; any malformed or unknown entry is an error, never silently skipped. */
    public static AssetManifest read(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        var files = new LinkedHashMap<String, String>();
        var grades = new LinkedHashMap<String, Grade>();
        var names = new LinkedHashMap<String, String>();
        var heights = new LinkedHashMap<String, Double>();
        var anchors = new LinkedHashMap<String, double[]>();
        var expressions = new LinkedHashMap<String, Map<String, String>>();
        var sequenceFields = new LinkedHashMap<String, Map<String, String>>();
        var audio = new LinkedHashMap<String, String>();
        var slides = new LinkedHashMap<String, Slide>();
        try {
            for (String key : new TreeSet<>(p.stringPropertyNames())) {
                String value = p.getProperty(key).strip();
                String[] part = key.split("\\.");
                switch (part[0]) {
                    case "background", "foreground", "texture", "ui" -> {
                        if (part.length < 2 || part.length > 3 || (part.length == 3 && !part[0].equals("background")))
                            throw new IllegalArgumentException("Unexpected key " + key);
                        for (int i = 1; i < part.length; i++) name(part[i], key);
                        files.put(key, file(value, key));
                    }
                    case "bgm", "sfx" -> {
                        if (part.length != 2) throw new IllegalArgumentException("Unexpected key " + key);
                        name(part[1], key);
                        audio.put(key, audio(value, key));
                    }
                    case "slide" -> {
                        if (part.length != 2) throw new IllegalArgumentException("Unexpected key " + key);
                        String name = name(part[1], key);
                        slides.put(name, new Slide(name, file(value, key)));
                    }
                    case "grade" -> {
                        if (part.length != 2) throw new IllegalArgumentException("Unexpected key " + key);
                        grades.put(name(part[1], key), Grade.parse(value));
                    }
                    case "sequence" -> {
                        if (part.length != 3 || !List.of("frames", "count", "frame-ms", "swap-at").contains(part[2]))
                            throw new IllegalArgumentException("Unexpected key " + key);
                        sequenceFields.computeIfAbsent(name(part[1], key), k -> new LinkedHashMap<>()).put(part[2], value);
                    }
                    case "character" -> {
                        if (part.length < 3) throw new IllegalArgumentException("Unexpected key " + key);
                        String id = name(part[1], key);
                        switch (part[2]) {
                            case "name" -> names.put(id, value);
                            case "height" -> heights.put(id, positive(value, key));
                            case "anchor" -> anchors.put(id, anchor(value, key));
                            case "expression" -> {
                                if (part.length != 4) throw new IllegalArgumentException("Unexpected key " + key);
                                expressions.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(name(part[3], key), file(value, key));
                            }
                            default -> throw new IllegalArgumentException("Unexpected key " + key);
                        }
                    }
                    default -> throw new IllegalArgumentException("Unexpected key " + key);
                }
            }
            var characters = new LinkedHashMap<String, CharacterSpec>();
            var ids = new TreeSet<String>();
            ids.addAll(names.keySet()); ids.addAll(heights.keySet()); ids.addAll(anchors.keySet()); ids.addAll(expressions.keySet());
            for (String id : ids) {
                if (!names.containsKey(id) || !heights.containsKey(id) || !anchors.containsKey(id)
                        || !expressions.getOrDefault(id, Map.of()).containsKey("neutral"))
                    throw new IllegalArgumentException("Character " + id + " needs name, height, anchor and a neutral expression.");
                double[] a = anchors.get(id);
                characters.put(id, new CharacterSpec(id, names.get(id), heights.get(id), a[0], a[1], expressions.get(id)));
            }
            var sequences = new LinkedHashMap<String, SequenceSpec>();
            sequenceFields.forEach((name, fields) -> sequences.put(name, sequence(name, fields)));
            return new AssetManifest(files, characters, grades, sequences, audio, slides);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid asset manifest: " + e.getMessage(), e);
        }
    }

    /** The file for a background, preferring the variant (e.g. a time of day) when one is listed. */
    public Optional<String> background(String place, String variant) {
        if (variant != null && files.containsKey("background." + place + "." + variant))
            return Optional.of(files.get("background." + place + "." + variant));
        return Optional.ofNullable(files.get("background." + place));
    }

    public Optional<String> foreground(String place) { return Optional.ofNullable(files.get("foreground." + place)); }

    public Optional<String> texture(String name) { return Optional.ofNullable(files.get("texture." + name)); }

    /** Optional artwork for an interface part (e.g. "clock-face"); code draws a fallback when absent. */
    public Optional<String> ui(String name) { return Optional.ofNullable(files.get("ui." + name)); }

    public Optional<Grade> grade(String name) { return Optional.ofNullable(grades.get(name)); }

    public Optional<CharacterSpec> character(String id) { return Optional.ofNullable(characters.get(id)); }

    public Optional<SequenceSpec> sequence(String name) { return Optional.ofNullable(sequences.get(name)); }

    private static SequenceSpec sequence(String name, Map<String, String> fields) {
        String key = "sequence." + name;
        String pattern = fields.get("frames");
        if (pattern == null || !fields.containsKey("count"))
            throw new IllegalArgumentException(key + " needs frames and count.");
        if (!pattern.matches("[^%]*%0[23]d[^%]*")) throw new IllegalArgumentException(key + ".frames needs one %02d or %03d.");
        file(String.format(java.util.Locale.ROOT, pattern, 0), key + ".frames");
        int count = Integer.parseInt(fields.get("count"));
        double frameMillis = Double.parseDouble(fields.getOrDefault("frame-ms", "50"));
        int swapAt = Integer.parseInt(fields.getOrDefault("swap-at", Integer.toString(count / 2)));
        if (count < 1 || count > 240 || !(frameMillis >= 16 && frameMillis <= 500) || swapAt < 0 || swapAt >= count)
            throw new IllegalArgumentException(key + " has out-of-range count, frame-ms or swap-at.");
        return new SequenceSpec(name, pattern, count, frameMillis, swapAt);
    }

    /** The slide a lecture cues by this name, if one is listed. */
    public Optional<Slide> slide(String name) { return Optional.ofNullable(slides.get(name)); }

    /** Every listed slide name, sorted. */
    public List<String> slideNames() { return slides.keySet().stream().sorted().toList(); }

    /** The looping music file for a cue (e.g. "title"), if one is listed. */
    public Optional<String> bgm(String cue) { return Optional.ofNullable(audio.get("bgm." + cue)); }

    /** The sound-effect file for a name (e.g. "confirm"), if one is listed. */
    public Optional<String> sfx(String name) { return Optional.ofNullable(audio.get("sfx." + name)); }

    /** The names listed under a sound family ("bgm" or "sfx"), sorted. */
    public List<String> audioNames(String family) {
        return audio.keySet().stream().filter(k -> k.startsWith(family + ".")).map(k -> k.substring(family.length() + 1))
                .sorted().toList();
    }

    /** Finds the character whose dialogue name is {@code name} (e.g. "Mr. Reward"). */
    public Optional<CharacterSpec> characterNamed(String name) {
        return characters.values().stream().filter(c -> c.name().equals(name)).findFirst();
    }

    /** Every file the manifest refers to, for the art checklist. */
    public List<String> allFiles() {
        var all = new ArrayList<>(files.values());
        characters.values().forEach(c -> all.addAll(c.expressions().values()));
        sequences.values().forEach(s -> all.addAll(s.files()));
        all.addAll(audio.values());
        slides.values().forEach(slide -> all.add(slide.file()));
        return all.stream().distinct().sorted().toList();
    }

    private static String name(String text, String key) {
        if (!NAME.matcher(text).matches()) throw new IllegalArgumentException("Bad name '" + text + "' in " + key);
        return text;
    }

    private static String file(String text, String key) {
        if (!FILE.matcher(text).matches())
            throw new IllegalArgumentException("Bad file '" + text + "' in " + key + " (lower-case relative .png/.jpg path)");
        return text;
    }

    private static String audio(String text, String key) {
        if (!AUDIO.matcher(text).matches())
            throw new IllegalArgumentException("Bad file '" + text + "' in " + key
                    + " (lower-case relative .wav/.mp3/.m4a/.aiff path)");
        return text;
    }

    private static double positive(String text, String key) {
        double value = Double.parseDouble(text);
        if (!(value > 0 && value <= 2000)) throw new IllegalArgumentException("Bad number in " + key);
        return value;
    }

    private static double[] anchor(String text, String key) {
        double[] values = Arrays.stream(text.split(",")).map(String::strip).mapToDouble(Double::parseDouble).toArray();
        if (values.length != 2 || Arrays.stream(values).anyMatch(v -> !(v >= 0 && v <= 1)))
            throw new IllegalArgumentException("Anchor must be two numbers from 0 to 1 in " + key);
        return values;
    }
}
