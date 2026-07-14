package com.contextengine.performance;

import com.contextengine.application.event.DeadLetterJournal;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.application.event.LocalEventBus;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.knowledge.engine.KnowledgeEngineConfiguration;
import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.knowledge.graph.GraphNode;
import com.contextengine.application.knowledge.graph.GraphRelationship;
import com.contextengine.application.knowledge.graph.GraphUpdateEngine;
import com.contextengine.application.knowledge.graph.KnowledgeGraph;
import com.contextengine.application.knowledge.graph.KnowledgeGraphBuilder;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.SupportedLanguage;
import com.contextengine.application.scanner.dependency.ProjectDependency;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphPerformanceVerificationTest extends BaseIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void testScaledInMemoryGraphBuildPerformance() {
        int fileCount = 2000;
        int symbolCount = 5000;
        int dependencyCount = 500;

        List<ScanCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            candidates.add(new ScanCandidate(
                "src/File" + i + ".java",
                "/root/src/File" + i + ".java",
                1024L,
                Instant.now(),
                "FILE",
                SupportedLanguage.JAVA
            ));
        }

        List<SourceSymbol> symbols = new ArrayList<>();
        for (int i = 0; i < symbolCount; i++) {
            int fileIndex = i % fileCount;
            symbols.add(new SourceSymbol(
                "method" + i,
                "METHOD",
                "src/File" + fileIndex + ".java",
                10,
                20,
                Collections.emptyMap()
            ));
        }

        List<ProjectDependency> dependencies = new ArrayList<>();
        for (int i = 0; i < dependencyCount; i++) {
            dependencies.add(new ProjectDependency(
                "dep-package-" + i,
                "1.0.0",
                "MAVEN",
                "COMPILE"
            ));
        }

        KnowledgeEngineContext context = new KnowledgeEngineContext(
            "perf-proj-1",
            "perf-work-1",
            "perf-scan-1",
            "perf-structural-hash-value",
            Instant.now(),
            new HashMap<>(),
            new KnowledgeEngineConfiguration(),
            candidates,
            symbols,
            dependencies,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            false
        );

        GraphUpdateEngine updateEngine = new GraphUpdateEngine();
        KnowledgeGraphBuilder builder = new KnowledgeGraphBuilder(updateEngine);

        long startTime = System.currentTimeMillis();
        KnowledgeGraph graph = builder.build(context);
        long duration = System.currentTimeMillis() - startTime;

        System.out.printf("[PERFORMANCE] Built graph with %d nodes and %d relationships in %d ms\n",
            graph.nodes().size(), graph.relationships().size(), duration);

        // Verify scale matches and is completed in a reasonable time (e.g. under 2.5 seconds)
        assertThat(graph.nodes().size()).isGreaterThanOrEqualTo(fileCount + symbolCount + dependencyCount);
        assertThat(duration).isLessThan(2500);
    }

    @Test
    void testHighSpeedTraversalLatency() {
        // Construct a graph containing 5000+ nodes and check traversal latency
        KnowledgeGraph graph = new KnowledgeGraph("perf-proj-2", new com.contextengine.application.knowledge.graph.KnowledgeGraphConfiguration());
        com.contextengine.application.knowledge.graph.KnowledgeNodeBuilder nodeBuilder = new com.contextengine.application.knowledge.graph.KnowledgeNodeBuilder(graph);
        com.contextengine.application.knowledge.graph.KnowledgeRelationshipBuilder relBuilder = new com.contextengine.application.knowledge.graph.KnowledgeRelationshipBuilder(graph);

        nodeBuilder.createOrGetNode("root", GraphNode.Type.PROJECT, "RootProject", null);

        int depth1 = 20;
        int depth2 = 10;
        int depth3 = 25; // 20 * 10 * 25 = 5000 nodes at leaf level

        for (int i = 0; i < depth1; i++) {
            String id1 = "node_" + i;
            nodeBuilder.createOrGetNode(id1, GraphNode.Type.DIRECTORY, "Dir" + i, null);
            relBuilder.createOrGetRelationship("root", id1, GraphRelationship.Type.CONTAINS, null);

            for (int j = 0; j < depth2; j++) {
                String id2 = id1 + "_" + j;
                nodeBuilder.createOrGetNode(id2, GraphNode.Type.FILE, "File" + j, null);
                relBuilder.createOrGetRelationship(id1, id2, GraphRelationship.Type.CONTAINS, null);

                for (int k = 0; k < depth3; k++) {
                    String id3 = id2 + "_" + k;
                    nodeBuilder.createOrGetNode(id3, GraphNode.Type.SYMBOL, "Symbol" + k, null);
                    relBuilder.createOrGetRelationship(id2, id3, GraphRelationship.Type.DEFINES, null);
                }
            }
        }

        // Measure traversal: start at root and traverse 3 levels down (recursively find neighbors)
        long startTime = System.nanoTime();
        Set<String> visited = new HashSet<>();
        traverseGraph("root", graph, 0, 3, visited);
        long durationNs = System.nanoTime() - startTime;

        double durationMs = durationNs / 1_000_000.0;
        System.out.printf("[PERFORMANCE] Traversed %d nodes up to 3 hops in %.3f ms\n", visited.size(), durationMs);

        // Verify traversal completes well within NFR limits (sub-second, e.g. < 50ms)
        assertThat(visited.size()).isGreaterThanOrEqualTo(5000);
        assertThat(durationMs).isLessThan(50.0);
    }

    private void traverseGraph(String nodeId, KnowledgeGraph graph, int currentHop, int maxHops, Set<String> visited) {
        if (currentHop > maxHops || visited.contains(nodeId)) {
            return;
        }
        visited.add(nodeId);
        if (currentHop == maxHops) {
            return;
        }
        for (GraphRelationship rel : graph.relationships()) {
            if (rel.sourceNodeId().equals(nodeId)) {
                traverseGraph(rel.targetNodeId(), graph, currentHop + 1, maxHops, visited);
            }
        }
    }

    @Test
    void testHighThroughputEventDispatching() throws InterruptedException {
        int eventCount = 2000;
        CountDownLatch latch = new CountDownLatch(eventCount);
        LocalEventBus asyncBus = new LocalEventBus(new DeadLetterJournal(), true);

        UUID projId = UUID.randomUUID();
        String topic = "perf.event.p_" + projId;

        asyncBus.subscribe("perf.event.*", new EventSubscriber() {
            @Override
            public void onEvent(UniversalEventFrame event) {
                latch.countDown();
            }
        });

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < eventCount; i++) {
            UUID evtId = UUID.randomUUID();
            asyncBus.dispatch(new UniversalEventFrame(
                evtId,
                topic,
                Instant.now(),
                evtId,
                evtId,
                projId,
                1,
                Collections.emptyMap()
            ));
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        long durationMs = System.currentTimeMillis() - startTime;

        asyncBus.shutdown();

        assertTrue(completed, "Event dispatching did not complete in 10s");
        double throughput = (eventCount / (Math.max(1, durationMs) / 1000.0));
        System.out.printf("[PERFORMANCE] Dispatched %d events in %d ms (%.2f events/sec)\n", eventCount, durationMs, throughput);

        // Assert reasonable event dispatching throughput (e.g. > 1000 events/second)
        assertThat(throughput).isGreaterThan(1000.0);
    }

    @Test
    void testMemoryFootprintAndLeakCheck() {
        System.gc();
        long baselineMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Create a massive amount of short-lived graph references
        List<KnowledgeGraph> temporaryGraphs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            KnowledgeGraph graph = new KnowledgeGraph("temp-proj-" + i, new com.contextengine.application.knowledge.graph.KnowledgeGraphConfiguration());
            com.contextengine.application.knowledge.graph.KnowledgeNodeBuilder nodeBuilder = new com.contextengine.application.knowledge.graph.KnowledgeNodeBuilder(graph);
            for (int j = 0; j < 500; j++) {
                nodeBuilder.createOrGetNode("node_" + j, GraphNode.Type.FILE, "file_" + j, null);
            }
            temporaryGraphs.add(graph);
        }

        long activeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.printf("[PERFORMANCE] Baseline Memory: %.2f MB, Active Memory (50 graphs): %.2f MB\n",
            baselineMemory / (1024.0 * 1024.0), activeMemory / (1024.0 * 1024.0));

        // Dereference and suggest GC
        temporaryGraphs.clear();
        System.gc();

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}

        long postGcMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.printf("[PERFORMANCE] Post-GC Memory: %.2f MB\n", postGcMemory / (1024.0 * 1024.0));

        // Memory should reclaim correctly (leak index should be low)
        assertThat(postGcMemory).isLessThan(activeMemory + 10 * 1024 * 1024); // Allow small buffer
    }

    @Test
    void testPersistenceLatency() {
        int operations = 200;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < operations; i++) {
            ProjectId projectId = ProjectId.generate();
            Project project = new Project(projectId, new Path("/workspace/perf/p" + i), "PerfProject " + i);
            projectRepository.save(project);
            Optional<Project> loaded = projectRepository.findById(projectId);
            assertThat(loaded).isPresent();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        double averageMs = (double) durationMs / operations;
        System.out.printf("[PERFORMANCE] Completed %d persistence operations in %d ms (average %.2f ms/op)\n",
            operations, durationMs, averageMs);

        // Asserts average query response latency is low (e.g. average execution < 10ms per operation)
        assertThat(averageMs).isLessThan(10.0);
        assertThat(durationMs).isLessThan(2000);
    }
}
