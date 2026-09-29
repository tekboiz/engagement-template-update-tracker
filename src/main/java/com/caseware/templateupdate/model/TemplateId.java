package com.caseware.templateupdate.model;

import java.util.Objects;

/**
 * Identity of a product template, such as {@code audit-ifrs}. Engagements point
 * at one template, and publishes are recorded against that same id.
 */
public record TemplateId(String value) {
    /**
     * Rejects a null or blank template id. Diffs, the catalog, and the summary
     * cache are all keyed by this value.
     */
    public TemplateId {
        Objects.requireNonNull(value, "templateId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
    }

    /**
     * Returns the raw template id so headlines can say {@code audit-ifrs}
     * without extra formatting.
     */
    @Override
    public String toString() {
        return value;
    }
}
