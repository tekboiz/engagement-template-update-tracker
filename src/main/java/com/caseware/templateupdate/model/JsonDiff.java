package com.caseware.templateupdate.model;

import java.util.List;

/**
 * The structured difference between two stored versions of one template.
 * {@code operations} is empty when the documents are identical. Each operation
 * carries the JSON Pointer of the value that changed.
 */
public record JsonDiff(TemplateId templateId, int fromVersion, int toVersion, List<JsonDiffOp> operations) {
    /**
     * Copies the operation list and rejects a span that runs backwards or uses
     * a version number below 1. A summary is always described as moving from
     * an earlier version to a later one.
     */
    public JsonDiff {
        operations = List.copyOf(operations);
        if (fromVersion < 1 || toVersion < 1) {
            throw new IllegalArgumentException("versions must be >= 1");
        }
        if (toVersion < fromVersion) {
            throw new IllegalArgumentException("toVersion must be >= fromVersion");
        }
    }
}
