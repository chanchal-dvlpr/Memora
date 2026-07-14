package com.contextengine.application.knowledge.budget;

import static org.assertj.core.api.Assertions.assertThat;

import com.contextengine.application.knowledge.ranking.ContextRankedResult;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.event.DomainEventPublisher;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.TokenBudget;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenBudgetManagerTest {

    private TokenBudgetManager budgetManager;
    private List<Object> publishedEvents;
    private DomainEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        publishedEvents = new ArrayList<>();
        eventPublisher = publishedEvents::add;
        budgetManager = new TokenBudgetManager(eventPublisher);
    }

    @Test
    void testApplyBudget_FitsWithinBudget_NoPruning() {
        ProjectId projectId = ProjectId.generate();
        SnapshotId contextId = SnapshotId.generate();

        KnowledgeNode node1 = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "100"))
        );
        KnowledgeNode node2 = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "50"))
        );

        List<ContextRankedResult> candidates = List.of(
            new ContextRankedResult(node1, 0.9),
            new ContextRankedResult(node2, 0.8)
        );

        List<ContextRankedResult> result = budgetManager.applyBudget(candidates, new TokenBudget(200), projectId, contextId);

        assertThat(result).hasSize(2);
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void testApplyBudget_Pass1Pruning_DropsCommitsAndTasks() {
        ProjectId projectId = ProjectId.generate();
        SnapshotId contextId = SnapshotId.generate();

        KnowledgeNode fileNode = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "100"))
        );
        KnowledgeNode commitNode = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "COMMIT",
            new Metadata(Map.of("tokens", "80"))
        );

        List<ContextRankedResult> candidates = List.of(
            new ContextRankedResult(fileNode, 0.9),
            new ContextRankedResult(commitNode, 0.5)
        );

        // Budget is 120. Total footprint is 180. Pass 1 should drop commitNode and satisfy the budget.
        List<ContextRankedResult> result = budgetManager.applyBudget(candidates, new TokenBudget(120), projectId, contextId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).node().type()).isEqualTo("FILE");
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void testApplyBudget_Pass2Pruning_StubsDependencies() {
        ProjectId projectId = ProjectId.generate();
        SnapshotId contextId = SnapshotId.generate();

        KnowledgeNode fileNode = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "100"))
        );
        KnowledgeNode depNode = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "DEPENDENCY",
            new Metadata(Map.of("tokens", "80"))
        );

        List<ContextRankedResult> candidates = List.of(
            new ContextRankedResult(fileNode, 0.9),
            new ContextRankedResult(depNode, 0.5)
        );

        // Budget is 110. Total footprint is 180.
        // Pass 1 drops nothing.
        // Pass 2 stubs dependency node from 80 tokens to 5 tokens. Total footprint becomes 105 <= 110.
        List<ContextRankedResult> result = budgetManager.applyBudget(candidates, new TokenBudget(110), projectId, contextId);

        assertThat(result).hasSize(2);
        ContextRankedResult updatedDep = result.stream()
            .filter(r -> r.node().type().equalsIgnoreCase("DEPENDENCY"))
            .findFirst()
            .orElseThrow();
        assertThat(updatedDep.node().attributes().get("tokens")).isEqualTo("5");
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void testApplyBudget_Pass3Pruning_PrunesHops() {
        ProjectId projectId = ProjectId.generate();
        SnapshotId contextId = SnapshotId.generate();

        KnowledgeNode fileNode = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "100", "distance", "0"))
        );
        KnowledgeNode farNode = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "80", "distance", "3"))
        );

        List<ContextRankedResult> candidates = List.of(
            new ContextRankedResult(fileNode, 0.9),
            new ContextRankedResult(farNode, 0.5)
        );

        // Budget is 110. Pass 3 drops distance >= 2, leaving only fileNode.
        List<ContextRankedResult> result = budgetManager.applyBudget(candidates, new TokenBudget(110), projectId, contextId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).node().attributes().get("distance")).isEqualTo("0");
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void testApplyBudget_Pass4Pruning_KnapsackFallback() {
        ProjectId projectId = ProjectId.generate();
        SnapshotId contextId = SnapshotId.generate();

        KnowledgeNode node1 = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "100"))
        );
        KnowledgeNode node2 = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "80"))
        );
        KnowledgeNode node3 = new KnowledgeNode(
            com.contextengine.domain.valueobject.NodeId.generate(),
            "FILE",
            new Metadata(Map.of("tokens", "50"))
        );

        List<ContextRankedResult> candidates = List.of(
            new ContextRankedResult(node1, 0.9),
            new ContextRankedResult(node2, 0.7),
            new ContextRankedResult(node3, 0.6)
        );

        // Budget is 130.
        // Option A: node1 (100 tokens, value 0.9)
        // Option B: node2 + node3 (80+50=130 tokens, value 0.7+0.6=1.3)
        // Knapsack should pick node2 and node3 because they have higher combined value/score (1.3 > 0.9).
        List<ContextRankedResult> result = budgetManager.applyBudget(candidates, new TokenBudget(130), projectId, contextId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(r -> r.node().id()).containsExactlyInAnyOrder(node2.id(), node3.id());
        assertThat(publishedEvents).hasSize(1);
    }
}
