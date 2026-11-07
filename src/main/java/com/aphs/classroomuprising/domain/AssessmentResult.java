package com.aphs.classroomuprising.domain;

public class AssessmentResult {
    private final String assessmentId;
    private final boolean passed;
    private final int score;
    private final String feedback;

    public AssessmentResult(String assessmentId, boolean passed, int score, String feedback) {
        this.assessmentId = assessmentId;
        this.passed = passed;
        this.score = score;
        this.feedback = feedback;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public boolean isPassed() {
        return passed;
    }

    public int getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}
