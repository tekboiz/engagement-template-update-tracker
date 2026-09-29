package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.JsonDiff;

/**
 * Turns a structured template diff into the sentences a user reads. The
 * deterministic summarizer is the implementation shipped here; another
 * generator can be plugged in behind this same method.
 */
public interface SummaryGenerator {
    /**
     * Describes {@code diff} in practitioner language. When {@code releaseNotes}
     * is non-blank, that text is the headline. A null or blank value means the
     * headline should be derived from the diff.
     */
    ChangeSummary summarize(JsonDiff diff, String releaseNotes);
}
