package com.aphs.classroomuprising;

import java.util.ArrayList;

/**
 * A story event: its scenes, when it triggers and its one-time effect.
 *
 * @author TeamPike
 * @since 1.0
 */
public class StoryEvent {

    private final String id;
    private final int unlockDay;
    private final DayPeriod triggerPeriod;
    private final boolean required;
    private final ArrayList<String> prerequisiteIds;
    private final ArrayList<String> scenes;
    private final StatChange completionEffect;

    /**
     * Creates a story event.
     *
     * @param id the unique story ID
     * @param unlockDay the first day the story can play
     * @param triggerPeriod the period in which the story plays
     * @param required whether the story blocks the daily routine until finished
     * @param prerequisiteIds the IDs of stories that must be completed first
     * @param scenes the scenes, in the order they play
     * @param completionEffect the effect applied once when the story finishes
     */
    public StoryEvent(String id, int unlockDay, DayPeriod triggerPeriod, boolean required,
                      ArrayList<String> prerequisiteIds, ArrayList<String> scenes, StatChange completionEffect) {
        this.id = id;
        this.unlockDay = unlockDay;
        this.triggerPeriod = triggerPeriod;
        this.required = required;
        this.prerequisiteIds = prerequisiteIds;
        this.scenes = scenes;
        this.completionEffect = completionEffect;
    }

    /**
     * Checks whether the date and prerequisites for this story are met.
     *
     * @param state the current game state
     * @return true if the story can be made available
     */
    public boolean isEligible(GameState state) {
        throw new UnsupportedOperationException("StoryEvent.isEligible not implemented yet");
    }
}
