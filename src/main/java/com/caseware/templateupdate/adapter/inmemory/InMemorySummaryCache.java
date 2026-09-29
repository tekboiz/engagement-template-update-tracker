package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.SummaryCache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches change summaries in a map keyed by template id and the two version
 * numbers. Tests use {@link #size()} to see how many pairs have been stored.
 */
public final class InMemorySummaryCache implements SummaryCache {
    private final Map<String, ChangeSummary> cache = new ConcurrentHashMap<>();

    /**
     * Looks up the summary for this version pair, or returns empty on a miss.
     */
    @Override
    public Optional<ChangeSummary> get(TemplateId templateId, int fromVersion, int toVersion) {
        return Optional.ofNullable(cache.get(key(templateId, fromVersion, toVersion)));
    }

    /**
     * Stores the summary under the template and version pair written on the
     * summary itself. A second put for the same pair overwrites the first.
     */
    @Override
    public void put(ChangeSummary summary) {
        cache.put(key(summary.templateId(), summary.fromVersion(), summary.toVersion()), summary);
    }

    /**
     * Returns how many version pairs are currently cached. This is a test aid
     * and is not part of the {@link SummaryCache} contract.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Builds the map key {@code template:from:to} so two spans that share a
     * template but differ in either version stay separate.
     */
    private static String key(TemplateId templateId, int fromVersion, int toVersion) {
        return templateId.value() + ":" + fromVersion + ":" + toVersion;
    }
}
