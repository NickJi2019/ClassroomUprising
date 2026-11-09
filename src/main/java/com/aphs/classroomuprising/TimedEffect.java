package com.aphs.classroomuprising;

/** Source-labelled arithmetic supplied by a configured policy, not a hidden grading formula. */
public record TimedEffect(String id, int firstDay, int lastDay, double sanDelta, double xpMultiplier) {
    public TimedEffect {
        if (id == null || id.isBlank() || firstDay < 1 || lastDay < firstDay)
            throw new IllegalArgumentException("Invalid effect lifetime.");
        StatChange.requireFinite(sanDelta); StatChange.requireFinite(xpMultiplier);
        if (xpMultiplier <= 0) throw new IllegalArgumentException("Invalid XP multiplier.");
    }
    public boolean activeOn(int day) { return day >= firstDay && day <= lastDay; }
}
