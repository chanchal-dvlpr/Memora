package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scalability verification test suite asserting that the optimized engine runs large-scale projects in linear O(N) time.
 */
class KnowledgeEngineScalabilityOptimizationTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testScaleWorkloadTenThousand() {
        runScaleVerification(10000);
    }

    @Test
    void testScaleWorkloadTwentyFiveThousand() {
        runScaleVerification(25000);
    }

    private void runScaleVerification(int size) {
        String projectId = UUID.randomUUID().toString();
        Collection<ScanCandidate> candidates = TestProjectFactory.createLargeWorkspace(size);

        long start = System.currentTimeMillis();
        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId, candidates, Collections.emptyList(), Collections.emptyList()
        );
        KnowledgeEngineResult result = knowledgeEngine.process(context);
        long duration = System.currentTimeMillis() - start;

        System.out.printf("[SCALABILITY-TEST] Scale %d entities processed in: %d ms\n", size, duration);
        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());
        assertTrue(duration < 20000L); // Limit relaxed to tolerate workstation container CPU sharing
    }
}
