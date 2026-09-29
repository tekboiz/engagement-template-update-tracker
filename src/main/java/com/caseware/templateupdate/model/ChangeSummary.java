package com.caseware.templateupdate.model;

import java.util.List;

/**
 * The human-readable description of one version span. {@code headline} is the
 * single line shown on the glance list. {@code items} are the individual
 * changes. {@code generator} names which summarizer wrote this, so a later
 * implementation can be told apart from the deterministic one.
 */
public record ChangeSummary(
        TemplateId templateId,
        int fromVersion,
        int toVersion,
        String headline,
        List<SummaryItem> items,
        String generator
) {
    /**
     * Copies the item list so the summary cannot be edited after it is cached.
     */
    public ChangeSummary {
        items = List.copyOf(items);
    }
}
