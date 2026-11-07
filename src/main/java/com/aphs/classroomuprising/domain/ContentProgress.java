package com.aphs.classroomuprising.domain;

import java.util.ArrayList;

public class ContentProgress {
    private boolean introductionCompleted;
    private ArrayList<String> unlockedTopicIds;
    private ArrayList<String> availableStoryIds;
    private ArrayList<String> completedStoryIds;
    private ArrayList<String> resolvedAssessmentIds;

    public boolean isTopicUnlocked(String id) { return false; }
    public boolean isStoryCompleted(String id) { return false; }
    public boolean isAssessmentResolved(String id) { return false; }
}
