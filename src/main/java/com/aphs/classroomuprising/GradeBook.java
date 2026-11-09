package com.aphs.classroomuprising;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owns attendance and submitted marks. The public state accessor returns a defensive copy. */
public final class GradeBook {
    final Map<String, AssessmentRecord> records = new LinkedHashMap<>();
    int unexcusedAbsences;
    public GradeBook() {}
    GradeBook copy() {
        var b = new GradeBook(); b.records.putAll(records); b.unexcusedAbsences = unexcusedAbsences; return b;
    }
    public List<AssessmentRecord> records() { return List.copyOf(records.values()); }
    public int unexcusedAbsences() { return unexcusedAbsences; }
    public int absenceDeductionPoints() { return Math.max(0, unexcusedAbsences - 2); }
    public AssessmentRecord record(String id) {
        var r = records.get(id);
        if (r == null) throw new IllegalArgumentException("No submitted assessment: " + id);
        return r;
    }
    void recordAbsence(boolean excused, boolean tutorial, RuleValues.TutorialAbsences policy) {
        if (excused) return;
        if (tutorial && policy == RuleValues.TutorialAbsences.UNDECIDED)
            throw new PendingRuleException("T11", "Whether tutorial absences consume free absences.");
        if (!tutorial || policy == RuleValues.TutorialAbsences.COUNT) unexcusedAbsences++;
    }
    void submit(AssessmentRecord record) {
        if (records.containsKey(record.id())) throw new IllegalStateException("Assessment already submitted.");
        records.put(record.id(), record);
    }
    List<String> releaseDue(int day) {
        var released = new ArrayList<String>();
        records.replaceAll((id, r) -> {
            if (!r.released && r.returnDay() <= day) { released.add(id); return r.publish(); }
            return r;
        });
        return List.copyOf(released);
    }
    /**
     * Computes the approved component arithmetic only after all marks are official.
     * Midterm's relative weight and Participation's percent must be supplied by an approved policy.
     * No missing-mark zero, cap, average policy or HP conversion is invented here.
     */
    public double finalPercentage(double midtermRelativeWeight, double participationPercent) {
        if (!Double.isFinite(midtermRelativeWeight) || midtermRelativeWeight <= 0
                || !Double.isFinite(participationPercent) || participationPercent < 0 || participationPercent > 100)
            throw new IllegalArgumentException("Explicit valid grading parameters required.");
        double weighted = 0, weight = 0, finalMark = 0;
        for (var day : SchoolCalendar.days()) {
            if (!day.course().isAssessment()) continue;
            var r = records.get(day.assessmentId());
            if (r == null || !r.released)
                throw new PendingRuleException("T02", "All semester marks must be official before final calculation.");
            if (day.course() == CourseType.FINAL) finalMark = r.score;
            else {
                double w = day.course() == CourseType.MIDTERM ? midtermRelativeWeight : 1;
                weighted += r.score * w; weight += w;
            }
        }
        double total = .65 * weighted / weight + .10 * participationPercent + .25 * finalMark
                - absenceDeductionPoints();
        if (total < 0 || total > 100) throw new PendingRuleException("T02", "Grade floor/cap policy required.");
        return total;
    }
}
