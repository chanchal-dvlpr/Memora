package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AggregateTest {

    @Test
    void testProjectCompositionInvariants() {
        ProjectId projectId = ProjectId.generate();
        Project project = new Project(projectId, new Path("src"), "Test Project");
        
        // Add Feature
        FeatureId featureId = FeatureId.generate();
        Feature feature = new Feature(featureId, projectId, "Feature 1", Priority.HIGH);
        project.addFeature(feature);
        
        // Add Task linked to Feature
        TaskId taskId = TaskId.generate();
        Task task = new Task(taskId, featureId, projectId, "Task 1", Priority.HIGH, Collections.emptyList());
        project.addTask(task);
        
        // Progress should be 0.0%
        assertThat(feature.progressPercentage()).isZero();
        
        // Start task execution
        project.startTaskExecution(taskId);
        
        // Complete task through the project root
        project.completeTask(taskId);
        
        // Progress should automatically recalculate to 100.0%
        assertThat(feature.progressPercentage()).isEqualTo(100.0);
    }

    @Test
    void testKnowledgeGraphZeroOrphanEdgesInvariant() {
        ProjectId projectId = ProjectId.generate();
        KnowledgeGraph graph = new KnowledgeGraph(projectId);
        
        NodeId sourceId = NodeId.generate();
        NodeId targetId = NodeId.generate();
        
        KnowledgeNode sourceNode = new KnowledgeNode(sourceId, "CODE_SYMBOL", Metadata.empty());
        KnowledgeNode targetNode = new KnowledgeNode(targetId, "CODE_SYMBOL", Metadata.empty());
        
        // Add relationship before nodes exist -> should fail referential integrity
        RelationshipId relId = RelationshipId.generate();
        KnowledgeRelationship relationship = new KnowledgeRelationship(
            relId, sourceId, targetId, "CALLS", new GraphWeight(1.0)
        );
        
        assertThatThrownBy(() -> graph.addRelationship(relationship))
            .isInstanceOf(IllegalArgumentException.class);
            
        // Add nodes
        graph.addNode(sourceNode);
        graph.addNode(targetNode);
        
        // Add relationship now -> should succeed
        graph.addRelationship(relationship);
        assertThat(graph.relationships()).contains(relationship);
        
        // Remove source node -> should cascade prune the relationship (Zero Orphan Edges)
        graph.removeNode(sourceId);
        assertThat(graph.nodes()).doesNotContain(sourceNode);
        assertThat(graph.relationships()).isEmpty();
    }

    @Test
    void testContextTokenBudgetEnforcement() {
        ProjectId projectId = ProjectId.generate();
        TokenBudget budget = new TokenBudget(100);
        Context context = new Context(projectId, budget);
        
        // Create Snapshot within budget
        SnapshotId s1 = SnapshotId.generate();
        ContextSummary summary1 = new ContextSummary(1, 99, List.of());
        ContextSnapshot snapshot1 = new ContextSnapshot(
            s1, projectId, new Version(1), Timestamp.now(), summary1, List.of()
        );
        context.addSnapshot(snapshot1);
        
        // Create Snapshot exceeding budget
        SnapshotId s2 = SnapshotId.generate();
        ContextSummary summary2 = new ContextSummary(1, 101, List.of());
        ContextSnapshot snapshot2 = new ContextSnapshot(
            s2, projectId, new Version(1), Timestamp.now(), summary2, List.of()
        );
        
        assertThatThrownBy(() -> context.addSnapshot(snapshot2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds the allowed token budget");
    }
}
