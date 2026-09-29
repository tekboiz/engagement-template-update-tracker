package com.caseware.templateupdate.model;

/**
 * The index row for one engagement: which template it was created from, which
 * version has been applied, and how far the user has dismissed updates.
 * {@code dismissedThroughVersion} is null until the user declines.
 */
public record EngagementTemplateRecord(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int appliedVersion,
        Integer dismissedThroughVersion
) {
    /**
     * Checks that both version numbers are real template versions. Applied
     * version is always required. The dismissal is optional, and when it is
     * set it must also be at least 1.
     */
    public EngagementTemplateRecord {
        if (appliedVersion < 1) {
            throw new IllegalArgumentException("appliedVersion must be >= 1");
        }
        if (dismissedThroughVersion != null && dismissedThroughVersion < 1) {
            throw new IllegalArgumentException("dismissedThroughVersion must be >= 1");
        }
    }

    /**
     * Reports whether this engagement should ignore {@code version}. True when
     * the user has declined through a version at least as high as this one.
     * Declining v3 therefore hides v2 and v3, while v4 is still visible.
     */
    public boolean isDismissedThrough(int version) {
        return dismissedThroughVersion != null && version <= dismissedThroughVersion;
    }
}
