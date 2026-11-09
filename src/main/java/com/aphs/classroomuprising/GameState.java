package com.aphs.classroomuprising;

import java.util.*;

/** Internal mutable aggregate. Controllers mutate a copy and commit only complete operations. */
public final class GameState {
    public enum Status { ACTIVE, MIDTERM_DECISION, FINAL_DECISION, DROPPED }
    String name;
    int day = 1;
    DayPeriod period = DayPeriod.ICS_CLASS;
    StudentState student;
    GradeBook grades = new GradeBook();
    RunProgress run = new RunProgress(0);
    Status status = Status.ACTIVE;
    boolean openingCompleted, lessonShown, nextMorningBonus;
    double nextMorningSanBonus;
    List<TimedEffect> effects = new ArrayList<>();
    Map<Integer, Integer> makeups = new LinkedHashMap<>();
    Set<Integer> missedAssessments = new LinkedHashSet<>();
    Set<String> seriousCheating = new LinkedHashSet<>();
    Set<String> feedbackApplied = new LinkedHashSet<>();
    Map<String, Double> estimates = new LinkedHashMap<>();
    Set<String> rewards = new LinkedHashSet<>();
    Map<String, Double> relationships = new LinkedHashMap<>();
    ActiveAssessment active;
    record ActiveAssessment(DayPlan original, List<ContentCatalog.Question> questions, boolean makeup) {
        ActiveAssessment { questions = List.copyOf(questions); }
    }

    public GameState(String name, StudentState student) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Student name required.");
        this.name = name; this.student = Objects.requireNonNull(student);
    }
    GameState copy() {
        var s = new GameState(name, student);
        s.day = day; s.period = period; s.grades = grades.copy(); s.run = run; s.status = status;
        s.openingCompleted = openingCompleted; s.lessonShown = lessonShown;
        s.nextMorningBonus = nextMorningBonus; s.nextMorningSanBonus = nextMorningSanBonus;
        s.effects = new ArrayList<>(effects); s.makeups = new LinkedHashMap<>(makeups);
        s.missedAssessments = new LinkedHashSet<>(missedAssessments);
        s.seriousCheating = new LinkedHashSet<>(seriousCheating);
        s.feedbackApplied = new LinkedHashSet<>(feedbackApplied); s.estimates = new LinkedHashMap<>(estimates);
        s.rewards = new LinkedHashSet<>(rewards); s.relationships = new LinkedHashMap<>(relationships);
        s.active = active; return s;
    }
    public String name() { return name; }
    public int day() { return day; }
    public DayPeriod period() { return period; }
    public StudentState student() { return student; }
    public GradeBook grades() { return grades.copy(); }
    public RunProgress run() { return run; }
    public Status status() { return status; }
    public boolean openingCompleted() { return openingCompleted; }
    public boolean hasMorningBonus() { return nextMorningBonus; }
    public List<TimedEffect> effects() { return List.copyOf(effects); }
    public Map<Integer, Integer> makeups() { return Map.copyOf(makeups); }
    public Set<Integer> missedAssessments() { return Set.copyOf(missedAssessments); }
    public Map<String, Double> estimates() { return Map.copyOf(estimates); }
    public Set<String> rewards() { return Set.copyOf(rewards); }
    public Map<String, Double> relationships() { return Map.copyOf(relationships); }
    public double effectiveSan() {
        double v = student.san() + effects.stream().filter(e -> e.activeOn(day)).mapToDouble(TimedEffect::sanDelta).sum();
        StatChange.requireFinite(v); return v;
    }
    public double xpMultiplier() {
        double result = 1;
        for (var e : effects) if (e.activeOn(day)) result *= e.xpMultiplier();
        StatChange.requireFinite(result); return result;
    }
    public DayPlan currentCourse() { return SchoolCalendar.day(makeups.getOrDefault(day, day)); }
    public boolean isTutorial() { return day <= 2; }
    public boolean canSave() { return openingCompleted && period == DayPeriod.DAY_END && status == Status.ACTIVE && active == null; }
}
