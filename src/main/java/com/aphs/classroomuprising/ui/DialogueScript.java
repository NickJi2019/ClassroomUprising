package com.aphs.classroomuprising.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits authored text into dialogue lines for display. Content authors write one line per
 * line break, optionally prefixed with "Speaker: " or "Speaker[expression]: ".
 * The expression tag (for example {@code Mr. Reward[smile]:}) selects character art; the
 * format must be agreed with the content authors. This is presentation only.
 *
 * <p>A lecture line may start with a slide cue, {@code [slide:file-structure]}: the projector
 * turns to that slide (listed in the asset manifest as {@code slide.file-structure}) when the
 * line begins, and keeps it until another cue. The cue is not shown. It is recognised only at
 * the very start of the text, so code such as {@code String[] args} later in a line is left alone.</p>
 */
final class DialogueScript {
    /**
     * @param speaker     name shown in the name tag
     * @param expression  requested art expression, or null
     * @param text        the line itself, without any slide cue
     * @param characterId art id of the speaker (filled in by the screen layer), or null
     * @param slide       the slide this line turns to, or null to keep the current one
     */
    record Line(String speaker, String expression, String text, String characterId, String slide) {
        Line(String speaker, String text) { this(speaker, null, text, null, null); }
    }

    private static final Pattern SPEAKER =
            Pattern.compile("^([A-Z][A-Za-z.' ]{0,30}?)(?:\\[([a-z0-9_-]{1,24})])?:\\s+(.+)$");
    private static final Pattern SLIDE = Pattern.compile("^\\[slide:([a-z0-9][a-z0-9_-]{0,47})]\\s*(.+)$");

    private DialogueScript() {}

    /** Parses authored text; see the class comment for the line format. */
    static List<Line> parse(String raw, String fallbackSpeaker) {
        var lines = new ArrayList<Line>();
        for (String part : raw.split("\\R")) {
            String line = part.strip();
            if (line.isEmpty()) continue;
            var match = SPEAKER.matcher(line);
            boolean named = match.matches();
            String speaker = named ? match.group(1).strip() : fallbackSpeaker;
            String expression = named ? match.group(2) : null;
            String text = named ? match.group(3).strip() : line;
            var cue = SLIDE.matcher(text);
            boolean cued = cue.matches();
            String slide = cued ? cue.group(1) : null;
            if (cued) text = cue.group(2).strip();
            lines.add(new Line(speaker, expression, text, null, slide));
        }
        return List.copyOf(lines);
    }
}
