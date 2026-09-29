package com.caseware.templateupdate.port;

import com.caseware.templateupdate.model.TemplateId;

import java.util.List;
import java.util.Optional;

/**
 * Remembers which versions of each template have been published. The service
 * uses it to know the latest version and to walk the versions between an
 * applied version and that latest version.
 */
public interface TemplateCatalog {
    /**
     * Records that {@code version} of this template is now published. Recording
     * the same version again does not create a second entry.
     */
    void recordPublish(TemplateId templateId, int version);

    /**
     * Returns the highest published version of this template, or empty when
     * nothing has been published yet.
     */
    Optional<Integer> latestVersion(TemplateId templateId);

    /**
     * Returns every published version of this template, sorted from oldest to
     * newest. The hop changelog is built by stepping through this list.
     */
    List<Integer> versions(TemplateId templateId);
}
