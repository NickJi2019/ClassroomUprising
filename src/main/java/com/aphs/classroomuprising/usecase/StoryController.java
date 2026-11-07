package com.aphs.classroomuprising.usecase;

import com.aphs.classroomuprising.domain.*;

public class StoryController {
    private StoryEvent activeEvent;
    private int sceneIndex;

    public void refreshUnlocks(GameState state) {}
    public void playIntroduction(GameState state) {}
    public void playDueStories(GameState state) {}
    public void advanceScene(GameState state) {}
    public boolean hasPendingRequiredStory(GameState state) { return false; }
}
