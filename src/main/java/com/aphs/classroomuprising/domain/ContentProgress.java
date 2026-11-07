package com.aphs.classroomuprising.domain;

import java.util.ArrayList;
import java.util.List;

public class ContentProgress {
    private boolean introductionCompleted;
    private final List<String> unlockedTopicIds;
    private final List<String> availableStoryIds;
    private final List<String> completedStoryIds;
    private final List<String> resolvedAssessmentIds;

    public ContentProgress() {
        this.introductionCompleted = false;
        this.unlockedTopicIds = new ArrayList<>();
        this.availableStoryIds = new ArrayList<>();
        this.completedStoryIds = new ArrayList<>();
        this.resolvedAssessmentIds = new ArrayList<>();
    }

    public boolean isIntroductionCompleted() {
        return introductionCompleted;
    }

    public void setIntroductionCompleted(boolean introductionCompleted) {
        this.introductionCompleted = introductionCompleted;
    }

    public boolean isTopicUnlocked(String id) {
        return unlockedTopicIds.contains(id);
    }

    public void unlockTopic(String id) {
        if (!unlockedTopicIds.contains(id)) {
            unlockedTopicIds.add(id);
        }
    }

    public boolean isStoryCompleted(String id) {
        return completedStoryIds.contains(id);
    }

    public void completeStory(String id) {
        if (!completedStoryIds.contains(id)) {
            completedStoryIds.add(id);
        }
    }

    public boolean isAssessmentResolved(String id) {
        return resolvedAssessmentIds.contains(id);
    }

    public void resolveAssessment(String id) {
        if (!resolvedAssessmentIds.contains(id)) {
            resolvedAssessmentIds.add(id);
        }
    }
}
