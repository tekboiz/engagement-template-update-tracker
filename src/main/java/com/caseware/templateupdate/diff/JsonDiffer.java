package com.caseware.templateupdate.diff;

import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.JsonDiffOp;
import com.caseware.templateupdate.model.TemplateId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares two template JSON documents and reports what was added, removed, or
 * replaced. Arrays of objects that each carry a unique id are matched by that
 * id, so reordering those items does not look like a delete followed by an add.
 */
public final class JsonDiffer {
    /**
     * Walks {@code from} and {@code to} from the root and returns every change
     * as a JSON Pointer operation. An empty operation list means the two
     * documents are equal. The version numbers are stored on the result; this
     * method does not look them up.
     */
    public JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion, JsonNode from, JsonNode to) {
        List<JsonDiffOp> operations = new ArrayList<>();
        diffValues(from, to, "", operations);
        return new JsonDiff(templateId, fromVersion, toVersion, operations);
    }

    /**
     * Compares one pair of nodes. Equal nodes produce nothing. Two objects are
     * compared field by field, two arrays are compared by id or by index, and
     * every other difference — including a change of JSON type — becomes a
     * single replace. The root path is stored as {@code "/"} because a JSON
     * Pointer cannot be empty.
     */
    private void diffValues(JsonNode from, JsonNode to, String path, List<JsonDiffOp> operations) {
        if (from.equals(to)) {
            return;
        }
        if (from instanceof ObjectNode fromObj && to instanceof ObjectNode toObj) {
            diffObjects(fromObj, toObj, path, operations);
            return;
        }
        if (from instanceof ArrayNode fromArr && to instanceof ArrayNode toArr) {
            diffArrays(fromArr, toArr, path, operations);
            return;
        }
        operations.add(new JsonDiffOp.Replace(path.isEmpty() ? "/" : path, from, to));
    }

    /**
     * Compares two JSON objects by the union of their field names, in the order
     * the names were first seen. A field that exists only on the new object is
     * an add. A field that exists only on the old object is a remove. A field
     * on both sides is compared again at the child path.
     */
    private void diffObjects(ObjectNode from, ObjectNode to, String path, List<JsonDiffOp> operations) {
        Set<String> names = new LinkedHashSet<>();
        from.fieldNames().forEachRemaining(names::add);
        to.fieldNames().forEachRemaining(names::add);
        for (String name : names) {
            String child = pointer(path, name);
            boolean hasFrom = from.has(name);
            boolean hasTo = to.has(name);
            if (!hasFrom) {
                operations.add(new JsonDiffOp.Add(child, to.get(name)));
            } else if (!hasTo) {
                operations.add(new JsonDiffOp.Remove(child, from.get(name)));
            } else {
                diffValues(from.get(name), to.get(name), child, operations);
            }
        }
    }

    /**
     * Compares two JSON arrays. When every element on both sides has a unique
     * id, elements are matched by that id and the path uses the id
     * ({@code /procedures/P-210}) so order does not matter. Otherwise elements
     * are matched by index: a longer new array adds the extra tail, a shorter
     * new array removes the missing tail, and overlapping indexes are compared
     * recursively.
     */
    private void diffArrays(ArrayNode from, ArrayNode to, String path, List<JsonDiffOp> operations) {
        Map<String, JsonNode> fromById = keyed(from);
        Map<String, JsonNode> toById = keyed(to);
        if (fromById != null && toById != null) {
            for (Map.Entry<String, JsonNode> entry : fromById.entrySet()) {
                if (!toById.containsKey(entry.getKey())) {
                    operations.add(new JsonDiffOp.Remove(pointer(path, entry.getKey()), entry.getValue()));
                }
            }
            for (Map.Entry<String, JsonNode> entry : toById.entrySet()) {
                if (!fromById.containsKey(entry.getKey())) {
                    operations.add(new JsonDiffOp.Add(pointer(path, entry.getKey()), entry.getValue()));
                } else {
                    diffValues(fromById.get(entry.getKey()), entry.getValue(), pointer(path, entry.getKey()), operations);
                }
            }
            return;
        }

        int max = Math.max(from.size(), to.size());
        for (int i = 0; i < max; i++) {
            String child = pointer(path, Integer.toString(i));
            if (i >= from.size()) {
                operations.add(new JsonDiffOp.Add(child, to.get(i)));
            } else if (i >= to.size()) {
                operations.add(new JsonDiffOp.Remove(child, from.get(i)));
            } else {
                diffValues(from.get(i), to.get(i), child, operations);
            }
        }
    }

    /**
     * Indexes an array by each element's {@code id} when that is safe. Returns
     * null — meaning "compare by index instead" — if any element lacks an id,
     * an id is blank, or the same id appears twice. Numeric ids are stored as
     * their numeric text so {@code 10} and {@code "10"} share one key form.
     */
    private static Map<String, JsonNode> keyed(ArrayNode array) {
        Map<String, JsonNode> byId = new LinkedHashMap<>();
        for (JsonNode element : array) {
            if (!element.has("id") || element.get("id").isNull()) {
                return null;
            }
            JsonNode idNode = element.get("id");
            String id = idNode.isNumber() ? idNode.numberValue().toString() : idNode.asText();
            if (id.isBlank() || byId.containsKey(id)) {
                return null;
            }
            byId.put(id, element);
        }
        return byId;
    }

    /**
     * Appends one segment to a JSON Pointer, escaping {@code ~} as {@code ~0}
     * and {@code /} as {@code ~1} so those characters stay literal inside the path.
     */
    private static String pointer(String parent, String key) {
        String escaped = key.replace("~", "~0").replace("/", "~1");
        return parent + "/" + escaped;
    }
}
