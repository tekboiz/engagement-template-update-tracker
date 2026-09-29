package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.EngagementTemplateRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the engagement index in a map for tests and the demo. Lookup by
 * engagement id is direct. The template and firm queries scan the map.
 */
public final class InMemoryEngagementTemplateRegistry implements EngagementTemplateRegistry {
    private final Map<EngagementId, EngagementTemplateRecord> records = new ConcurrentHashMap<>();

    /**
     * Stores the row under its engagement id, replacing whatever was there.
     */
    @Override
    public void upsert(EngagementTemplateRecord record) {
        records.put(record.engagementId(), record);
    }

    /**
     * Returns the row for this engagement, or empty when it was never created.
     */
    @Override
    public Optional<EngagementTemplateRecord> find(EngagementId engagementId) {
        return Optional.ofNullable(records.get(engagementId));
    }

    /**
     * Scans every row and keeps those on {@code templateId} whose applied
     * version is strictly older than {@code version}.
     */
    @Override
    public List<EngagementTemplateRecord> findOnTemplateOlderThan(TemplateId templateId, int version) {
        List<EngagementTemplateRecord> result = new ArrayList<>();
        for (EngagementTemplateRecord record : records.values()) {
            if (record.templateId().equals(templateId) && record.appliedVersion() < version) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * Scans every row and keeps those that belong to this firm. Order follows
     * the map; the service sorts the glance list itself.
     */
    @Override
    public List<EngagementTemplateRecord> listByFirm(FirmId firmId) {
        List<EngagementTemplateRecord> result = new ArrayList<>();
        for (EngagementTemplateRecord record : records.values()) {
            if (record.firmId().equals(firmId)) {
                result.add(record);
            }
        }
        return result;
    }
}
