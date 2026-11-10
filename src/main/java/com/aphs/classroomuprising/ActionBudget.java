package com.aphs.classroomuprising;

/**
 * Tracks the actions remaining in each period of the day.
 *
 * @author TeamPike
 * @since 1.0
 */
public class ActionBudget {

    /** The last day that was initialized; 0 means no day has been initialized yet. */
    private int initializedDay;

    /** Remaining actions, indexed by {@link DayPeriod#ordinal()}. */
    private final int[] remainingByPeriod;

    /** Creates an empty budget. */
    public ActionBudget() {
        this.initializedDay = 0;
        this.remainingByPeriod = new int[DayPeriod.values().length];
    }

    /**
     * Sets the fixed allowances for a new day. Each day is initialized only once.
     *
     * @param day the day to initialize
     */
    public void initializeDay(int day) {
        throw new UnsupportedOperationException("ActionBudget.initializeDay not implemented yet");
    }

    /**
     * Checks whether a period has enough actions left.
     *
     * @param period the period to check
     * @param cost the number of actions needed
     * @return true if the cost can be spent
     */
    public boolean canSpend(DayPeriod period, int cost) {
        throw new UnsupportedOperationException("ActionBudget.canSpend not implemented yet");
    }

    /**
     * Spends actions from a period.
     *
     * @param period the period to spend from
     * @param cost the number of actions to spend
     */
    public void spend(DayPeriod period, int cost) {
        throw new UnsupportedOperationException("ActionBudget.spend not implemented yet");
    }

    /**
     * Discards the unused actions of a finished period.
     *
     * @param period the period to close
     */
    public void closePeriod(DayPeriod period) {
        throw new UnsupportedOperationException("ActionBudget.closePeriod not implemented yet");
    }

    /**
     * Returns the actions left in a period.
     *
     * @param period the period to check
     * @return the number of actions remaining
     */
    public int remaining(DayPeriod period) {
        throw new UnsupportedOperationException("ActionBudget.remaining not implemented yet");
    }
}
