package com.caseware.templateupdate.model;

/**
 * One line of the firm-wide glance list. {@code pending} is false when the
 * engagement is current or has dismissed the latest version. {@code latestVersion}
 * and {@code headline} may be null when the catalog has no publish yet, or when
 * there is nothing waiting to describe.
 */
public record AtAGlanceRow(
        EngagementId engagementId,
        TemplateId templateId,
        int appliedVersion,
        Integer latestVersion,
        boolean pending,
        String headline
) {}
