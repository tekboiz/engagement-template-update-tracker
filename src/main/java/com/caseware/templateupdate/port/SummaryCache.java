package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.TemplateId;

import java.util.Optional;

/**
 * Remembers a change summary for a template version pair so many engagements
 * on the same applied version share one diff and one write-up.
 */
public interface SummaryCache {
    /**
     * Returns the summary already computed for this template from
     * {@code fromVersion} to {@code toVersion}, or empty when that pair has
     * not been summarized yet.
     */
    Optional<ChangeSummary> get(TemplateId templateId, int fromVersion, int toVersion);

    /**
     * Stores a summary under the template and version pair it describes.
     * A later put for the same pair replaces the previous summary.
     */
    void put(ChangeSummary summary);
}
