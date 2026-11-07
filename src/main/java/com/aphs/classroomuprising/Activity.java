package com.aphs.classroomuprising;

/**
 * An activity the player can choose in a period, with its action cost and effect.
 *
 * @author TeamPike
 * @since 1.0
 */
public class Activity {

    private final String id;
    private final DayPeriod period;
    private final int cost;
    private final StatChange effect;

    /**
     * Creates an activity.
     *
     * @param id the unique activity ID
     * @param period the period in which the activity is available
     * @param cost the number of actions it uses
     * @param effect the stat change it applies
     */
    public Activity(String id, DayPeriod period, int cost, StatChange effect) {
        this.id = id;
        this.period = period;
        this.cost = cost;
        this.effect = effect;
    }

    /**
     * Returns the activity ID.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the period in which the activity is available.
     *
     * @return the period
     */
    public DayPeriod getPeriod() {
        return period;
    }

    /**
     * Returns the number of actions the activity uses.
     *
     * @return the cost
     */
    public int getCost() {
        return cost;
    }

    /**
     * Returns the stat change the activity applies.
     *
     * @return the effect
     */
    public StatChange getEffect() {
        return effect;
    }
}
