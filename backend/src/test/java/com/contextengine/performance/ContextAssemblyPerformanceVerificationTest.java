package com.contextengine.performance;

import com.contextengine.application.knowledge.context.*;
import com.contextengine.application.knowledge.engine.*;
import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.graph.KnowledgeGraphConfiguration;
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
class ContextAssemblyPerformanceVerificationTest {

    @Autowired
    private ContextAssemblyEngine contextAssemblyEngine;

    @Autowired
    private KnowledgeEngine knowledgeEngine;

    private KnowledgeGraph scaleGraph;

    @BeforeEach
    void setUp() {
        // Build a scale graph with 7,500+ nodes (1 Project, 1 Workspace, 2,000 files, 5,000 symbols, 500 dependencies)
        scaleGraph = new KnowledgeGraph("p-perf-scale", new KnowledgeGraphConfiguration());
        scaleGraph.addNode(new GraphNode("project:p-perf-scale", GraphNode.Type.PROJECT, "Scale Project", null));
        scaleGraph.addNode(new GraphNode("workspace:w-perf-scale", GraphNode.Type.WORKSPACE, "Scale Workspace", null));
        scaleGraph.addRelationship(new GraphRelationship("project:p-perf-scale", "workspace:w-perf-scale", GraphRelationship.Type.CONTAINS, null));

        // Add 2,000 files and 5,000 symbols
        int symbolIdCounter = 1;
        for (int i = 1; i <= 2000; i++) {
            String fileId = "file:src/File" + i + ".java";
            scaleGraph.addNode(new GraphNode(fileId, GraphNode.Type.FILE, "File" + i + ".java", Map.of("language", "JAVA")));
            scaleGraph.addRelationship(new GraphRelationship("workspace:w-perf-scale", fileId, GraphRelationship.Type.CONTAINS, null));

            // Distribute 5,000 symbols across these 2,000 files (2-3 symbols per file)
            int symbolsForThisFile = (i <= 1000) ? 3 : 2;
            for (int s = 0; s < symbolsForThisFile; s++) {
                String symbolId = "symbol:src/File" + i + ".java:sym" + symbolIdCounter;
                scaleGraph.addNode(new GraphNode(symbolId, GraphNode.Type.SYMBOL, "sym" + symbolIdCounter, Map.of("filePath", "src/File" + i + ".java", "kind", "METHOD")));
                scaleGraph.addRelationship(new GraphRelationship(fileId, symbolId, GraphRelationship.Type.DEFINES, null));
                symbolIdCounter++;
            }
        }

        // Add 500 dependencies
        for (int i = 1; i <= 500; i++) {
            String depId = "dep:dependency-" + i;
            scaleGraph.addNode(new GraphNode(depId, GraphNode.Type.DEPENDENCY, "dependency-" + i, Map.of("version", "1.0." + i)));
            scaleGraph.addRelationship(new GraphRelationship("project:p-perf-scale", depId, GraphRelationship.Type.DEPENDS_ON, null));
        }
    }

