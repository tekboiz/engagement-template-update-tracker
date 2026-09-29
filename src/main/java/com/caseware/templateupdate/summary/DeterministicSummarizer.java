package com.caseware.templateupdate.summary;

import com.caseware.templateupdate.model.ChangeAction;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.JsonDiffOp;
import com.caseware.templateupdate.model.SummaryItem;
import com.caseware.templateupdate.port.SummaryGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns a JSON diff into short sentences a practitioner can read. Every line
 * is derived from one diff operation, so the wording is stable for the same
 * documents and does not invent changes that are not in the diff.
 */
public final class DeterministicSummarizer implements SummaryGenerator {
    private static final Map<String, String> CATEGORIES = Map.of(
            "procedures", "Procedures",
            "checklists", "Checklists",
            "disclosures", "Disclosures",
            "guidance", "Guidance",
            "workpapers", "Workpapers",
            "methodology", "Methodology"
    );

    /**
     * Writes one summary line per diff operation, then a headline. When release
     * notes are present they become the headline unchanged (aside from trimming).
     * Otherwise the headline counts how many lines were added, removed, and
     * updated. The generator name on the result is {@code "deterministic"}.
     */
    @Override
    public ChangeSummary summarize(JsonDiff diff, String releaseNotes) {
        List<SummaryItem> items = new ArrayList<>();
        for (JsonDiffOp op : diff.operations()) {
            items.add(toItem(op));
        }
        return new ChangeSummary(
                diff.templateId(),
                diff.fromVersion(),
                diff.toVersion(),
                headline(diff, items, releaseNotes),
                items,
                "deterministic"
        );
    }

    /**
     * Checks that every summary line cites a path that actually appears in the
     * diff. A line whose path is missing is rejected. This is the guard against
     * a summary that describes a change the documents do not contain.
     */
    public static void assertGrounded(JsonDiff diff, ChangeSummary summary) {
        Set<String> allowed = diff.operations().stream().map(JsonDiffOp::path).collect(Collectors.toSet());
        for (SummaryItem item : summary.items()) {
            if (!allowed.contains(item.sourcePath())) {
                throw new UngroundedSummaryException(
                        "Ungrounded summary item for path " + item.sourcePath()
                );
            }
        }
    }

    /**
     * Turns one diff operation into one summary line. A removal names the thing
     * that went away. A replacement shows the old value and the new value. An
     * addition is worded either as a new entity or as a field added onto an
     * entity that already existed.
     */
    private static SummaryItem toItem(JsonDiffOp op) {
        return switch (op) {
            case JsonDiffOp.Add add -> describeAdd(add);
            case JsonDiffOp.Remove remove -> new SummaryItem(
                    remove.path(),
                    ChangeAction.REMOVED,
                    category(remove.path()),
                    "Removed " + singular(category(remove.path())) + " " + label(remove.value())
            );
            case JsonDiffOp.Replace replace -> new SummaryItem(
                    replace.path(),
                    ChangeAction.UPDATED,
                    category(replace.path()),
                    "Updated " + leaf(replace.path()) + ": " + compact(replace.from()) + " → " + compact(replace.to())
            );
        };
    }

    /**
     * Words an addition. A nested scalar on an existing item, such as new
     * guidance text on procedure P-100, is described as an update of that item.
     * Adding a whole new object, such as a procedure, is described as an add
     * and labeled with the object's title and id when those fields exist.
     */
    private static SummaryItem describeAdd(JsonDiffOp.Add add) {
        JsonNode value = add.value();
        if (isNestedScalarField(add.path(), value)) {
            return new SummaryItem(
                    add.path(),
                    ChangeAction.UPDATED,
                    category(add.path()),
                    "Updated " + parentEntity(add.path()) + ": added " + leaf(add.path())
                            + " \"" + compact(value) + "\""
            );
        }
        return new SummaryItem(
                add.path(),
                ChangeAction.ADDED,
                category(add.path()),
                "Added " + singular(category(add.path())) + " " + label(value)
        );
    }

    /**
     * Decides whether an added value is a field on something that already
     * existed. That is true when the path is at least three segments deep
     * ({@code /procedures/P-100/guidance}) and the value is not itself a titled
     * object. A brand-new procedure object fails this test and stays an add.
     */
    private static boolean isNestedScalarField(String path, JsonNode value) {
        return segments(path).size() >= 3 && (value == null || !value.isContainerNode() || !value.has("title"));
    }

    /**
     * Returns the path segment that names the entity a nested field belongs to.
     * For {@code /procedures/P-100/guidance} that segment is {@code P-100}.
     */
    private static String parentEntity(String path) {
        List<String> parts = segments(path);
        if (parts.size() >= 2) {
            return parts.get(parts.size() - 2);
        }
        return leaf(path);
    }

