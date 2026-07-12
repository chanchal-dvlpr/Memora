package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class EntityTest {

    @Test
    void testProjectLifecycleAndBinding() {
        ProjectId projectId = ProjectId.generate();
        Path path = new Path("src/test");
        Project project = new Project(projectId, path, "Test Project");

        assertThat(project.state()).isEqualTo(ProjectState.INITIALIZING);

        // Bind Workspace
        WorkspaceId workspaceId = WorkspaceId.generate();
        Workspace workspace = new Workspace(workspaceId, projectId);
        project.bindWorkspace(workspace);
        assertThat(project.workspace()).isEqualTo(workspace);

        // State Transitions
        project.activate();
        assertThat(project.state()).isEqualTo(ProjectState.ACTIVE);

        project.idle();
        assertThat(project.state()).isEqualTo(ProjectState.IDLE);

        project.suspend();
        assertThat(project.state()).isEqualTo(ProjectState.SUSPENDED);

        project.archive();
        assertThat(project.state()).isEqualTo(ProjectState.ARCHIVED);
    }

    @Test
    void testFeatureProgressAndCompletion() {
        FeatureId featureId = FeatureId.generate();
        ProjectId projectId = ProjectId.generate();
        Feature feature = new Feature(featureId, projectId, "Feature 1", Priority.HIGH);

        assertThat(feature.status()).isEqualTo(FeatureState.BACKLOG);
        
        feature.startProgress();
        assertThat(feature.status()).isEqualTo(FeatureState.IN_PROGRESS);

        // Update progress
        feature.updateProgress(50.0);
        assertThat(feature.progressPercentage()).isEqualTo(50.0);

        // Cannot complete if not at 100%
        assertThatThrownBy(feature::complete)
            .isInstanceOf(IllegalStateException.class);

        feature.updateProgress(100.0);
        feature.sendToReview();
        feature.complete();
        assertThat(feature.status()).isEqualTo(FeatureState.COMPLETED);
    }

    @Test
    void testTaskExecution() {
        TaskId taskId = TaskId.generate();
        ProjectId projectId = ProjectId.generate();
        Task task = new Task(taskId, null, projectId, "Task 1", Priority.MEDIUM, Collections.emptyList());

        assertThat(task.status()).isEqualTo(TaskState.READY);
        task.startExecution();
        assertThat(task.status()).isEqualTo(TaskState.IN_PROGRESS);
        task.complete();
        assertThat(task.status()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void testDecisionSupersedeInvariant() {
        DecisionId d1 = DecisionId.generate();
        ProjectId projectId = ProjectId.generate();
        Decision decision = new Decision(d1, projectId, "Decision 1", new Path("adr.md"));

        decision.approve();
        assertThat(decision.status()).isEqualTo(DecisionState.APPROVED);

        // Cannot supersede by itself (DI-3 / DI-6)
        assertThatThrownBy(() -> decision.supersede(d1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("DI-3 Invariant");
            
        DecisionId d2 = DecisionId.generate();
        decision.supersede(d2);
        assertThat(decision.status()).isEqualTo(DecisionState.SUPERSEDED);
        assertThat(decision.supersededBy()).isEqualTo(d2);
    }

    @Test
    void testBugInvariants() {
        BugId bugId = BugId.generate();
        ProjectId projectId = ProjectId.generate();

        // Must fail if both file path and commit hash are missing
        assertThatThrownBy(() -> new Bug(bugId, projectId, null, 0, 0, null))
            .isInstanceOf(IllegalArgumentException.class);
            
        Bug bug = new Bug(bugId, projectId, new Path("file.java"), 1, 10, null);
        assertThat(bug.status()).isEqualTo(BugState.UNRESOLVED);
        
        bug.startInvestigation();
        assertThat(bug.status()).isEqualTo(BugState.UNDER_INVESTIGATION);
        bug.resolve();
        assertThat(bug.status()).isEqualTo(BugState.RESOLVED);
    }
}
