package com.caseware.templateupdate.adapter.inmemory;

import com.caseware.templateupdate.app.PendingUpdateService;
import com.caseware.templateupdate.summary.DeterministicSummarizer;

/**
 * Builds a {@link com.caseware.templateupdate.app.PendingUpdateService} whose
 * ports are all in-memory. The demo asks for a ready-to-run service. Tests ask
 * for a harness so they can inspect the registry and the diff counter.
 */
public final class InMemoryPendingUpdateServiceFactory {
    /**
     * Hidden because this type is only a collection of factory methods.
     */
    private InMemoryPendingUpdateServiceFactory() {}

    /**
     * Returns a service wired to fresh in-memory indexes, the deterministic
     * summarizer, and the differ the caller already loaded with template JSON.
     */
    public static PendingUpdateService create(InMemoryTemplateDiffer differ) {
        return new PendingUpdateService(
                new InMemoryEngagementTemplateRegistry(),
                new InMemoryTemplateCatalog(),
                differ,
                new DeterministicSummarizer(),
                new InMemoryPendingUpdateStore(),
                new InMemorySummaryCache()
        );
    }

    /**
     * Returns the same wiring as {@link #create}, plus handles to each adapter
     * so a test can read the index and count diffs after the service runs.
     */
    public static Harness harness() {
        InMemoryTemplateDiffer differ = new InMemoryTemplateDiffer();
        InMemorySummaryCache cache = new InMemorySummaryCache();
        InMemoryEngagementTemplateRegistry registry = new InMemoryEngagementTemplateRegistry();
        InMemoryTemplateCatalog catalog = new InMemoryTemplateCatalog();
        InMemoryPendingUpdateStore pending = new InMemoryPendingUpdateStore();
        PendingUpdateService service = new PendingUpdateService(
                registry,
                catalog,
                differ,
                new DeterministicSummarizer(),
                pending,
                cache
        );
        return new Harness(service, differ, cache, registry, catalog, pending);
    }

    /**
     * The service and the in-memory adapters behind it. Tests use the adapters
     * to assert on stored versions and on how many diffs ran.
     */
    public record Harness(
            PendingUpdateService service,
            InMemoryTemplateDiffer differ,
            InMemorySummaryCache cache,
            InMemoryEngagementTemplateRegistry registry,
            InMemoryTemplateCatalog catalog,
            InMemoryPendingUpdateStore pending
    ) {}
}
