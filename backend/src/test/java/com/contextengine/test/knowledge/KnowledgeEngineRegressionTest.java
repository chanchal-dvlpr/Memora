package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests confirming subsystem changes do not trigger regressions across the engine.
 */
class KnowledgeEngineRegressionTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testSubsystemCrossRegressions() {
        String projectId = java.util.UUID.randomUUID().toString();
        Collection<ScanCandidate> candidates = TestProjectFactory.createSingleFileProject();

        KnowledgeEngineContext context = TestContextFactory.createEngineContext(
            projectId, candidates, Collections.emptyList(), Collections.emptyList()
        );

        // Run process
        KnowledgeEngineResult result = knowledgeEngine.process(context);
        assertNotNull(result);
        assertEquals("COMPLETED", result.processingStatus());

        // Check outputs
        ContextAssemblyResult assemblyResult = result.contextAssemblyResult();
        assertNotNull(assemblyResult);
        assertTrue(assemblyResult.fragments().size() > 0);

        assertNotNull(result.rankingResult());
        assertNotNull(result.budgetResult());
        assertNotNull(result.snapshotComparisonResult());
        assertNotNull(result.searchResult());
    }
}
