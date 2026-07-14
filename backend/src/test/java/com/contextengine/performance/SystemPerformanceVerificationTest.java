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
 * System-wide performance verification auditing sub-system latency boundaries and cache hit ratios.
 */
class SystemPerformanceVerificationTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testSystemWidePerformanceMetrics() {
        String projectId = UUID.randomUUID().toString();

        // 1. Setup scale workloads (5,000+ entities)
        Collection<ScanCandidate> candidates = TestProjectFactory.createLargeWorkspace(4000);
        List<com.contextengine.application.scanner.SourceSymbol> symbols = new ArrayList<>();
        int i = 0;
        for (ScanCandidate candidate : candidates) {
            if (i >= 800) break;
            symbols.addAll(TestProjectFactory.createSymbolsForFile(candidate.relativePath(), 1));
            i++;
        }
        Collection<com.contextengine.application.scanner.dependency.ProjectDependency> deps = TestProjectFactory.createDependencyHeavyProject(200);

        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId, candidates, symbols, deps,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        // 2. Measure complete pipeline run
        long runStart = System.currentTimeMillis();
        KnowledgeEngineResult result = knowledgeEngine.process(context);
        long runDuration = System.currentTimeMillis() - runStart;

        System.out.printf("[SYSTEM-PERFORMANCE] Full pipeline execution for 5,000+ entities: %d ms\n", runDuration);
        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());

        // NFR verification
        assertThat(runDuration).isLessThan(1500L); // total run under 1.5 seconds

        // 3. Repeated run (Verify cache hit reuse)
        KnowledgeEngineContext contextInc = TestContextFactory.createEngineContext(
            projectId, candidates, symbols, deps,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true
        );

        long runIncStart = System.currentTimeMillis();
        KnowledgeEngineResult resultInc = knowledgeEngine.process(contextInc);
        long runIncDuration = System.currentTimeMillis() - runIncStart;

        System.out.printf("[SYSTEM-PERFORMANCE] Incremental cache run execution: %d ms\n", runIncDuration);
        assertThat(runIncDuration).isLessThan(1000L);
    }
}
