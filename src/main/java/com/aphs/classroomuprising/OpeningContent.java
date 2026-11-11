package com.aphs.classroomuprising;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Authored prologue, separate from this summer's lessons, grades and normal combat policy. */
public final class OpeningContent {
    public record Option(String id, String text) { }
    public record QuestionView(String id, String prompt, String code, List<Option> options) {
        public QuestionView { options = List.copyOf(options); }
    }
    public record Source(String document, int question, String pdfPages, int answerKeyPdfPage,
                         String selectionReason) { }
    record Question(QuestionView view, String answer, String explanation, Source source) { }

    private final Map<String, Question> questions;
    private final StudentState initialStudent;
    private final double duckRecovery;
    private final Duration duckDuration;
    private final List<String> intro, defeat, victory;

    private OpeningContent(Map<String, Question> questions, double san, double vibe, List<Double> knowledge,
                           double duckRecovery, Duration duckDuration,
                           List<String> intro, List<String> defeat, List<String> victory) {
        this.questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
        this.initialStudent = new StudentState(9, san, vibe, knowledge);
        this.duckRecovery = duckRecovery; this.duckDuration = duckDuration;
        this.intro = List.copyOf(intro); this.defeat = List.copyOf(defeat); this.victory = List.copyOf(victory);
    }
    public static OpeningContent load(String resource) throws IOException {
        if (resource == null || !resource.startsWith("/demo/")) throw new IOException("Expected a /demo/ classpath resource.");
        var stream = OpeningContent.class.getResourceAsStream(resource);
        if (stream == null) throw new IOException("Missing opening resource: " + resource);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) { return read(reader); }
    }
    /** Strict authoring format. Only Demo SAN/XP and duck amounts are provisional; HP starts at 9. */
    public static OpeningContent read(Reader reader) throws IOException {
        var p = new Properties(); p.load(reader); var f = new Fields(p);
        try {
            if (!f.text("profile.label").startsWith("DEMO")) throw new IllegalArgumentException("Demo label required.");
            double san = f.number("initial.san"), vibe = f.number("initial.codingVibe");
            var knowledge = Arrays.stream(f.text("initial.knowledge").split(",", -1)).map(Double::valueOf).toList();
            double heal = f.number("duck.hp");
            long seconds = Long.parseLong(f.text("duck.seconds"));
            if (heal <= 0 || seconds <= 0) throw new IllegalArgumentException("Duck recovery and duration must be positive.");
            var duration = Duration.ofSeconds(seconds); duration.toNanos();
            var questions = new LinkedHashMap<String, Question>();
            for (String id : f.text("question.ids").split(",", -1)) {
                if (id.isBlank() || !id.equals(id.strip())) throw new IllegalArgumentException("Invalid question id.");
                String key = "question." + id + ".";
                var options = new ArrayList<Option>();
                for (String option : List.of("A", "B", "C", "D", "E"))
                    options.add(new Option(option, f.text(key + "option." + option)));
                String answer = f.text(key + "answer");
                if (options.stream().noneMatch(o -> o.id().equals(answer))) throw new IllegalArgumentException("Unknown answer.");
                var source = new Source(f.text(key + "source"), Integer.parseInt(f.text(key + "number")),
                        f.text(key + "pages"), Integer.parseInt(f.text(key + "keyPage")), f.text(key + "reason"));
                if (source.question() < 1 || source.answerKeyPdfPage() < 1) throw new IllegalArgumentException("Invalid source page/number.");
                var q = new Question(new QuestionView(id, f.text(key + "prompt"), f.text(key + "code"), options),
                        answer, f.text(key + "explanation"), source);
                if (questions.put(id, q) != null) throw new IllegalArgumentException("Duplicate question id.");
            }
            if (questions.size() != 10) throw new IllegalArgumentException("The opening requires exactly ten questions.");
            var value = new OpeningContent(questions, san, vibe, knowledge, heal, duration,
                    f.lines("story.intro"), f.lines("story.defeat"), f.lines("story.victory"));
            f.finish(); return value;
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new IOException("Invalid opening content: " + e.getMessage(), e);
        }
    }
    public StudentState initialStudent() { return initialStudent; }
    public double duckRecovery() { return duckRecovery; }
    public Duration duckDuration() { return duckDuration; }
    public List<String> intro() { return intro; }
    public List<String> defeat() { return defeat; }
    public List<String> victory() { return victory; }
    /** No answer keys/explanations are exposed before a response is submitted. */
    public List<QuestionView> questions() { return questions.values().stream().map(Question::view).toList(); }
    public Source source(String id) { return question(id).source(); }
    Question question(String id) {
        var q = questions.get(id);
        if (q == null) throw new IllegalArgumentException("Unknown opening question: " + id);
        return q;
    }
    private static final class Fields {
        final Properties values; final Set<String> used = new HashSet<>();
        Fields(Properties values) { this.values = values; }
        String text(String key) {
            String value = values.getProperty(key);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing value: " + key);
            used.add(key); return value.strip();
        }
        double number(String key) { double n = Double.parseDouble(text(key)); StatChange.requireFinite(n); return n; }
        List<String> lines(String key) {
            var lines = text(key).lines().toList();
            if (lines.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Empty story line.");
            return lines;
        }
        void finish() {
            var extra = new TreeSet<>(values.stringPropertyNames()); extra.removeAll(used);
            if (!extra.isEmpty()) throw new IllegalArgumentException("Unknown opening values: " + extra);
        }
    }
}
