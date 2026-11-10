package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * A Lab or Test on the preset schedule.
 *
 * @author TeamPike
 * @since 1.0
 */
public class ScheduledAssessment {

    private final String id;
    private final int day;
    private final ArrayList<String> prerequisiteStoryIds;

    /**
     * Creates a scheduled assessment.
     *
     * @param id the unique assessment ID
     * @param day the day the assessment is due
     * @param prerequisiteStoryIds the stories that must be completed first
     */
    public ScheduledAssessment(String id, int day, ArrayList<String> prerequisiteStoryIds) {
        this.id = id;
        this.day = day;
        this.prerequisiteStoryIds = prerequisiteStoryIds;
    }

    /**
     * Returns the assessment ID.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the day the assessment is due.
     *
     * @return the day number
     */
    public int getDay() {
        return day;
    }

    /**
     * Returns the stories that must be completed first.
     *
     * @return the prerequisite story IDs
     */
    public ArrayList<String> getPrerequisiteStoryIds() {
        return prerequisiteStoryIds;
    }
}
