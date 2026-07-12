package com.contextengine.domain.event;

import com.contextengine.domain.entity.AssumptionState;
import com.contextengine.domain.entity.DecisionState;
import com.contextengine.domain.entity.FeatureState;
import com.contextengine.domain.service.FormatEnum;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class EventTest {

    private final ProjectId projectId = ProjectId.generate();
    private final Path path = new Path("src/test");
    private final Instant now = Instant.now();

    @Test
    void testProjectRegistered() {
        ProjectRegistered event = new ProjectRegistered(projectId, path, now);
        assertThat(event.projectId()).isEqualTo(projectId);
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    void testProjectScannedValidation() {
        ProjectScanned event = new ProjectScanned(projectId, 10, 50, now);
        assertThat(event.filesScannedCount()).isEqualTo(10);
        
        assertThatThrownBy(() -> new ProjectScanned(projectId, -1, 50, now))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testModuleDiscovered() {
        ModuleId moduleId = ModuleId.generate();
        ModuleDiscovered event = new ModuleDiscovered(moduleId, projectId, "Module1", path, now);
        assertThat(event.moduleName()).isEqualTo("Module1");
    }

    @Test
    void testFeatureCreated() {
        FeatureId featureId = FeatureId.generate();
        FeatureCreated event = new FeatureCreated(featureId, projectId, "Feature1", Priority.HIGH, now);
        assertThat(event.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void testFeatureUpdated() {
        FeatureId featureId = FeatureId.generate();
        FeatureUpdated event = new FeatureUpdated(featureId, projectId, 85.5, FeatureState.IN_PROGRESS, now);
        assertThat(event.progressPercentage()).isEqualTo(85.5);
    }

    @Test
    void testTaskCreated() {
        TaskId taskId = TaskId.generate();
        TaskCreated event = new TaskCreated(taskId, null, projectId, Priority.MEDIUM, now);
        assertThat(event.taskId()).isEqualTo(taskId);
    }

    @Test
    void testTaskCompleted() {
        TaskId taskId = TaskId.generate();
        TaskCompleted event = new TaskCompleted(taskId, null, projectId, now);
        assertThat(event.projectId()).isEqualTo(projectId);
    }

    @Test
    void testDecisionRecorded() {
        DecisionId decisionId = DecisionId.generate();
        DecisionRecorded event = new DecisionRecorded(decisionId, projectId, path, DecisionState.PROPOSED, now);
        assertThat(event.status()).isEqualTo(DecisionState.PROPOSED);
    }

    @Test
    void testDecisionApproved() {
        DecisionId decisionId = DecisionId.generate();
        DecisionApproved event = new DecisionApproved(decisionId, projectId, path, "Author", now);
        assertThat(event.approvedBy()).isEqualTo("Author");
    }

    @Test
    void testBugDetected() {
        BugId bugId = BugId.generate();
        BugDetected event = new BugDetected(bugId, projectId, path, null, now);
        assertThat(event.bugId()).isEqualTo(bugId);
    }

    @Test
    void testConstraintAdded() {
        ConstraintId constraintId = ConstraintId.generate();
        ConstraintAdded event = new ConstraintAdded(constraintId, projectId, "dependency-license", now);
        assertThat(event.constraintType()).isEqualTo("dependency-license");
    }

    @Test
    void testAssumptionVerified() {
        AssumptionId assumptionId = AssumptionId.generate();
        AssumptionVerified event = new AssumptionVerified(assumptionId, projectId, AssumptionState.VERIFIED, now);
        assertThat(event.verificationStatus()).isEqualTo(AssumptionState.VERIFIED);
    }

    @Test
    void testDependencyUpdated() {
        DependencyId dependencyId = DependencyId.generate();
        DependencyUpdated event = new DependencyUpdated(
            dependencyId, projectId, "lib", new SemanticVersion("1.0.0"), new SemanticVersion("1.1.0"), now
        );
        assertThat(event.packageName()).isEqualTo("lib");
    }

    @Test
    void testContextGenerated() {
        SnapshotId snapshotId = SnapshotId.generate();
        ContextGenerated event = new ContextGenerated(snapshotId, projectId, 150, FormatEnum.MARKDOWN, now);
        assertThat(event.tokenCount()).isEqualTo(150);
    }

    @Test
    void testContextRetrieved() {
        ContextRetrieved event = new ContextRetrieved(projectId, "searchTerm", 5, now);
        assertThat(event.retrievedNodesCount()).isEqualTo(5);
    }

    @Test
    void testContextSnapshotCreated() {
        SnapshotId snapshotId = SnapshotId.generate();
        ContextSnapshotCreated event = new ContextSnapshotCreated(snapshotId, projectId, new Version(1), now);
        assertThat(event.graphVersion().value()).isEqualTo(1);
    }

    @Test
    void testContextVersionCreated() {
        SnapshotId snapshotId = SnapshotId.generate();
        Hash hash = new Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        ContextVersionCreated event = new ContextVersionCreated(snapshotId, new Version(1), hash, now);
        assertThat(event.deltaHash()).isEqualTo(hash);
    }

    @Test
    void testSearchExecuted() {
        SearchExecuted event = new SearchExecuted("queryText", "lexical", 15, 3, now);
        assertThat(event.searchTimeMs()).isEqualTo(15);
    }

    @Test
    void testKnowledgeGraphUpdated() {
        KnowledgeGraphUpdated event = new KnowledgeGraphUpdated(projectId, 5, 2, now);
        assertThat(event.nodesAddedCount()).isEqualTo(5);
    }

    @Test
    void testAIHandoffGenerated() {
        AIHandoffGenerated event = new AIHandoffGenerated(projectId, path, 250, now);
        assertThat(event.tokenCount()).isEqualTo(250);
    }
}
