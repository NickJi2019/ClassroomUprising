package com.aphs.classroomuprising;

import java.util.List;
import java.util.OptionalDouble;

/** A submitted, normalized mark from an evaluator; HP is never used as its value. */
public final class AssessmentRecord {
    final String id;
    final CourseType type;
    final int submittedDay, returnDay;
    final List<String> questionIds;
    final boolean makeup;
    final double score;
    final boolean released;
    final String evidence;
    AssessmentRecord(String id, CourseType type, int submittedDay, int returnDay,
                     List<String> questionIds, boolean makeup, double score, boolean released, String evidence) {
        if (id == null || id.isBlank() || type == null || !type.isAssessment()
                || submittedDay < 1 || submittedDay > 40 || returnDay != submittedDay + (type.isMajor() ? 0 : 3)
                || !Double.isFinite(score) || score < 0 || score > 100 || evidence == null)
            throw new IllegalArgumentException("Invalid evaluated assessment record.");
        this.id = id; this.type = type; this.submittedDay = submittedDay; this.returnDay = returnDay;
        this.questionIds = List.copyOf(questionIds); this.makeup = makeup; this.score = score;
        this.released = released; this.evidence = evidence;
        if (this.questionIds.isEmpty() || this.questionIds.stream().distinct().count() != this.questionIds.size())
            throw new IllegalArgumentException("Distinct question ids required.");
    }
    public String id() { return id; }
    public CourseType type() { return type; }
    public int submittedDay() { return submittedDay; }
    public int returnDay() { return returnDay; }
    public List<String> questionIds() { return questionIds; }
    public boolean makeup() { return makeup; }
    public OptionalDouble officialScore() { return released ? OptionalDouble.of(score) : OptionalDouble.empty(); }
    AssessmentRecord publish() {
        return new AssessmentRecord(id, type, submittedDay, returnDay, questionIds, makeup, score, true, evidence);
    }
}
