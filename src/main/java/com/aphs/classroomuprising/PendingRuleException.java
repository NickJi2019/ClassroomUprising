package com.aphs.classroomuprising;

/** A deliberate boundary for a design decision which the team has not approved. */
public final class PendingRuleException extends IllegalStateException {
    private final String ruleId;
    public PendingRuleException(String ruleId, String detail) {
        super(ruleId + ": " + detail);
        this.ruleId = ruleId;
    }
    public String ruleId() { return ruleId; }
}
