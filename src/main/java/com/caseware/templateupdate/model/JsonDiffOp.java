package com.caseware.templateupdate.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One change inside a template diff. {@code path} is the JSON Pointer of the
 * value that changed. The three kinds are an added value, a removed value, and
 * a value that existed on both sides but is no longer equal.
 */
public sealed interface JsonDiffOp {
    /**
     * Returns the JSON Pointer this operation applies to.
     */
    String path();

    /**
     * A value that exists on the newer template and did not exist on the older one.
     */
    record Add(String path, JsonNode value) implements JsonDiffOp {}

    /**
     * A value that existed on the older template and is gone from the newer one.
     */
    record Remove(String path, JsonNode value) implements JsonDiffOp {}

    /**
     * A value present on both sides whose content changed. {@code from} is the
     * old JSON and {@code to} is the new JSON.
     */
    record Replace(String path, JsonNode from, JsonNode to) implements JsonDiffOp {}
}
