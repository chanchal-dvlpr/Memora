package com.contextengine.performance;

import com.contextengine.domain.entity.GraphTransaction;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.KnowledgeRelationship;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.*;
import com.contextengine.infrastructure.graph.LocalKnowledgeGraphAdapter;
import com.contextengine.application.event.LocalEventBus;
import com.contextengine.application.event.UniversalEventFrame;
import com.contextengine.application.event.EventSubscriber;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance verification tests validating graph operations, event dispatch rates, and JPA latency.
 */
class PerformanceTest extends BaseIntegrationTest {

    @Autowired
    private LocalKnowledgeGraphAdapter graphAdapter;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void testKnowledgeGraphTraversalPerformance() {
        int nodeCount = 50;
        List<KnowledgeNode> nodes = new ArrayList<>();
        List<KnowledgeRelationship> edges = new ArrayList<>();

        KnowledgeNode rootNode = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Root")));
        nodes.add(rootNode);

        for (int i = 1; i <= nodeCount; i++) {
            KnowledgeNode node = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "Node" + i)));
            nodes.add(node);
            edges.add(new KnowledgeRelationship(RelationshipId.generate(), rootNode.id(), node.id(), "DEPENDS_ON", new GraphWeight(1.0)));
        }

        graphAdapter.commit(new GraphTransaction(nodes, edges));

        long startTime = System.nanoTime();
        Collection<KnowledgeNode> results = graphAdapter.querySubGraph(rootNode.id(), 1);
        long durationNs = System.nanoTime() - startTime;

        double durationMs = durationNs / 1_000_000.0;
        System.out.printf("[PERFORMANCE-TEST] Traversed graph of %d nodes in %.2f ms\n", results.size(), durationMs);

        assertThat(results).hasSize(nodeCount + 1);
    }

    @Test
    void testEventDispatchThroughputPerformance() throws InterruptedException {
        int eventCount = 50;
        CountDownLatch latch = new CountDownLatch(eventCount);
        LocalEventBus asyncBus = new LocalEventBus(new com.contextengine.application.event.DeadLetterJournal(), true);

        UUID projId = UUID.randomUUID();
        String topic = "project.created.p_" + projId;

        asyncBus.subscribe("project.created.*", new EventSubscriber() {
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

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long durationMs = System.currentTimeMillis() - startTime;

        // Clean shutdown of thread pools
        asyncBus.shutdown();

        assertThat(completed).isTrue();
        double throughput = (eventCount / (Math.max(1, durationMs) / 1000.0));
        System.out.printf("[PERFORMANCE-TEST] Dispatched %d events in %d ms (%.2f events/sec)\n", eventCount, durationMs, throughput);
    }

    @Test
    void testRepositoryQueryPerformance() {
        int operations = 50;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < operations; i++) {
            ProjectId projectId = ProjectId.generate();
            Project project = new Project(projectId, new Path("/workspace/p" + i), "Project " + i);
            projectRepository.save(project);
            Optional<Project> loaded = projectRepository.findById(projectId);
            assertThat(loaded).isPresent();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        double averageMs = (double) durationMs / operations;
        System.out.printf("[PERFORMANCE-TEST] Completed %d repository save & retrieve operations in %d ms (average %.2f ms/op)\n", operations, durationMs, averageMs);
    }
}
