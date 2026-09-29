package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;

import java.util.List;
import java.util.Optional;

/**
 * Stores at most one waiting template update per engagement. A later publish
 * or decision replaces or removes that row; history of old pending rows is not kept.
 */
public interface PendingUpdateStore {
    /**
     * Saves this pending update, replacing any row already stored for the same
     * engagement. The new row is the whole story from the applied version to
     * the latest version, not a delta on top of the previous pending row.
     */
    void put(PendingUpdate update);

    /**
     * Deletes the pending update for this engagement. Used when the file has
     * caught up to the latest version or the user has dismissed it.
     */
    void remove(EngagementId engagementId);

    /**
     * Returns the pending update for one engagement, or empty when that file
     * has nothing waiting.
     */
    Optional<PendingUpdate> find(EngagementId engagementId);

    /**
     * Returns every pending update that belongs to this firm.
     */
    List<PendingUpdate> listByFirm(FirmId firmId);
}
