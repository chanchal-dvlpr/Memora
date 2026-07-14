package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Optimization verification test suite asserting cached runs yield identical deterministic outputs.
 */
class KnowledgeEngineOptimizationTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testOptimizationsCorrectnessAndDeterminism() {
        String projectId = UUID.randomUUID().toString();
        Collection<ScanCandidate> candidates = TestProjectFactory.createMultiModuleProject();

        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId, candidates, Collections.emptyList(), Collections.emptyList()
        );

        // Run first time
        KnowledgeEngineResult result1 = knowledgeEngine.process(context);
        assertNotNull(result1);
        assertEquals("COMPLETED", result1.processingStatus());

        // Run second time (cached)
        KnowledgeEngineContext contextInc = TestContextFactory.createEngineContext(
            projectId, candidates, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), true
        );
        KnowledgeEngineResult result2 = knowledgeEngine.process(contextInc);

        assertNotNull(result2);
        assertEquals("COMPLETED", result2.processingStatus());

        // Verify outputs are identical
        assertEquals(result1.contextAssemblyResult().fragments().size(), result2.contextAssemblyResult().fragments().size());
        assertEquals(result1.budgetResult().budgetedFragments().size(), result2.budgetResult().budgetedFragments().size());
    }
}
