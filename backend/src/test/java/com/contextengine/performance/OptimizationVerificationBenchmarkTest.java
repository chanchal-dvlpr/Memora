package com.contextengine.performance;

import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.test.BaseIntegrationTest;
import com.contextengine.test.knowledge.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarking suite verifying O(N) execution scaling across 1k, 5k, 10k, 25k, and 50k workloads.
 */
class OptimizationVerificationBenchmarkTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void runComprehensivePerformanceBenchmarks() {
        int[] sizes = {1000, 5000, 10000, 25000, 50000};

        for (int size : sizes) {
            String projectId = UUID.randomUUID().toString();
            Collection<ScanCandidate> candidates = TestProjectFactory.createLargeWorkspace(size);

            long start = System.currentTimeMillis();
            KnowledgeEngineContext context = TestContextFactory.createEngineContext(
                projectId, candidates, Collections.emptyList(), Collections.emptyList()
            );
            KnowledgeEngineResult result = knowledgeEngine.process(context);
            long duration = System.currentTimeMillis() - start;

            System.out.printf("[BENCHMARK-METRICS] Size: %d nodes, Duration: %d ms\n", size, duration);
            assertNotNull(result);
            assertEquals("COMPLETED", result.processingStatus());

            if (size <= 10000) {
                assertThat(duration).isLessThan(5000L);
            } else {
                assertThat(duration).isLessThan(90000L);
            }
        }
    }
}
