package com.caseware.templateupdate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The audit-ifrs documents shared by the service, differ, and summarizer tests.
 * v1 is the starting template, v2 adds a procedure, v3 adds guidance and a
 * disclosure, and v4 adds one more procedure.
 */
final class Templates {
    static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Hidden because the fixtures are static methods.
     */
    private Templates() {}

    /**
     * Returns template v1: cash-confirmation procedure P-100 and checklist C-10.
     */
    static JsonNode v1() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(procedure("P-100", "Confirm cash balances", true, null)));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        return root;
    }

    /**
     * Returns template v2: v1 plus the going-concern procedure P-210.
     */
    static JsonNode v2() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(
                procedure("P-100", "Confirm cash balances", true, null),
                procedure("P-210", "Evaluate going concern", true, null)
        ));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        return root;
    }

    /**
     * Returns template v3: v2, plus compensating-balance guidance on P-100 and
     * the related-party disclosure D-3.
     */
    static JsonNode v3() {
        ObjectNode root = JSON.createObjectNode();
        root.set("procedures", array(
                procedure("P-100", "Confirm cash balances", true, "Include compensating balances."),
                procedure("P-210", "Evaluate going concern", true, null)
        ));
        root.set("checklists", array(item("C-10", "Year-end close checklist")));
        root.set("disclosures", array(item("D-3", "Related-party transactions")));
        return root;
    }

    /**
     * Returns template v4: a copy of v3 plus the subsequent-events procedure P-300.
     * The copy keeps later tests from mutating the v3 fixture.
     */
    static JsonNode v4() {
        ObjectNode root = v3().deepCopy();
        ((ArrayNode) root.get("procedures")).add(procedure("P-300", "Test subsequent events", false, null));
        return root;
    }

    /**
     * Wraps the given objects in a JSON array, in the order they were passed.
     */
    private static ArrayNode array(ObjectNode... nodes) {
        ArrayNode array = JSON.createArrayNode();
        for (ObjectNode node : nodes) {
            array.add(node);
        }
        return array;
    }

    /**
     * Builds an object with an id and a title, used for checklists and disclosures.
     */
    private static ObjectNode item(String id, String title) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", id);
        node.put("title", title);
        return node;
    }

    /**
     * Builds a procedure. Guidance is left off the object when it is null, so a
     * later version that adds guidance is a new field.
     */
    private static ObjectNode procedure(String id, String title, boolean required, String guidance) {
        ObjectNode node = item(id, title);
        node.put("required", required);
        if (guidance != null) {
            node.put("guidance", guidance);
        }
        return node;
    }
}
