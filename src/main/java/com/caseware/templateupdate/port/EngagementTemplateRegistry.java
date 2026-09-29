package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.TemplateId;

import java.util.List;
import java.util.Optional;

/**
 * Queryable projection of engagement → template version.
 * Populated from engagement-management hooks. Never loads an engagement file.
 */
public interface EngagementTemplateRegistry {
    /**
     * Inserts this engagement's index row, or replaces the row already stored
     * for the same engagement id. Called when a file is created and again
     * after every apply or decline.
     */
    void upsert(EngagementTemplateRecord record);

    /**
     * Returns the index row for one engagement, or empty when that file has
     * never been registered.
     */
    Optional<EngagementTemplateRecord> find(EngagementId engagementId);

    /**
     * Returns every engagement on this template whose applied version is still
     * older than {@code version}. A publish uses this to find who needs a new
     * pending update, without opening any engagement file.
     */
    List<EngagementTemplateRecord> findOnTemplateOlderThan(TemplateId templateId, int version);

    /**
     * Returns every engagement that belongs to this firm, including ones that
     * are already current. The glance list is built from this set.
     */
    List<EngagementTemplateRecord> listByFirm(FirmId firmId);
}
