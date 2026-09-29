package com.caseware.templateupdate.model;

import java.util.Objects;

/**
 * Identity of the firm that owns the engagements. Pending lists and the glance
 * view are always scoped to one firm.
 */
public record FirmId(String value) {
    /**
     * Rejects a null or blank firm id so a query cannot accidentally match
     * every firm.
     */
    public FirmId {
        Objects.requireNonNull(value, "firmId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("firmId must not be blank");
        }
    }

    /**
     * Returns the raw firm id for logs and diagnostics.
     */
    @Override
    public String toString() {
        return value;
    }
}
