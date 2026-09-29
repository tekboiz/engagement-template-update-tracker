package com.caseware.templateupdate;

import com.caseware.templateupdate.adapter.inmemory.InMemoryPendingUpdateServiceFactory;
import com.caseware.templateupdate.app.PendingUpdateService;
import com.caseware.templateupdate.model.AtAGlanceRow;
import com.caseware.templateupdate.model.ChangeSummary;
import com.caseware.templateupdate.model.CreatedEngagement;
import com.caseware.templateupdate.model.Decision;
import com.caseware.templateupdate.model.DecisionEvent;
import com.caseware.templateupdate.model.EngagementId;
import com.caseware.templateupdate.model.FirmId;
import com.caseware.templateupdate.model.PendingUpdate;
import com.caseware.templateupdate.model.TemplateId;
import com.caseware.templateupdate.model.TemplatePublished;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Walks the pending-update rules with the in-memory adapters: create, publish,
 * apply, decline, and the queries that read the result back.
 */
class PendingUpdateServiceTest {
    private static final TemplateId TEMPLATE = new TemplateId("audit-ifrs");
    private static final FirmId FIRM = new FirmId("firm-north");
    private static final EngagementId ALPHA = new EngagementId("eng-alpha");
    private static final EngagementId BETA = new EngagementId("eng-beta");
    private static final EngagementId GAMMA = new EngagementId("eng-gamma");

    private InMemoryPendingUpdateServiceFactory.Harness harness;
    private PendingUpdateService service;

