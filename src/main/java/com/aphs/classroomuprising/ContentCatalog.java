package com.aphs.classroomuprising;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Content supplied by the team; no invented lessons, rewards or question pools are installed. */
public final class ContentCatalog {
    public enum Interaction { ASK_TEACHER, RAISE_HAND }
    public record Lesson(String text, Interaction interaction, Map<String, StatChange> choices) {
        public Lesson {
            Objects.requireNonNull(text); Objects.requireNonNull(interaction);
            choices = Map.copyOf(choices);
            if (text.isBlank() || choices.isEmpty()) throw new IllegalArgumentException("Lesson content/choices required.");
        }
    }
    public record Question(String id, String prompt) {
        public Question {
            if (id == null || id.isBlank() || prompt == null || prompt.isBlank())
                throw new IllegalArgumentException("Question id and prompt required.");
        }
    }
    private final Map<Integer, Lesson> lessons;
    private final Map<String, StatChange> hub;
    private final Map<Integer, StatChange> reviews;
    private final Map<String, List<Question>> banks;
    private final Map<CourseType, Integer> sampleCounts;
    private final StatChange otherClassRevision;

    public ContentCatalog(Map<Integer, Lesson> lessons, Map<String, StatChange> hub,
            Map<Integer, StatChange> reviews, Map<String, List<Question>> banks,
            Map<CourseType, Integer> sampleCounts, StatChange otherClassRevision) {
        this.lessons = Map.copyOf(lessons); this.hub = Map.copyOf(hub);
        this.reviews = Map.copyOf(reviews); this.sampleCounts = Map.copyOf(sampleCounts);
        var copy = new java.util.LinkedHashMap<String, List<Question>>();
        banks.forEach((id, questions) -> {
            var q = List.copyOf(questions);
            if (q.isEmpty() || q.stream().map(Question::id).distinct().count() != q.size())
                throw new IllegalArgumentException("A bank requires distinct questions.");
            copy.put(id, q);
        });
        this.banks = Map.copyOf(copy); this.otherClassRevision = otherClassRevision;
    }
    public static ContentCatalog pending() {
        return new ContentCatalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), null);
    }
    public Lesson lesson(int day) {
        var value = lessons.get(day);
        if (value == null) throw new PendingRuleException("T14", "Lesson content for day " + day);
        return value;
    }
    /** Available IDs in stable natural order. This immutable list does not grant activity eligibility. */
    public List<String> hubActivityIds() {
        return hub.keySet().stream().sorted().toList();
    }
    public StatChange hub(String id) {
        var value = hub.get(id);
        if (value == null) throw new PendingRuleException("T04", "Hub activity " + id);
        return value;
    }
    public StatChange review(int day) {
        var value = reviews.get(day);
        if (value == null) throw new PendingRuleException("T14", "Review content for day " + day);
        return value;
    }
    public StatChange otherRevision() {
        if (otherClassRevision == null) throw new PendingRuleException("T04", "OTHER_CLASSES revision effects");
        return otherClassRevision;
    }
    public List<Question> questions(DayPlan original, RandomGenerator random) {
        var bank = banks.get(original.assessmentId());
        if (bank == null) throw new PendingRuleException("T07", "Question bank " + original.assessmentId());
        if (!original.course().isMajor()) return bank;
        Integer count = sampleCounts.get(original.course());
        if (count == null) throw new PendingRuleException("T07", "Major-exam sample size");
        if (count <= 0 || count > bank.size()) throw new IllegalArgumentException("Invalid sample size.");
        var copy = new java.util.ArrayList<>(bank);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            var old = copy.get(i); copy.set(i, copy.get(j)); copy.set(j, old);
        }
        return List.copyOf(copy.subList(0, count));
    }
}
