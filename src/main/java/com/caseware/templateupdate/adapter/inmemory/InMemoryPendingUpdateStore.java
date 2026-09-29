package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.port.PendingUpdateStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds one pending update per engagement in a map. Putting a second update
 * for the same engagement replaces the first.
 */
public final class InMemoryPendingUpdateStore implements PendingUpdateStore {
    private final Map<EngagementId, PendingUpdate> updates = new ConcurrentHashMap<>();

    /**
     * Saves the pending row under its engagement id.
     */
    @Override
    public void put(PendingUpdate update) {
        updates.put(update.engagementId(), update);
    }

    /**
     * Drops the pending row for this engagement. Removing an id that is not
     * stored does nothing.
     */
    @Override
    public void remove(EngagementId engagementId) {
        updates.remove(engagementId);
    }

    /**
     * Returns the pending row, or empty when this engagement has none.
     */
    @Override
    public Optional<PendingUpdate> find(EngagementId engagementId) {
        return Optional.ofNullable(updates.get(engagementId));
    }

    /**
     * Scans the map and returns the pending rows that belong to this firm.
     */
    @Override
    public List<PendingUpdate> listByFirm(FirmId firmId) {
        List<PendingUpdate> result = new ArrayList<>();
        for (PendingUpdate update : updates.values()) {
            if (update.firmId().equals(firmId)) {
                result.add(update);
            }
        }
        return result;
    }
}
