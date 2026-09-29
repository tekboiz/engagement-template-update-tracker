package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.diff.JsonDiffer;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.port.TemplateDiffer;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A template differ whose documents live in a map instead of a template store.
 * Tests and the demo call {@link #put} before any publish so a later diff can
 * find both versions. {@link #diffCalls()} counts how many times the documents
 * were actually compared.
 */
public final class InMemoryTemplateDiffer implements TemplateDiffer {
    private final Map<String, JsonNode> documents = new ConcurrentHashMap<>();
    private final JsonDiffer jsonDiffer = new JsonDiffer();
    private final AtomicInteger diffCalls = new AtomicInteger();

    /**
     * Stores the JSON document for one template version, replacing any document
     * previously stored for that same version.
     */
    public void put(TemplateId templateId, int version, JsonNode document) {
        documents.put(key(templateId, version), document);
    }

    /**
     * Returns how many times {@link #diff} has loaded documents and compared
     * them. Tests use this to prove that many engagements on the same applied
     * version share a single comparison.
     */
    public int diffCalls() {
        return diffCalls.get();
    }

    /**
     * Loads the two stored documents and compares them. The call is counted
     * even when a document is missing. A missing document fails fast, because
     * a summary must not be invented for a version that was never stored.
     */
    @Override
    public JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion) {
        diffCalls.incrementAndGet();
        JsonNode from = documents.get(key(templateId, fromVersion));
        JsonNode to = documents.get(key(templateId, toVersion));
        if (from == null || to == null) {
            throw new IllegalStateException(
                    "Missing template documents for " + templateId + " " + fromVersion + "→" + toVersion
            );
        }
        return jsonDiffer.diff(templateId, fromVersion, toVersion, from, to);
    }

    /**
     * Builds the map key {@code template:version} used to store and load a
     * template document.
     */
    private static String key(TemplateId templateId, int version) {
        return templateId.value() + ":" + version;
    }
}
