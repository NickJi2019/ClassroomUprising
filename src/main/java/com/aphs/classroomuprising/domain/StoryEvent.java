package com.aphs.classroomuprising.domain;

import java.util.ArrayList;

public class StoryEvent {
    private String id;
    private int unlockDay;
    private DayPeriod triggerPeriod;
    private boolean required;
    private ArrayList<String> prerequisiteIds;
    private ArrayList<String> scenes;
    private StatChange completionEffect;

    public boolean isEligible(GameState state) { return false; }
}
