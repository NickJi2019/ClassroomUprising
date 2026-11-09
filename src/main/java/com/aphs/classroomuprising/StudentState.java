package com.aphs.classroomuprising;

import java.util.List;

/** HP, SAN and learning are independent. No HP-to-mark or HP-to-energy conversion exists. */
public record StudentState(double hp, double san, double codingVibe, List<Double> unitKnowledge) {
    public StudentState {
        StatChange.requireFinite(hp); StatChange.requireFinite(san); StatChange.requireFinite(codingVibe);
        unitKnowledge = List.copyOf(unitKnowledge);
        if (unitKnowledge.size() != 6) throw new IllegalArgumentException("Six units are required.");
        unitKnowledge.forEach(StatChange::requireFinite);
    }
    StudentState apply(StatChange delta, double scale, double xpMultiplier, boolean tutorial) {
        double h = permitted(delta.hp(), tutorial) * scale;
        double s = permitted(delta.san(), tutorial) * scale;
        double v = permitted(delta.codingVibe(), tutorial) * scale;
        var units = new java.util.ArrayList<Double>();
        for (int i = 0; i < 6; i++) {
            double gain = permitted(delta.unitKnowledge().get(i), tutorial) * scale;
            units.add(unitKnowledge.get(i) + gain * (gain > 0 ? xpMultiplier : 1));
        }
        return new StudentState(hp + h, san + s, codingVibe + v * (v > 0 ? xpMultiplier : 1), units);
    }
    private static double permitted(double value, boolean tutorial) {
        return tutorial && value < 0 ? 0 : value;
    }
}
