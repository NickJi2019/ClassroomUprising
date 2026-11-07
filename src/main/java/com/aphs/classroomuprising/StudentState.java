package com.aphs.classroomuprising;

/**
 * The student's name and four stats.
 *
 * @author TeamPike
 * @since 1.0
 */
public class StudentState {

    private final String name;
    private int mentalState;
    private int icsKnowledge;
    private int otherKnowledge;
    private int gradeHp;

    /**
     * Creates a student. Starting values are passed in because they are a balancing decision.
     *
     * @param name the student's name
     * @param mentalState the starting mental state
     * @param icsKnowledge the starting ICS4U knowledge
     * @param otherKnowledge the starting other-course knowledge
     * @param gradeHp the starting grade, which acts as HP
     */
    public StudentState(String name, int mentalState, int icsKnowledge, int otherKnowledge, int gradeHp) {
        this.name = name;
        this.mentalState = mentalState;
        this.icsKnowledge = icsKnowledge;
        this.otherKnowledge = otherKnowledge;
        this.gradeHp = gradeHp;
    }

    /**
     * Applies a stat change to this student.
     *
     * @param change the amounts to add to each stat
     */
    public void apply(StatChange change) {
        throw new UnsupportedOperationException("StudentState.apply not implemented yet");
    }

    /**
     * Checks whether the grade (HP) has run out.
     *
     * @return true if the grade is depleted
     */
    public boolean isGradeDepleted() {
        throw new UnsupportedOperationException("StudentState.isGradeDepleted not implemented yet");
    }

    /**
     * Returns the student's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the mental state.
     *
     * @return the mental state
     */
    public int getMentalState() {
        return mentalState;
    }

    /**
     * Returns the ICS4U knowledge.
     *
     * @return the ICS4U knowledge
     */
    public int getIcsKnowledge() {
        return icsKnowledge;
    }

    /**
     * Returns the other-course knowledge.
     *
     * @return the other-course knowledge
     */
    public int getOtherKnowledge() {
        return otherKnowledge;
    }

    /**
     * Returns the grade, which acts as HP.
     *
     * @return the grade
     */
    public int getGradeHp() {
        return gradeHp;
    }
}