    /**
     * Splits a JSON Pointer into its segments and unescapes each one. Empty
     * pieces from the leading slash are dropped.
     */
    private static List<String> segments(String path) {
        List<String> parts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isBlank()) {
                parts.add(unescape(part));
            }
        }
        return parts;
    }

    /**
     * Chooses the one-line headline for a version span. Release notes win when
     * they are non-blank. A diff with no operations says the template moved
     * forward with no content changes. Any other diff reports how many lines
     * were added, removed, and updated, omitting counts that are zero.
     */
    private static String headline(JsonDiff diff, List<SummaryItem> items, String releaseNotes) {
        if (releaseNotes != null && !releaseNotes.isBlank()) {
            return releaseNotes.trim();
        }
        if (items.isEmpty()) {
            return "Template " + diff.templateId() + " v" + diff.fromVersion() + " → v" + diff.toVersion()
                    + ": no content changes";
        }
        long added = items.stream().filter(i -> i.action() == ChangeAction.ADDED).count();
        long removed = items.stream().filter(i -> i.action() == ChangeAction.REMOVED).count();
        long updated = items.stream().filter(i -> i.action() == ChangeAction.UPDATED).count();
        List<String> parts = new ArrayList<>();
        if (added > 0) {
            parts.add(added + " added");
        }
        if (removed > 0) {
            parts.add(removed + " removed");
        }
        if (updated > 0) {
            parts.add(updated + " updated");
        }
        return "Template " + diff.templateId() + " v" + diff.fromVersion() + " → v" + diff.toVersion()
                + ": " + String.join(", ", parts);
    }

    /**
     * Maps the first path segment to a display category. Known template sections
     * such as procedures and checklists use a fixed label. Any other section
     * name is capitalized as-is. A path with no segment is called Template.
     */
    private static String category(String path) {
        String first = firstSegment(path);
        if (first == null) {
            return "Template";
        }
        return CATEGORIES.getOrDefault(first, capitalize(first));
    }

    /**
     * Turns a plural category label into the singular lowercase word used inside
     * a sentence, so "Procedures" becomes "procedure" in "Added procedure …".
     * A label that does not end in s is only lowercased.
     */
    private static String singular(String category) {
        if (category.endsWith("s") && category.length() > 1) {
            return category.substring(0, category.length() - 1).toLowerCase();
        }
        return category.toLowerCase();
    }

    /**
     * Picks the human-readable name of a JSON value. An object with a title is
     * shown as that title, with the id in parentheses when an id is present.
     * An array becomes the labels of its elements joined with semicolons.
     * Anything else falls back to a short JSON rendering.
     */
    private static String label(JsonNode value) {
        if (value != null && value.isArray() && !value.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode element : value) {
                parts.add(label(element));
            }
            return String.join("; ", parts);
        }
        if (value != null && value.hasNonNull("title")) {
            String title = value.get("title").asText();
            if (value.hasNonNull("id")) {
                return "\"" + title + "\" (" + value.get("id").asText() + ")";
            }
            return "\"" + title + "\"";
        }
        return compact(value);
    }

    /**
     * Renders a JSON value in a form short enough to sit inside a sentence.
     * Null, text, numbers, and booleans are written directly. An object with a
     * title uses that title. Other JSON is the raw text, cut off after 80
     * characters.
     */
    private static String compact(JsonNode value) {
        if (value == null || value.isNull()) {
            return "null";
        }
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        if (value.hasNonNull("title")) {
            return label(value);
        }
        String json = value.toString();
        return json.length() > 80 ? json.substring(0, 77) + "..." : json;
    }

    /**
     * Returns the first non-empty segment of a JSON Pointer, or null when the
     * path has none. That segment is the template section the change belongs to.
     */
    private static String firstSegment(String path) {
        String[] parts = path.split("/");
        for (String part : parts) {
            if (!part.isBlank()) {
                return unescape(part);
            }
        }
        return null;
    }

    /**
     * Returns the last non-empty segment of a JSON Pointer, which is the field
     * or item the sentence should name. A path with no segment is called "item".
     */
    private static String leaf(String path) {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isBlank()) {
                return unescape(parts[i]);
            }
        }
        return "item";
    }

    /**
     * Reverses JSON Pointer escaping so {@code ~1} becomes {@code /} and
     * {@code ~0} becomes {@code ~} in text shown to a person.
     */
    private static String unescape(String segment) {
        return segment.replace("~1", "/").replace("~0", "~");
    }

    /**
     * Uppercases the first character of a section name so an unknown path
     * segment can be shown as a category label.
     */
    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
