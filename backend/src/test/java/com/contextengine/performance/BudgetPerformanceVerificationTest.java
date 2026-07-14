package com.contextengine.performance;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;
import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.knowledge.ranking.*;
import com.contextengine.application.knowledge.budget.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BudgetPerformanceVerificationTest {

    @Autowired
    private ContextBudgetManager budgetManager;

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    private RankingResult scaleRankingResult;

    @BeforeEach
    void setUp() {
        // Build 7,500 ranked fragments (1 project, 1 workspace, 2,000 files, 5,000 symbols, 498 dependencies)
        List<RankedFragment> scaleFragments = new ArrayList<>();
        scaleFragments.add(new RankedFragment(
            new ContextFragment("frag-proj", ContextFragmentType.PROJECT, "node-p", "project", "Project Node", "content", Collections.emptyMap(), 10),
            new RankingScore(100.0, Collections.emptyList())
        ));
        scaleFragments.add(new RankedFragment(
            new ContextFragment("frag-ws", ContextFragmentType.WORKSPACE, "node-w", "workspace", "Workspace Node", "content", Collections.emptyMap(), 10),
            new RankingScore(100.0, Collections.emptyList())
        ));

        for (int i = 1; i <= 2000; i++) {
            scaleFragments.add(new RankedFragment(
                new ContextFragment("frag-file-" + i, ContextFragmentType.FILE, "node-f-" + i, "src/File" + i + ".java", "File" + i + ".java", "content", Collections.emptyMap(), 10),
                new RankingScore(80.0, Collections.emptyList())
            ));
        }
        for (int i = 1; i <= 5000; i++) {
            int fileIndex = (i % 2000) + 1;
            scaleFragments.add(new RankedFragment(
                new ContextFragment("frag-sym-" + i, ContextFragmentType.SYMBOL, "node-s-" + i, "src/File" + fileIndex + ".java", "Sym" + i, "content", Collections.emptyMap(), 10),
                new RankingScore(60.0, Collections.emptyList())
            ));
        }
        for (int i = 1; i <= 498; i++) {
            scaleFragments.add(new RankedFragment(
                new ContextFragment("frag-dep-" + i, ContextFragmentType.DEPENDENCY, "node-d-" + i, "pom.xml", "dependency-" + i, "content", Collections.emptyMap(), 10),
                new RankingScore(50.0, Collections.emptyList())
            ));
        }

        scaleRankingResult = new RankingResult("p-scale", scaleFragments, new RankingStatistics(), Instant.now());
    }

    @Test
    void testPerformanceMetrics() {
        BudgetConfiguration config = new BudgetConfiguration();
        BudgetContext context = new BudgetContext(scaleRankingResult, config, "hash-initial", false, Collections.emptyList());

        // Warm up JIT compiler
        for (int w = 0; w < 5; w++) {
            budgetManager.budget(context);
        }

        // 1. Budgeting Latency
        long startBudget = System.currentTimeMillis();
        BudgetResult result = budgetManager.budget(context);
        long budgetDuration = System.currentTimeMillis() - startBudget;

        System.out.println("[PERFORMANCE-BUDGET] Context Budgeting Duration: " + budgetDuration + " ms");
        assertTrue(budgetDuration < 150, "Budgeting should take less than 150ms, took " + budgetDuration + " ms");
        assertEquals(7500, result.budgetedFragments().size());

        // 2. Validation Latency
        BudgetValidator validator = new BudgetValidator();
        long startVal = System.currentTimeMillis();
        BudgetValidationResult valResult = validator.validate(result.budgetedFragments(), config);
        long valDuration = System.currentTimeMillis() - startVal;

        System.out.println("[PERFORMANCE-BUDGET] Context Budget Validation Duration: " + valDuration + " ms");
        assertTrue(valDuration < 60, "Validation should take less than 60ms, took " + valDuration + " ms");
        assertTrue(valResult.isValid());

        // 3. Incremental Budgeting Latency (Partial updates with 5 dirty paths)
        BudgetContext incContext = new BudgetContext(
            scaleRankingResult,
            config,
            "hash-updated",
            true,
            List.of("src/File1.java", "src/File2.java", "src/File3.java", "src/File4.java", "src/File5.java")
        );

        // Warm up incremental flow JIT
        for (int w = 0; w < 5; w++) {
            budgetManager.budget(incContext);
        }

        long startInc = System.currentTimeMillis();
        BudgetResult incResult = budgetManager.budget(incContext);
        long incDuration = System.currentTimeMillis() - startInc;

        System.out.println("[PERFORMANCE-BUDGET] Incremental Budgeting Duration: " + incDuration + " ms");
        assertTrue(incDuration < 60, "Incremental budgeting should be fast, took " + incDuration + " ms");
    }

    @Test
    void testThreadSafetyAndConcurrency() throws Exception {
        BudgetConfiguration config = new BudgetConfiguration();
        BudgetContext context = new BudgetContext(scaleRankingResult, config);

        int threadCount = 30;
        int runsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<BudgetResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * runsPerThread; i++) {
            futures.add(executor.submit(() -> budgetManager.budget(context)));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

        for (Future<BudgetResult> future : futures) {
            BudgetResult result = future.get();
            assertNotNull(result);
            assertEquals(7500, result.budgetedFragments().size());
        }
    }

    @Test
    void testMemoryFootprintAndGC() {
        System.gc();
        long runtimeMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        BudgetConfiguration config = new BudgetConfiguration();
        BudgetContext context = new BudgetContext(scaleRankingResult, config);

        for (int i = 0; i < 50; i++) {
            BudgetResult result = budgetManager.budget(context);
            assertNotNull(result);
        }

        System.gc();
        long runtimeMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long differenceMb = (runtimeMemoryAfter - runtimeMemoryBefore) / (1024 * 1024);

        System.out.println("[PERFORMANCE-BUDGET] Memory footprint difference after 50 runs: " + differenceMb + " MB");
        assertTrue(differenceMb < 25, "Possible memory leak: difference is " + differenceMb + " MB");
    }

    @Test
    void testStrictVsLaxValidationModes() {
        // 1. STRICT Mode validation in KnowledgeEngineImpl should fail (returns FAILED status, null contextAssemblyResult)
        KnowledgeEngineConfiguration strictConfig = new KnowledgeEngineConfiguration(10, false, false, false, "STRICT");
        KnowledgeEngineContext strictContext = new KnowledgeEngineContext(
            "p-invalid", "w-invalid", "s-1", "", Instant.now(), Collections.emptyMap(), strictConfig,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeEngineResult strictResult = knowledgeEngine.process(strictContext);
        assertEquals("FAILED", strictResult.processingStatus());
        assertNull(strictResult.contextAssemblyResult());

        // 2. LAX Mode validation in KnowledgeEngineImpl should continue (returns COMPLETED status)
        KnowledgeEngineConfiguration laxConfig = new KnowledgeEngineConfiguration(10, false, false, true, "LAX");
        KnowledgeEngineContext laxContext = new KnowledgeEngineContext(
            "p-invalid", "w-invalid", "s-2", "", Instant.now(), Collections.emptyMap(), laxConfig,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeEngineResult laxResult = knowledgeEngine.process(laxContext);
        assertEquals("COMPLETED", laxResult.processingStatus());
        assertNotNull(laxResult.budgetResult());
    }
}
