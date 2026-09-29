package com.caseware.templateupdate.model;

/**
 * A user's apply or decline choice for one engagement. {@code targetVersion}
 * is the template version the choice refers to, which is normally the latest
 * version they were shown.
 */
public record DecisionEvent(EngagementId engagementId, Decision decision, int targetVersion) {
    /**
     * Rejects a target version below 1 so a decision cannot point at a version
     * the catalog could never have published.
     */
    public DecisionEvent {
        if (targetVersion < 1) {
            throw new IllegalArgumentException("targetVersion must be >= 1");
        }
    }
}
