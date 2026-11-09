package com.aphs.classroomuprising;

import java.util.Collections;
import java.util.List;

/** Explicit content-authored deltas, never production balancing defaults. */
public record StatChange(double hp, double san, double codingVibe, List<Double> unitKnowledge) {
    public StatChange {
        requireFinite(hp); requireFinite(san); requireFinite(codingVibe);
        unitKnowledge = List.copyOf(unitKnowledge);
        if (unitKnowledge.size() != 6) throw new IllegalArgumentException("Six unit deltas are required.");
        unitKnowledge.forEach(StatChange::requireFinite);
    }
    public static StatChange none() { return new StatChange(0, 0, 0, Collections.nCopies(6, 0.0)); }
    public static StatChange learning(int unit, double vibe, double knowledge, double san) {
        if (unit < 1 || unit > 6) throw new IllegalArgumentException("Unit must be 1..6.");
        var values = new java.util.ArrayList<>(Collections.nCopies(6, 0.0));
        values.set(unit - 1, knowledge);
        return new StatChange(0, san, vibe, values);
    }
    public static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("A finite value is required.");
    }
}
