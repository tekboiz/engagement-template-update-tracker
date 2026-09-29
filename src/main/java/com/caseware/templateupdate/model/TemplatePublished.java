package com.caseware.templateupdate.model;

/**
 * The fact that a template version was published. {@code releaseNotes} is
 * optional author text. When it is present, it becomes the headline of the
 * accumulated summary for this publish.
 */
public record TemplatePublished(TemplateId templateId, int version, String releaseNotes) {
    /**
     * Rejects a publish version below 1.
     */
    public TemplatePublished {
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
    }

    /**
     * Publishes a version that has no author release notes. The summary
     * headline is then computed from the diff itself.
     */
    public TemplatePublished(TemplateId templateId, int version) {
        this(templateId, version, null);
    }
}
