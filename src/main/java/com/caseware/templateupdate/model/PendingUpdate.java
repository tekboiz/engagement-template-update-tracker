package com.caseware.templateupdate.model;

import java.util.List;

/**
 * One engagement's waiting template update. {@code summary} describes the whole
 * jump from the applied version to the latest. {@code hops} describes each
 * adjacent publish in between, so a user can see v1 to v2 and v2 to v3 as well
 * as the combined v1 to v3 story.
 */
public record PendingUpdate(
        EngagementId engagementId,
        FirmId firmId,
        TemplateId templateId,
        int appliedVersion,
        int latestVersion,
        ChangeSummary summary,
        List<ChangeSummary> hops
) {
    /**
     * Copies the hop list so a caller cannot change the stored changelog after
     * the pending row has been saved.
     */
    public PendingUpdate {
        hops = List.copyOf(hops);
    }
}
