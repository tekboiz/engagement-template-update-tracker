package com.caseware.templateupdate.app;

import com.caseware.templateupdate.ConflictException;
import com.caseware.templateupdate.NotFoundException;
import com.caseware.templateupdate.model.AtAGlanceRow;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.CreatedEngagement;
import com.caseware.templateupdate.model.Decision;
import com.caseware.templateupdate.model.DecisionEvent;
import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.EngagementTemplateRecord;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.JsonDiff;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.model.TemplatePublished;
import com.caseware.templateupdate.port.EngagementTemplateRegistry;
import com.caseware.templateupdate.port.PendingUpdateStore;
import com.caseware.templateupdate.port.SummaryCache;
import com.caseware.templateupdate.port.SummaryGenerator;
import com.caseware.templateupdate.port.TemplateCatalog;
import com.caseware.templateupdate.port.TemplateDiffer;
import com.caseware.templateupdate.summary.DeterministicSummarizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps a queryable engagement→template index and materializes human-readable
 * pending updates when templates are published.
 *
 * <p>This service has no engagement-loader dependency. The 1-minute engagement
 * load is never used to answer "what version is this file on?" or to list
 * pending updates.
 */
public final class PendingUpdateService {
    private final EngagementTemplateRegistry registry;
    private final TemplateCatalog catalog;
    private final TemplateDiffer differ;
    private final SummaryGenerator summarizer;
    private final PendingUpdateStore pending;
    private final SummaryCache cache;

    /**
     * Wires the six collaborators this service needs. None of them may be null:
     * the registry and catalog are the two indexes, the differ and summarizer
     * turn template JSON into sentences, and the pending store plus summary
     * cache hold the results so later queries do not recompute them.
     */
    public PendingUpdateService(
            EngagementTemplateRegistry registry,
            TemplateCatalog catalog,
            TemplateDiffer differ,
            SummaryGenerator summarizer,
            PendingUpdateStore pending,
            SummaryCache cache
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.catalog = Objects.requireNonNull(catalog);
        this.differ = Objects.requireNonNull(differ);
        this.summarizer = Objects.requireNonNull(summarizer);
        this.pending = Objects.requireNonNull(pending);
        this.cache = Objects.requireNonNull(cache);
    }

    /**
     * Records that an engagement file was just created from a template version.
     * Saves an index row with that applied version and no dismissal, then
     * recalculates pending state. If the file was created on the newest
     * published version, nothing is pending. If a newer version is already in
     * the catalog, a pending update is opened immediately.
     */
    public void onEngagementCreated(CreatedEngagement event) {
        EngagementTemplateRecord record = new EngagementTemplateRecord(
                event.engagementId(),
                event.firmId(),
                event.templateId(),
                event.version(),
                null
        );
        registry.upsert(record);
        syncPending(record);
    }

    /**
     * Records a newly published template version and refreshes every engagement
     * that is still on an older version of that template.
     *
     * <p>Engagements that share the same applied version are grouped together so
     * the diff and the summary are built once per group, not once per file.
     * An engagement that already declined through this version is left alone.
     * Everyone else gets a single pending row covering the whole jump from the
     * version they have applied to the version just published, including a
     * per-hop changelog of the versions in between.
     */
    public void onTemplatePublished(TemplatePublished event) {
        catalog.recordPublish(event.templateId(), event.version());
        List<EngagementTemplateRecord> affected =
                registry.findOnTemplateOlderThan(event.templateId(), event.version());

        Map<Integer, List<EngagementTemplateRecord>> byApplied = new LinkedHashMap<>();
        for (EngagementTemplateRecord record : affected) {
            byApplied.computeIfAbsent(record.appliedVersion(), key -> new ArrayList<>()).add(record);
        }

        for (Map.Entry<Integer, List<EngagementTemplateRecord>> group : byApplied.entrySet()) {
            Summaries summaries = buildSummaries(
                    event.templateId(),
                    group.getKey(),
                    event.version(),
                    event.releaseNotes()
            );
            for (EngagementTemplateRecord record : group.getValue()) {
                if (record.isDismissedThrough(event.version())) {
                    continue;
                }
                pending.put(new PendingUpdate(
                        record.engagementId(),
                        record.firmId(),
                        record.templateId(),
                        record.appliedVersion(),
                        event.version(),
                        summaries.accumulated(),
                        summaries.hops()
                ));
            }
        }
    }

    /**
     * Records the user's choice for one engagement and then recalculates what
     * is still pending.
     *
     * <p>Apply moves the applied version forward to the chosen version and
     * clears any earlier dismissal. Choosing a version older than the one
     * already applied is rejected. Decline leaves the applied version where it
     * is and marks every version up through the chosen one as dismissed, so
     * those versions stop showing as pending.
     *
     * <p>The content of the engagement file is not changed here. If a newer
     * version was published while the user was deciding, the follow-up sync
     * opens a new pending row from the version they just accepted.
     */
    public void onDecision(DecisionEvent event) {
        EngagementTemplateRecord record = registry.find(event.engagementId())
                .orElseThrow(() -> new NotFoundException("engagement", event.engagementId().value()));

        EngagementTemplateRecord updated;
        if (event.decision() == Decision.APPLY) {
            if (event.targetVersion() < record.appliedVersion()) {
                throw new ConflictException(
                        "Cannot apply template v" + event.targetVersion()
                                + " over newer applied v" + record.appliedVersion()
                );
            }
            updated = new EngagementTemplateRecord(
                    record.engagementId(),
                    record.firmId(),
                    record.templateId(),
                    event.targetVersion(),
                    null
            );
        } else {
            updated = new EngagementTemplateRecord(
                    record.engagementId(),
                    record.firmId(),
                    record.templateId(),
                    record.appliedVersion(),
                    event.targetVersion()
            );
        }
        registry.upsert(updated);
        syncPending(updated);
    }

