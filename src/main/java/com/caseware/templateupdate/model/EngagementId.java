package com.caseware.templateupdate.model;

import java.util.Objects;

/**
 * Identity of one engagement file. The value is the id the rest of the system
 * already uses; this type only stops a missing or blank id from entering the index.
 */
public record EngagementId(String value) {
    /**
     * Rejects a null or blank id. A blank id would collapse unrelated files
     * onto the same index row.
     */
    public EngagementId {
        Objects.requireNonNull(value, "engagementId");
        if (value.isBlank()) {
            throw new IllegalArgumentException("engagementId must not be blank");
        }
    }

    /**
     * Returns the raw id so logs and glance lines show {@code eng-alpha}
     * rather than the record type name.
     */
    @Override
    public String toString() {
        return value;
    }
}
