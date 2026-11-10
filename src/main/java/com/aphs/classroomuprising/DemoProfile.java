package com.aphs.classroomuprising;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.random.RandomGenerator;

/** Explicit opt-in demo data, not production defaults. No frontend, battle or grading policy. */
public record DemoProfile(String label, StudentState initialStudent, RuleValues rules, ContentCatalog content) {
    public DemoProfile {
        if (label == null || !label.startsWith("DEMO")) throw new IllegalArgumentException("Demo label required.");
        Objects.requireNonNull(initialStudent); Objects.requireNonNull(rules); Objects.requireNonNull(content);
    }

    /** Load the UTF-8 demo resource from the classpath, including when packaged in a JAR. */
    public static DemoProfile load() throws IOException {
        var stream = DemoProfile.class.getResourceAsStream("/demo/demo.properties");
        if (stream == null) throw new IOException("Missing demo/demo.properties resource.");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) { return read(reader); }
    }

    /** Read a deliberately selected configuration. The caller owns the reader. Fail rather than guess. */
    public static DemoProfile read(Reader reader) throws IOException {
        var p = new Properties(); p.load(reader);
        var f = new Fields(p);
        try {
            var initial = new StudentState(f.number("initial.hp"), f.number("initial.san"),
                    f.number("initial.codingVibe"), f.numbers("initial.knowledge", 6));
            var rules = new RuleValues(f.number("rules.lateNightSanReduction"), f.number("rules.lateNightXpMultiplier"),
                    f.number("rules.earlySleepSanBonus"), f.number("rules.makeupSanReduction"),
                    f.number("rules.caughtCheatingSanReduction"),
                    RuleValues.TutorialAbsences.valueOf(f.text("rules.tutorialAbsences")),
                    f.number("rules.normalSleepSanRecovery"));
            var lessons = new LinkedHashMap<Integer, ContentCatalog.Lesson>();
            for (String dayText : f.list("lesson.days")) {
                int day = Integer.parseInt(dayText);
                if (SchoolCalendar.day(day).course() != CourseType.LESSON)
                    throw new IllegalArgumentException("Demo lesson must occupy a scheduled Lesson: " + day);
                String key = "lesson." + day + ".";
                var options = new ArrayList<LessonDecision.Option>();
                for (String id : f.list(key + "options")) {
                    String option = key + "option." + id + ".";
                    options.add(new LessonDecision.Option(id, f.text(option + "label"),
                            LessonDecision.Outcome.valueOf(f.text(option + "outcome")),
                            f.effect(option + "effect"), f.text(option + "feedback")));
                }
                var decision = new LessonDecision(f.text(key + "prompt"), options);
                var lesson = new ContentCatalog.Lesson(f.text(key + "title"), f.text(key + "text"),
                        ContentCatalog.Interaction.valueOf(f.text(key + "interaction")), decision);
                if (lessons.put(day, lesson) != null) throw new IllegalArgumentException("Duplicate lesson day.");
            }
            var hub = new LinkedHashMap<String, StatChange>();
            for (String id : f.list("hub.ids")) hub.put(id, f.effect("hub." + id));
            // Assessments and later course content are deliberately absent, not fake evaluated marks.
            var content = new ContentCatalog(lessons, hub, Map.of(), Map.of(), Map.of(), f.effect("other.reviseIcs"));
            var profile = new DemoProfile(f.text("profile.label"), initial, rules, content);
            f.finish();
            return profile;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Demo configuration: " + e.getMessage(), e);
        }
    }

    /** Each call starts a separate Day 1 run; the opening acknowledgement is still required. */
    public GameController newGame(String name, RandomGenerator random) {
        return new GameController(new GameState(name, initialStudent), rules, content, random);
    }

    /** Explicit combat policy injection; the shipped Demo still has no assessment question bank. */
    public GameController newGame(String name, RandomGenerator random, CombatRules combatRules) {
        return new GameController(new GameState(name, initialStudent), rules, content, random, combatRules);
    }

    private static final class Fields {
        private final Properties values;
        private final Set<String> used = new HashSet<>();
        Fields(Properties values) { this.values = values; }
        String text(String key) {
            String value = values.getProperty(key);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing value: " + key);
            used.add(key); return value.strip();
        }
        double number(String key) {
            double value = Double.parseDouble(text(key)); StatChange.requireFinite(value); return value;
        }
        List<String> list(String key) {
            var result = Arrays.stream(text(key).split(",", -1)).map(String::strip).toList();
            if (result.stream().anyMatch(String::isBlank) || new HashSet<>(result).size() != result.size())
                throw new IllegalArgumentException("Empty or duplicate entry: " + key);
            return result;
        }
        List<Double> numbers(String key, int size) {
            var result = Arrays.stream(text(key).split(",", -1)).map(String::strip).map(Double::valueOf).toList();
            if (result.size() != size) throw new IllegalArgumentException("Expected " + size + " numbers: " + key);
            result.forEach(StatChange::requireFinite); return result;
        }
        StatChange effect(String key) {
            var v = numbers(key, 9);
            return new StatChange(v.get(0), v.get(1), v.get(2), v.subList(3, 9));
        }
        void finish() {
            var extra = new TreeSet<>(values.stringPropertyNames()); extra.removeAll(used);
            if (!extra.isEmpty()) throw new IllegalArgumentException("Unknown values: " + extra);
        }
    }
}
