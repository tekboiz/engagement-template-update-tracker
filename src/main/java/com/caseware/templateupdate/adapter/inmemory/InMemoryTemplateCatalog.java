package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.TemplateCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers published versions in a set per template. A set is enough because
 * publishing the same version twice should not create two entries.
 */
public final class InMemoryTemplateCatalog implements TemplateCatalog {
    private final Map<TemplateId, Set<Integer>> versions = new ConcurrentHashMap<>();

    /**
     * Adds this version to the template's set. Adding it again changes nothing.
     */
    @Override
    public void recordPublish(TemplateId templateId, int version) {
        versions.computeIfAbsent(templateId, key -> ConcurrentHashMap.newKeySet()).add(version);
    }

    /**
     * Returns the largest version in the set, or empty when this template has
     * never been published.
     */
    @Override
    public Optional<Integer> latestVersion(TemplateId templateId) {
        Set<Integer> set = versions.get(templateId);
        if (set == null || set.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Collections.max(set));
    }

    /**
     * Returns a sorted copy of the published versions, oldest first. The caller
     * can walk the copy without affecting the stored set.
     */
    @Override
    public List<Integer> versions(TemplateId templateId) {
        Set<Integer> set = versions.get(templateId);
        if (set == null) {
            return List.of();
        }
        List<Integer> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        return sorted;
    }
}
