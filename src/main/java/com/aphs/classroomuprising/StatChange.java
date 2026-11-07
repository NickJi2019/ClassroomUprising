package com.aphs.classroomuprising;

/**
 * The amounts to add to each of the student's four stats. Values may be negative.
 *
 * @author TeamPike
 * @since 1.0
 */
public class StatChange {

    private final int mentalState;
    private final int icsKnowledge;
    private final int otherKnowledge;
    private final int gradeHp;

    /**
     * Creates a stat change.
     *
     * @param mentalState the change to mental state
     * @param icsKnowledge the change to ICS4U knowledge
     * @param otherKnowledge the change to other-course knowledge
     * @param gradeHp the change to the grade (HP)
     */
    public StatChange(int mentalState, int icsKnowledge, int otherKnowledge, int gradeHp) {
        this.mentalState = mentalState;
        this.icsKnowledge = icsKnowledge;
        this.otherKnowledge = otherKnowledge;
        this.gradeHp = gradeHp;
    }

    /**
     * Returns the change to mental state.
     *
     * @return the mental state change
     */
    public int getMentalState() {
        return mentalState;
    }

    /**
     * Returns the change to ICS4U knowledge.
     *
     * @return the ICS4U knowledge change
     */
    public int getIcsKnowledge() {
        return icsKnowledge;
    }

    /**
     * Returns the change to other-course knowledge.
     *
     * @return the other-course knowledge change
     */
    public int getOtherKnowledge() {
        return otherKnowledge;
    }

    /**
     * Returns the change to the grade (HP).
     *
     * @return the grade change
     */
    public int getGradeHp() {
        return gradeHp;
    }
}
