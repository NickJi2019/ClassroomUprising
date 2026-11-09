package com.aphs.classroomuprising.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits authored text into dialogue lines for display. Content authors write one line per
 * line break, optionally prefixed with "Speaker: ". This is presentation only.
 */
final class DialogueScript {
    record Line(String speaker, String text) {}

    private static final Pattern SPEAKER = Pattern.compile("^([A-Z][A-Za-z.' ]{0,30}):\\s+(.+)$");

    private DialogueScript() {}

    static List<Line> parse(String raw, String fallbackSpeaker) {
        var lines = new ArrayList<Line>();
        for (String part : raw.split("\\R")) {
            String line = part.strip();
            if (line.isEmpty()) continue;
            var match = SPEAKER.matcher(line);
            if (match.matches()) lines.add(new Line(match.group(1).strip(), match.group(2).strip()));
            else lines.add(new Line(fallbackSpeaker, line));
        }
        return List.copyOf(lines);
    }
}
