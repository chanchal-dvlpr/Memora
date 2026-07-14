package com.contextengine.application.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.contextengine.application.knowledge.retrieval.RetrievalQuery;
import com.contextengine.application.knowledge.retrieval.RetrievalResult;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.infrastructure.cache.CacheManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowledgeOptimizerTest {

    private CacheManager cacheManager;
    private KnowledgeOptimizer optimizer;

    @BeforeEach
    void setUp() {
        cacheManager = new CacheManager();
        optimizer = new KnowledgeOptimizer(cacheManager);
    }

    @Test
    void testOptimizeRetrieval_CachesOnFirstCall() {
        ProjectId projectId = ProjectId.generate();
        RetrievalQuery query = new RetrievalQuery(projectId, "term", "App.java", 3);

        RetrievalResult mockResult = new RetrievalResult(List.of(), List.of());
        AtomicInteger invocationCount = new AtomicInteger(0);

        // First call - should invoke fallback supplier
        RetrievalResult result1 = optimizer.optimizeRetrieval(query, () -> {
            invocationCount.incrementAndGet();
            return mockResult;
        });

        assertThat(result1).isSameAs(mockResult);
        assertThat(invocationCount.get()).isEqualTo(1);

        // Second call - should fetch from cache and NOT invoke supplier again
        RetrievalResult result2 = optimizer.optimizeRetrieval(query, () -> {
            invocationCount.incrementAndGet();
            return new RetrievalResult(List.of(), List.of());
        });

        assertThat(result2).isSameAs(mockResult);
        assertThat(invocationCount.get()).isEqualTo(1);
    }

    @Test
    void testClearCache_EvictsEntries() {
        ProjectId projectId = ProjectId.generate();
        RetrievalQuery query = new RetrievalQuery(projectId, "term2", "App.java", 3);

        RetrievalResult mockResult = new RetrievalResult(List.of(), List.of());
        AtomicInteger invocationCount = new AtomicInteger(0);

        optimizer.optimizeRetrieval(query, () -> {
            invocationCount.incrementAndGet();
            return mockResult;
        });

        assertThat(invocationCount.get()).isEqualTo(1);

        // Evict
        optimizer.clearCache();

        // Subsequent call - should invoke supplier again
        optimizer.optimizeRetrieval(query, () -> {
            invocationCount.incrementAndGet();
            return mockResult;
        });

        assertThat(invocationCount.get()).isEqualTo(2);
    }
}
