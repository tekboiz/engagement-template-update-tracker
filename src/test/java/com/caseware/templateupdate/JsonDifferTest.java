package com.caseware.templateupdate;

import com.caseware.templateupdate.diff.JsonDiffer;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.JsonDiffOp;
import com.caseware.templateupdate.model.TemplateId;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that template JSON is compared by id when items have ids, by index
 * when they do not, and that equal documents produce an empty diff.
 */
class JsonDifferTest {
    private final JsonDiffer differ = new JsonDiffer();
    private final TemplateId template = new TemplateId("audit-ifrs");

    /**
     * Adding procedure P-210 between v1 and v2 is reported as an add at
     * {@code /procedures/P-210}, matched by id rather than by array position.
     */
    @Test
    void detectsAddedProcedureById() {
        JsonDiff diff = differ.diff(template, 1, 2, Templates.v1(), Templates.v2());
        assertTrue(diff.operations().stream().anyMatch(op ->
                op instanceof JsonDiffOp.Add add && add.path().equals("/procedures/P-210")));
    }

    /**
     * Guidance that appears on P-100 only in v3 is an add of that field, not a
     * replacement of the whole procedure.
     */
    @Test
    void detectsNestedFieldAddWhenPreviouslyAbsent() {
        JsonDiff diff = differ.diff(template, 2, 3, Templates.v2(), Templates.v3());
        assertTrue(diff.operations().stream().anyMatch(op ->
                op instanceof JsonDiffOp.Add add
                        && add.path().equals("/procedures/P-100/guidance")));
    }

    /**
     * When guidance exists on both sides and the text changes, the diff is a
     * replace of that field, carrying the old text and the new text.
     */
    @Test
    void detectsNestedReplaceWhenBothSidesPresent() throws Exception {
        JsonNode from = Templates.JSON.readTree("{\"procedures\":{\"P-100\":{\"guidance\":\"old\"}}}");
        JsonNode to = Templates.JSON.readTree("{\"procedures\":{\"P-100\":{\"guidance\":\"new\"}}}");
        JsonDiff diff = differ.diff(template, 1, 2, from, to);
        JsonDiffOp.Replace replace = assertInstanceOf(JsonDiffOp.Replace.class, diff.operations().getFirst());
        assertEquals("/procedures/P-100/guidance", replace.path());
        assertEquals("old", replace.from().asText());
        assertEquals("new", replace.to().asText());
    }

    /**
     * A document compared with its own deep copy produces no operations, so a
     * republish of identical content does not invent a changelog.
     */
    @Test
    void identicalDocumentsProduceNoOperations() {
        JsonNode doc = Templates.v1();
        JsonDiff diff = differ.diff(template, 1, 1, doc, doc.deepCopy());
        assertTrue(diff.operations().isEmpty());
    }

    /**
     * An array of plain strings has no ids, so the second element is compared
     * by index and a changed value is a replace at {@code /tags/1}.
     */
    @Test
    void indexBasedArrayDiffWhenItemsHaveNoId() throws Exception {
        JsonNode from = Templates.JSON.readTree("{\"tags\":[\"a\",\"b\"]}");
        JsonNode to = Templates.JSON.readTree("{\"tags\":[\"a\",\"c\"]}");
        JsonDiff diff = differ.diff(template, 1, 2, from, to);
        JsonDiffOp.Replace replace = assertInstanceOf(JsonDiffOp.Replace.class, diff.operations().getFirst());
        assertEquals("/tags/1", replace.path());
        assertEquals("b", replace.from().asText());
        assertEquals("c", replace.to().asText());
    }
}