    /**
     * Returns every engagement in this firm that still has a template update
     * waiting for a decision.
     */
    public List<PendingUpdate> listPendingForFirm(FirmId firmId) {
        return pending.listByFirm(firmId);
    }

    /**
     * Returns the pending update for one engagement, or empty when that file
     * is already on the latest version or has dismissed it.
     */
    public Optional<PendingUpdate> getPending(EngagementId engagementId) {
        return pending.find(engagementId);
    }

    /**
     * Builds the firm-wide glance list: one row per engagement, whether or not
     * it has something pending. Pending rows are listed first, then rows are
     * ordered by engagement id. The headline is included only when a pending
     * update exists. The latest version comes from that pending row, or from
     * the catalog when the engagement is current.
     */
    public List<AtAGlanceRow> listAtAGlance(FirmId firmId) {
        List<AtAGlanceRow> rows = new ArrayList<>();
        for (EngagementTemplateRecord record : registry.listByFirm(firmId)) {
            Optional<PendingUpdate> maybePending = pending.find(record.engagementId());
            Integer latest = maybePending.map(PendingUpdate::latestVersion)
                    .orElseGet(() -> catalog.latestVersion(record.templateId()).orElse(null));
            rows.add(new AtAGlanceRow(
                    record.engagementId(),
                    record.templateId(),
                    record.appliedVersion(),
                    latest,
                    maybePending.isPresent(),
                    maybePending.map(p -> p.summary().headline()).orElse(null)
            ));
        }
        rows.sort(Comparator.comparing((AtAGlanceRow row) -> !row.pending())
                .thenComparing(row -> row.engagementId().value()));
        return rows;
    }

    /**
     * Makes the pending store match the index row. Removes the pending update
     * when there is no newer published version, or when the user has already
     * dismissed through that latest version. Otherwise stores a fresh pending
     * row from the applied version to the catalog's latest, with summaries
     * rebuilt from the template documents. Release notes are not available on
     * this path; only a publish event supplies them.
     */
    private void syncPending(EngagementTemplateRecord record) {
        Optional<Integer> latest = catalog.latestVersion(record.templateId());
        if (latest.isEmpty() || latest.get() <= record.appliedVersion() || record.isDismissedThrough(latest.get())) {
            pending.remove(record.engagementId());
            return;
        }
        Summaries summaries = buildSummaries(record.templateId(), record.appliedVersion(), latest.get(), null);
        pending.put(new PendingUpdate(
                record.engagementId(),
                record.firmId(),
                record.templateId(),
                record.appliedVersion(),
                latest.get(),
                summaries.accumulated(),
                summaries.hops()
        ));
    }

    /**
     * Builds both layers of the changelog for a version span. The accumulated
     * summary compares the start version directly with the end version, and
     * uses the publish release notes as its headline when those notes were
     * supplied. The hop list is one summary per adjacent step along the
     * published versions in between, always without release notes.
     */
    private Summaries buildSummaries(TemplateId templateId, int fromVersion, int toVersion, String releaseNotes) {
        ChangeSummary accumulated = summaryFor(templateId, fromVersion, toVersion, releaseNotes);
        List<Integer> chain = versionChain(templateId, fromVersion, toVersion);
        List<ChangeSummary> hops = new ArrayList<>();
        for (int i = 0; i < chain.size() - 1; i++) {
            hops.add(summaryFor(templateId, chain.get(i), chain.get(i + 1), null));
        }
        return new Summaries(accumulated, hops);
    }

    /**
     * Lists the published versions from the applied version through the target,
     * in ascending order. If either endpoint is missing from the catalog it is
     * inserted, so the hop list still connects the two ends.
     */
    private List<Integer> versionChain(TemplateId templateId, int fromVersion, int toVersion) {
        List<Integer> chain = new ArrayList<>();
        for (int version : catalog.versions(templateId)) {
            if (version >= fromVersion && version <= toVersion) {
                chain.add(version);
            }
        }
        if (chain.isEmpty() || chain.getFirst() != fromVersion) {
            chain.addFirst(fromVersion);
        }
        if (chain.getLast() != toVersion) {
            chain.add(toVersion);
        }
        return chain;
    }

    /**
     * Returns the human-readable summary for one version pair. When this call
     * has no release notes and the pair is already cached, the cached summary
     * is returned and the template documents are not diffed again. Otherwise
     * the two versions are diffed, summarized, checked so every line points at
     * a real diff path, and stored in the cache. A call that carries release
     * notes skips the cache read so those notes become the headline, then
     * writes that summary back for later reuse.
     */
    private ChangeSummary summaryFor(TemplateId templateId, int fromVersion, int toVersion, String releaseNotes) {
        if (releaseNotes == null || releaseNotes.isBlank()) {
            Optional<ChangeSummary> cached = cache.get(templateId, fromVersion, toVersion);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        JsonDiff diff = differ.diff(templateId, fromVersion, toVersion);
        ChangeSummary summary = summarizer.summarize(diff, releaseNotes);
        DeterministicSummarizer.assertGrounded(diff, summary);
        cache.put(summary);
        return summary;
    }

    /**
     * Holds the two summary layers produced for one version span: the single
     * accumulated summary, and the ordered list of adjacent hop summaries.
     */
    private record Summaries(ChangeSummary accumulated, List<ChangeSummary> hops) {}
}