    @Test
    void testPerformanceMetrics() {
        ContextAssemblyConfiguration config = new ContextAssemblyConfiguration();
        ContextAssemblyContext context = new ContextAssemblyContext(
            scaleGraph,
            config,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            false,
            "hash-initial"
        );

        // Warm up JIT compiler before measuring to ensure typical latency bounds are met
        for (int w = 0; w < 5; w++) {
            contextAssemblyEngine.assemble(context);
        }

        // 1. Context Assembly Latency
        long startAssembly = System.currentTimeMillis();
        ContextAssemblyResult result = contextAssemblyEngine.assemble(context);
        long assemblyDuration = System.currentTimeMillis() - startAssembly;

        System.out.println("[PERFORMANCE-VERIFICATION] Context Assembly Duration: " + assemblyDuration + " ms");
        // Verify bounds: typical execution is sub-50ms (giving safety padding to slow CI nodes)
        assertTrue(assemblyDuration < 150, "Assembly should take less than 150ms, took " + assemblyDuration + " ms");
        assertEquals(7502, result.fragments().size());

        // 2. Context Validation Latency
        ContextValidator validator = new ContextValidator();
        long startVal = System.currentTimeMillis();
        ContextValidationResult valResult = validator.validate(result.fragments(), scaleGraph);
        long valDuration = System.currentTimeMillis() - startVal;

        System.out.println("[PERFORMANCE-VERIFICATION] Context Validation Duration: " + valDuration + " ms");
        assertTrue(valDuration < 60, "Validation should take less than 60ms, took " + valDuration + " ms");
        assertTrue(valResult.isValid());

        // 3. Incremental Assembly Latency (Modifying 5 files)
        ContextAssemblyContext incrementalContext = new ContextAssemblyContext(
            scaleGraph,
            config,
            Collections.emptyList(),
            List.of("src/File1.java", "src/File2.java", "src/File3.java", "src/File4.java", "src/File5.java"),
            Collections.emptyList(),
            true,
            "hash-updated"
        );

        // Warm up JIT compiler for incremental code paths too
        for (int w = 0; w < 5; w++) {
            contextAssemblyEngine.assemble(incrementalContext);
        }

        long startInc = System.currentTimeMillis();
        ContextAssemblyResult incResult = contextAssemblyEngine.assemble(incrementalContext);
        long incDuration = System.currentTimeMillis() - startInc;

        System.out.println("[PERFORMANCE-VERIFICATION] Incremental Assembly Duration: " + incDuration + " ms");
        assertTrue(incDuration < 60, "Incremental assembly should be fast, took " + incDuration + " ms");
        assertEquals(7502, incResult.fragments().size());
    }

    @Test
    void testThreadSafetyAndConcurrency() throws Exception {
        ContextAssemblyConfiguration config = new ContextAssemblyConfiguration();
        ContextAssemblyContext context = new ContextAssemblyContext(scaleGraph, config);

        int threadCount = 30;
        int runsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ContextAssemblyResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount * runsPerThread; i++) {
            futures.add(executor.submit(() -> contextAssemblyEngine.assemble(context)));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));

        for (Future<ContextAssemblyResult> future : futures) {
            ContextAssemblyResult result = future.get();
            assertNotNull(result);
            assertEquals(7502, result.fragments().size());
        }
    }

    @Test
    void testMemoryFootprintAndGC() {
        System.gc();
        long runtimeMemoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ContextAssemblyConfiguration config = new ContextAssemblyConfiguration();
        ContextAssemblyContext context = new ContextAssemblyContext(scaleGraph, config);

        for (int i = 0; i < 50; i++) {
            ContextAssemblyResult result = contextAssemblyEngine.assemble(context);
            assertNotNull(result);
        }

        System.gc();
        long runtimeMemoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long differenceMb = (runtimeMemoryAfter - runtimeMemoryBefore) / (1024 * 1024);

        System.out.println("[PERFORMANCE-VERIFICATION] Memory footprint difference after 50 runs: " + differenceMb + " MB");
        assertTrue(differenceMb < 25, "Possible memory leak: difference is " + differenceMb + " MB");
    }

    @Test
    void testStrictVsLaxValidationModes() {
        // Trigger structural hash warning by passing an empty structural hash
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
        KnowledgeEngineConfiguration laxConfig = new KnowledgeEngineConfiguration(10, false, false, false, "LAX");
        KnowledgeEngineContext laxContext = new KnowledgeEngineContext(
            "p-invalid", "w-invalid", "s-2", "", Instant.now(), Collections.emptyMap(), laxConfig,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false
        );

        KnowledgeEngineResult laxResult = knowledgeEngine.process(laxContext);
        assertEquals("COMPLETED", laxResult.processingStatus());
        assertNotNull(laxResult.contextAssemblyResult());
    }
}
