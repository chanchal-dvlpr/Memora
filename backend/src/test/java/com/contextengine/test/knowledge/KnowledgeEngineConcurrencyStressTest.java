package com.contextengine.test.knowledge;

import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress test verifying concurrency safety across parallel scan engines under load.
 */
class KnowledgeEngineConcurrencyStressTest extends BaseIntegrationTest {

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    @Test
    void testConcurrentExecutionStress() throws InterruptedException {
        int workersCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(workersCount);
        CountDownLatch latch = new CountDownLatch(workersCount);
        List<KnowledgeEngineResult> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < workersCount; i++) {
            final String projectId = "project-" + i;
            executor.submit(() -> {
                try {
                    Collection<ScanCandidate> candidates = TestProjectFactory.createSingleFileProject();
                    KnowledgeEngineContext context = TestContextFactory.createEngineContext(
                        projectId, candidates, Collections.emptyList(), Collections.emptyList()
                    );
                    results.add(knowledgeEngine.process(context));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished);
        assertEquals(workersCount, results.size());
        for (KnowledgeEngineResult res : results) {
            assertNotNull(res);
            assertEquals("COMPLETED", res.processingStatus());
        }
    }
}