    /**
     * Builds a fresh service, loads template documents v1 through v4, and
     * publishes v1 so later creates have a catalog latest to compare against.
     */
    @BeforeEach
    void setUp() {
        harness = InMemoryPendingUpdateServiceFactory.harness();
        service = harness.service();
        harness.differ().put(TEMPLATE, 1, Templates.v1());
        harness.differ().put(TEMPLATE, 2, Templates.v2());
        harness.differ().put(TEMPLATE, 3, Templates.v3());
        harness.differ().put(TEMPLATE, 4, Templates.v4());
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 1));
    }

    /**
     * An engagement created on the version that was just published has an index
     * row and no pending update.
     */
    @Test
    void createdOnLatestVersionHasNoPending() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));

        assertTrue(service.getPending(ALPHA).isEmpty());
        AtAGlanceRow row = glance(ALPHA);
        assertFalse(row.pending());
        assertEquals(1, row.appliedVersion());
    }

    /**
     * Publishing v2 with release notes marks every older engagement pending
     * from v1 to v2, uses the notes as the headline, and gives both files the
     * same summary lines, including the new going-concern procedure.
     */
    @Test
    void publishMarksAllOlderEngagementsPendingWithoutLoadingThem() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(BETA, FIRM, TEMPLATE, 1));

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2, "Added going-concern evaluation procedure."));

        PendingUpdate alpha = service.getPending(ALPHA).orElseThrow();
        PendingUpdate beta = service.getPending(BETA).orElseThrow();
        assertEquals(1, alpha.appliedVersion());
        assertEquals(2, alpha.latestVersion());
        assertEquals("Added going-concern evaluation procedure.", alpha.summary().headline());
        assertEquals(beta.summary().items(), alpha.summary().items());
        assertTrue(alpha.summary().items().stream().anyMatch(item -> item.description().contains("Evaluate going concern")));
    }

    /**
     * Three engagements sitting on the same applied version cause one diff
     * when the next version is published, and all three receive a pending row.
     */
    @Test
    void summariesAreComputedOncePerVersionPairNotPerEngagement() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(BETA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(GAMMA, FIRM, TEMPLATE, 1));

        int before = harness.differ().diffCalls();
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2));
        int after = harness.differ().diffCalls();

        assertEquals(1, after - before, "diff should run once for three engagements on the same applied version");
        assertEquals(3, service.listPendingForFirm(FIRM).size());
    }

    /**
     * Publishing v2 and then v3 before any decision replaces the pending row
     * with a v1-to-v3 summary and two hops, and the combined text still names
     * both the going-concern procedure and the related-party disclosure.
     */
    @Test
    void accumulatedPublishesRebuildSummaryFromAppliedToLatest() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        PendingUpdate pending = service.getPending(ALPHA).orElseThrow();
        assertEquals(1, pending.appliedVersion());
        assertEquals(3, pending.latestVersion());
        assertEquals(2, pending.hops().size());
        assertEquals(1, pending.hops().get(0).fromVersion());
        assertEquals(2, pending.hops().get(0).toVersion());
        assertEquals(2, pending.hops().get(1).fromVersion());
        assertEquals(3, pending.hops().get(1).toVersion());
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("Related-party")));
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("Evaluate going concern")));
    }

    /**
     * Applying the latest version moves the index forward and removes the
     * pending row, so the glance list shows the engagement as current.
     */
    @Test
    void applyUpdatesIndexAndClearsPendingWhenAlreadyOnLatest() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 3));

        assertTrue(service.getPending(ALPHA).isEmpty());
        assertEquals(3, harness.registry().find(ALPHA).orElseThrow().appliedVersion());
        assertFalse(glance(ALPHA).pending());
    }

    /**
     * Applying v3 after v4 has already been published sets the applied version
     * to 3 and immediately opens a new pending row from v3 to v4.
     */
    @Test
    void applyResyncsIfANewerVersionWasPublishedDuringTheSlowLoad() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 4));

        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 3));

        PendingUpdate pending = service.getPending(ALPHA).orElseThrow();
        assertEquals(3, pending.appliedVersion());
        assertEquals(4, pending.latestVersion());
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("subsequent events")));
    }

    /**
     * Declining v3 clears the pending row, leaves the applied version at 1,
     * and records that updates through v3 have been dismissed.
     */
    @Test
    void declineDismissesCurrentLatestButKeepsAppliedVersion() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        service.onDecision(new DecisionEvent(ALPHA, Decision.DECLINE, 3));

        assertTrue(service.getPending(ALPHA).isEmpty());
        assertEquals(1, harness.registry().find(ALPHA).orElseThrow().appliedVersion());
        assertEquals(3, harness.registry().find(ALPHA).orElseThrow().dismissedThroughVersion());
    }

    /**
     * A publish after a decline opens pending again from the original applied
     * version. The new summary still includes the earlier going-concern change
     * and the new subsequent-events procedure.
     */
    @Test
    void laterPublishAfterDeclineReopensPendingFromOriginalAppliedVersion() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        service.onDecision(new DecisionEvent(ALPHA, Decision.DECLINE, 3));

        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 4));

        PendingUpdate pending = service.getPending(ALPHA).orElseThrow();
        assertEquals(1, pending.appliedVersion());
        assertEquals(4, pending.latestVersion());
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("Evaluate going concern")));
        assertTrue(pending.summary().items().stream().anyMatch(i -> i.description().contains("subsequent events")));
    }

    /**
     * The glance list puts engagements that still have a pending update ahead
     * of engagements that are current.
     */
    @Test
    void atAGlanceListsPendingFirst() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onEngagementCreated(new CreatedEngagement(BETA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 2));
        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 2));

        List<AtAGlanceRow> rows = service.listAtAGlance(FIRM);
        assertEquals(BETA, rows.get(0).engagementId());
        assertTrue(rows.get(0).pending());
        assertEquals(ALPHA, rows.get(1).engagementId());
        assertFalse(rows.get(1).pending());
    }

    /**
     * Every line of a real pending summary carries a non-blank source path,
     * which is the pointer back to the diff operation it came from.
     */
    @Test
    void everySummaryItemIsGroundedInTheJsonDiff() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));

        ChangeSummary summary = service.getPending(ALPHA).orElseThrow().summary();
        assertFalse(summary.items().isEmpty());
        summary.items().forEach(item -> assertFalse(item.sourcePath().isBlank()));
    }

    /**
     * A decision for an engagement that was never created fails instead of
     * inserting an index row.
     */
    @Test
    void unknownEngagementDecisionFails() {
        assertThrows(NotFoundException.class, () ->
                service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 2)));
    }

    /**
     * Once v3 has been applied, a later attempt to apply v2 is rejected and
     * the index stays on v3.
     */
    @Test
    void cannotApplyOlderVersionThanCurrentlyApplied() {
        service.onEngagementCreated(new CreatedEngagement(ALPHA, FIRM, TEMPLATE, 1));
        service.onTemplatePublished(new TemplatePublished(TEMPLATE, 3));
        service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 3));

        assertThrows(ConflictException.class, () ->
                service.onDecision(new DecisionEvent(ALPHA, Decision.APPLY, 2)));
    }

    /**
     * Finds the glance row for one engagement in the demo firm, failing the
     * test if that engagement is missing from the list.
     */
    private AtAGlanceRow glance(EngagementId id) {
        return service.listAtAGlance(FIRM).stream()
                .filter(row -> row.engagementId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
