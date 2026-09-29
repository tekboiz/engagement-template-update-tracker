package com.caseware.templateupdate.model;

/**
 * The fact that an engagement file was created from a template version.
 * {@code version} is the version applied at creation time, which becomes the
 * engagement's starting point in the index.
 */
public record CreatedEngagement(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int version
) {
    /**
     * Rejects a creation version below 1. Template versions start at 1, so a
     * lower number cannot be stored as the applied version.
     */
    public CreatedEngagement {
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }
}
