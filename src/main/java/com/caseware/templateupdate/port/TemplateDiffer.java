package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.TemplateId;

/**
 * Existing capability assumed by the assignment: fast, reliable JSON diff of two
 * template versions retrieved by (templateId, version).
 */
public interface TemplateDiffer {
    /**
     * Loads the two stored template documents and returns the operations that
     * turn {@code fromVersion} into {@code toVersion}. Callers identify versions
     * by id; they do not pass the JSON themselves.
     */
    JsonDiff diff(TemplateId templateId, int fromVersion, int toVersion);
}
