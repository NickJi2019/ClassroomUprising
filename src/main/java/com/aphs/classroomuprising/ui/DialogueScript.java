package com.aphs.classroomuprising.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits authored text into dialogue lines for display. Content authors write one line per
 * line break, optionally prefixed with "Speaker: " or "Speaker[expression]: ".
 * The expression tag (for example {@code Mr. Reward[smile]:}) selects character art; the
 * format must be agreed with the content authors. This is presentation only.
 */
final class DialogueScript {
    /**
     * @param speaker     name shown in the name tag
     * @param expression  requested art expression, or null
     * @param text        the line itself
     * @param characterId art id of the speaker (filled in by the screen layer), or null
     */
    record Line(String speaker, String expression, String text, String characterId) {
        Line(String speaker, String text) { this(speaker, null, text, null); }
    }

    private static final Pattern SPEAKER =
            Pattern.compile("^([A-Z][A-Za-z.' ]{0,30}?)(?:\\[([a-z0-9_-]{1,24})])?:\\s+(.+)$");

    private DialogueScript() {}

    static List<Line> parse(String raw, String fallbackSpeaker) {
        var lines = new ArrayList<Line>();
        for (String part : raw.split("\\R")) {
            String line = part.strip();
            if (line.isEmpty()) continue;
            var match = SPEAKER.matcher(line);
            if (match.matches()) lines.add(new Line(match.group(1).strip(), match.group(2), match.group(3).strip(), null));
            else lines.add(new Line(fallbackSpeaker, line));
        }
        return List.copyOf(lines);
    }
}
