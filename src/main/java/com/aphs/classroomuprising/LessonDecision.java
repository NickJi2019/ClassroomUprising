package com.aphs.classroomuprising;

import java.util.*;

/** Authored post-lecture choices. Opening the response menu is presentation, not a second reward. */
public record LessonDecision(String prompt, List<Option> options) {
    public enum Outcome { APPROPRIATE, INAPPROPRIATE, SKIP }

    public record Option(String id, String label, Outcome outcome, StatChange effect, String feedback) {
        public Option {
            requireText(id, "Option ID"); requireText(label, "Option label"); requireText(feedback, "Feedback");
            if (!id.matches("[A-Za-z0-9][A-Za-z0-9._-]*"))
                throw new IllegalArgumentException("Option ID must be a stable identifier.");
            Objects.requireNonNull(outcome); Objects.requireNonNull(effect);
            if (outcome == Outcome.SKIP && !effect.equals(StatChange.none()))
                throw new IllegalArgumentException("Staying quiet must have no effect.");
            if (outcome == Outcome.INAPPROPRIATE && (effect.hp() != 0 || effect.san() >= 0
                    || effect.codingVibe() != 0 || effect.unitKnowledge().stream().anyMatch(x -> x != 0)))
                throw new IllegalArgumentException("An inappropriate option only reduces SAN; it grants no bonus.");
            if (outcome == Outcome.APPROPRIATE && (effect.codingVibe() < 0
                    || effect.unitKnowledge().stream().anyMatch(x -> x < 0)
                    || (effect.codingVibe() == 0 && effect.unitKnowledge().stream().allMatch(x -> x == 0))))
                throw new IllegalArgumentException("An appropriate option must grant a learning bonus.");
        }
    }

    public LessonDecision {
        requireText(prompt, "Decision prompt"); options = List.copyOf(options);
        if (options.stream().map(Option::id).distinct().count() != options.size())
            throw new IllegalArgumentException("Option IDs must be distinct within a lesson.");
        if (options.stream().filter(o -> o.outcome() == Outcome.SKIP).count() != 1
                || options.stream().noneMatch(o -> o.outcome() == Outcome.APPROPRIATE)
                || options.stream().noneMatch(o -> o.outcome() == Outcome.INAPPROPRIATE))
            throw new IllegalArgumentException("Provide one skip, an appropriate response and an inappropriate response.");
    }

    /** Authored order, excluding the first-level silent choice. Does not reveal or apply a new reward. */
    public List<Option> responses() { return options.stream().filter(o -> o.outcome() != Outcome.SKIP).toList(); }
    public Option skip() { return options.stream().filter(o -> o.outcome() == Outcome.SKIP).findFirst().orElseThrow(); }
    public Option option(String id) {
        return options.stream().filter(o -> o.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown classroom option."));
    }
    public Map<String, StatChange> effects() {
        var values = new LinkedHashMap<String, StatChange>();
        options.forEach(o -> values.put(o.id(), o.effect()));
        return Collections.unmodifiableMap(values);
    }
    private static void requireText(String text, String name) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(name + " required.");
    }
}
