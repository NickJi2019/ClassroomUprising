package com.aphs.classroomuprising;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/** Content supplied by the team; no invented lessons, rewards or question pools are installed. */
public final class ContentCatalog {
    public enum Interaction { ASK_TEACHER, RAISE_HAND }
    public record Lesson(String text, Interaction interaction, Map<String, StatChange> choices,
                         String title, LessonDecision decision) {
        /** Compatibility for explicit legacy/fixture catalogs; the shipped Demo uses authored decisions. */
        public Lesson(String text, Interaction interaction, Map<String, StatChange> choices) {
            this(text, interaction, choices, "", null);
        }
        public Lesson(String title, String text, Interaction interaction, LessonDecision decision) {
            this(text, interaction, Objects.requireNonNull(decision).effects(), title, decision);
        }
        public Lesson {
            Objects.requireNonNull(text); Objects.requireNonNull(interaction);
            Objects.requireNonNull(title);
            choices = Map.copyOf(choices);
            if (text.isBlank() || choices.isEmpty()) throw new IllegalArgumentException("Lesson content/choices required.");
            if (decision != null && (title.isBlank() || !choices.equals(decision.effects())))
                throw new IllegalArgumentException("Authored lessons require a title and the decision's exact effects.");
        }
    }
    public record Question(String id, String prompt, Integer difficultyRank) {
        public Question(String id, String prompt) { this(id, prompt, null); }
        public Question {
            if (id == null || id.isBlank() || prompt == null || prompt.isBlank())
                throw new IllegalArgumentException("Question id and prompt required.");
            if (difficultyRank != null && difficultyRank < 1)
                throw new IllegalArgumentException("Difficulty rank must be positive.");
        }
    }
    private final OpeningContent opening;
    private final Map<Integer, Lesson> lessons;
    private final Map<String, StatChange> hub;
    private final Map<Integer, StatChange> reviews;
    private final Map<String, List<Question>> banks;
    private final Map<CourseType, Integer> sampleCounts;
    private final StatChange otherClassRevision;

    public ContentCatalog(Map<Integer, Lesson> lessons, Map<String, StatChange> hub,
            Map<Integer, StatChange> reviews, Map<String, List<Question>> banks,
            Map<CourseType, Integer> sampleCounts, StatChange otherClassRevision) {
        this(lessons, hub, reviews, banks, sampleCounts, otherClassRevision, null);
    }
    public ContentCatalog(Map<Integer, Lesson> lessons, Map<String, StatChange> hub,
            Map<Integer, StatChange> reviews, Map<String, List<Question>> banks,
            Map<CourseType, Integer> sampleCounts, StatChange otherClassRevision, OpeningContent opening) {
        this.opening = opening;
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
    public boolean hasOpeningBattle() { return opening != null; }
    public OpeningContent opening() {
        if (opening == null) throw new PendingRuleException("T11", "Opening story and exam content");
        return opening;
    }
    public static ContentCatalog pending() {
        return new ContentCatalog(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), null);
    }
    public Lesson lesson(int day) {
        var value = lessons.get(day);
        if (value == null) throw new PendingRuleException("T14", "Lesson content for day " + day);
        return value;
    }
    /** Content availability, not a replacement for the authoritative forty-day SchoolCalendar. */
    public List<Integer> lessonDays() { return lessons.keySet().stream().sorted().toList(); }
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
