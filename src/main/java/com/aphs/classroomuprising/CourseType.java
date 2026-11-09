package com.aphs.classroomuprising;

/** Calendar content, separate from a time-of-day period. */
public enum CourseType {
    LESSON, LAB, UNIT_TEST, REVIEW, MIDTERM, FINAL;
    public boolean isAssessment() {
        return this == LAB || this == UNIT_TEST || this == MIDTERM || this == FINAL;
    }
    public boolean isMajor() { return this == MIDTERM || this == FINAL; }
    public boolean isTermAssessment() { return isAssessment() && this != FINAL; }
}
