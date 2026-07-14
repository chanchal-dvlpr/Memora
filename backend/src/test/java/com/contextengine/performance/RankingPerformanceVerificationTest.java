package com.contextengine.performance;

import com.contextengine.application.knowledge.context.ContextFragment;
import com.contextengine.application.knowledge.context.ContextFragmentType;
import com.contextengine.application.knowledge.context.ContextAssemblyResult;
import com.contextengine.application.knowledge.context.ContextStatistics;
import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.knowledge.ranking.*;
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
class RankingPerformanceVerificationTest {

    @Autowired
    private RelevanceRankingEngine rankingEngine;

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    private ContextAssemblyResult scaleAssemblyResult;

    @BeforeEach
    void setUp() {
        // Build 7,500 fragments (1 project, 1 workspace, 2,000 files, 5,000 symbols, 498 dependencies)
        List<ContextFragment> scaleFragments = new ArrayList<>();
        scaleFragments.add(new ContextFragment("frag-proj", ContextFragmentType.PROJECT, "node-p", "project", "Project Node", "content", Collections.emptyMap(), 10));
        scaleFragments.add(new ContextFragment("frag-ws", ContextFragmentType.WORKSPACE, "node-w", "workspace", "Workspace Node", "content", Collections.emptyMap(), 10));

        for (int i = 1; i <= 2000; i++) {
            scaleFragments.add(new ContextFragment("frag-file-" + i, ContextFragmentType.FILE, "node-f-" + i, "src/File" + i + ".java", "File" + i + ".java", "content", Collections.emptyMap(), 10));
        }
        for (int i = 1; i <= 5000; i++) {
            int fileIndex = (i % 2000) + 1;
            scaleFragments.add(new ContextFragment("frag-sym-" + i, ContextFragmentType.SYMBOL, "node-s-" + i, "src/File" + fileIndex + ".java", "Sym" + i, "content", Collections.emptyMap(), 10));
        }
        for (int i = 1; i <= 498; i++) {
            scaleFragments.add(new ContextFragment("frag-dep-" + i, ContextFragmentType.DEPENDENCY, "node-d-" + i, "pom.xml", "dependency-" + i, "content", Collections.emptyMap(), 10));
        }

        scaleAssemblyResult = new ContextAssemblyResult("p-scale", scaleFragments, new ContextStatistics(), Instant.now());
    }

    @Test
    void testPerformanceMetrics() {
        RankingConfiguration config = new RankingConfiguration();
        RankingContext context = new RankingContext(scaleAssemblyResult, config, "hash-initial", false, Collections.emptyList());

        // Warm up JIT compiler
        for (int w = 0; w < 5; w++) {
            rankingEngine.rank(context);
        }

        // 1. Ranking Latency
        long startRank = System.currentTimeMillis();
        RankingResult result = rankingEngine.rank(context);
        long rankDuration = System.currentTimeMillis() - startRank;

        System.out.println("[PERFORMANCE-RANKING] Relevance Ranking Duration: " + rankDuration + " ms");
        assertTrue(rankDuration < 150, "Ranking should take less than 150ms, took " + rankDuration + " ms");
        assertEquals(1000, result.rankedFragments().size()); // truncated to maximumCandidates default limit

        // 2. Validation Latency
        RankingValidator validator = new RankingValidator();
        long startVal = System.currentTimeMillis();
        RankingValidationResult valResult = validator.validate(result.rankedFragments(), config);
        long valDuration = System.currentTimeMillis() - startVal;

        System.out.println("[PERFORMANCE-RANKING] Relevance Validation Duration: " + valDuration + " ms");
        assertTrue(valDuration < 60, "Validation should take less than 60ms, took " + valDuration + " ms");
        assertTrue(valResult.isValid());

        // 3. Incremental Ranking Latency (Partial re-rank with 5 dirty paths)
        RankingContext incContext = new RankingContext(
            scaleAssemblyResult,
            config,
            "hash-updated",
            true,
            List.of("src/File1.java", "src/File2.java", "src/File3.java", "src/File4.java", "src/File5.java")
        );

        // Warm up incremental flow JIT
        for (int w = 0; w < 5; w++) {
            rankingEngine.rank(incContext);
        }

        long startInc = System.currentTimeMillis();
        RankingResult incResult = rankingEngine.rank(incContext);
        long incDuration = System.currentTimeMillis() - startInc;

        System.out.println("[PERFORMANCE-RANKING] Incremental Ranking Duration: " + incDuration + " ms");
        assertTrue(incDuration < 60, "Incremental ranking should be fast, took " + incDuration + " ms");
    }

    @Test
    void testThreadSafetyAndConcurrency() throws Exception {
        RankingConfiguration config = new RankingConfiguration();
        RankingContext context = new RankingContext(scaleAssemblyResult, config);

        int threadCount = 30;
        int runsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<RankingResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * runsPerThread; i++) {
            futures.add(executor.submit(() -> rankingEngine.rank(context)));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

        for (Future<RankingResult> future : futures) {
            RankingResult result = future.get();
            assertNotNull(result);
            assertEquals(1000, result.rankedFragments().size());
        }
    }

    @Test
    void testMemoryFootprintAndGC() {
        System.gc();
        long runtimeMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        RankingConfiguration config = new RankingConfiguration();
        RankingContext context = new RankingContext(scaleAssemblyResult, config);

        for (int i = 0; i < 50; i++) {
            RankingResult result = rankingEngine.rank(context);
            assertNotNull(result);
        }

        System.gc();
        long runtimeMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long differenceMb = (runtimeMemoryAfter - runtimeMemoryBefore) / (1024 * 1024);

        System.out.println("[PERFORMANCE-RANKING] Memory footprint difference after 50 runs: " + differenceMb + " MB");
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
        assertNotNull(laxResult.rankingResult());
    }
}
