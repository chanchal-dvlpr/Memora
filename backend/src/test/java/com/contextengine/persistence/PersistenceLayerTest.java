package com.contextengine.persistence;

import com.contextengine.domain.entity.AISession;
import com.contextengine.domain.entity.Context;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.Module;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.entity.SessionState;
import com.contextengine.domain.entity.Task;
import com.contextengine.domain.entity.Workspace;
import com.contextengine.domain.repository.AISessionRepository;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.ContextSummary;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Hash;
import com.contextengine.domain.valueobject.ModuleId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SessionId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.TaskId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.TokenBudget;
import com.contextengine.domain.valueobject.Version;
import com.contextengine.domain.valueobject.WorkspaceId;
import com.contextengine.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenceLayerTest extends BaseIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private AISessionRepository aiSessionRepository;

    @Autowired
    private com.contextengine.application.port.TransactionManager transactionManager;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void testProjectSaveAndRetrieve() {
        ProjectId id = ProjectId.generate();
        Project project = new Project(id, new Path("/workspace/test"), "Test Project");
        project.activate();

        Workspace workspace = new Workspace(WorkspaceId.generate(), id);
        workspace.updateGitMetadata("main", "abcdef");
        workspace.trackPath(new Path("/workspace/test/src"));
        project.bindWorkspace(workspace);

        Module module = new Module(ModuleId.generate(), id, "CoreModule", new Path("src/core"));
        project.addModule(module);

        com.contextengine.domain.valueobject.Priority priority = com.contextengine.domain.valueobject.Priority.HIGH;
        Feature feature = new Feature(FeatureId.generate(), id, "CoolFeature", priority);
        project.addFeature(feature);

        Task task = new Task(TaskId.generate(), feature.id(), id, "CoolTask description", priority, Collections.emptyList());
        project.addTask(task);

        projectRepository.save(project);

        Optional<Project> retrieved = projectRepository.findById(id);
        assertThat(retrieved).isPresent();
        Project domainProj = retrieved.get();
        assertThat(domainProj.title()).isEqualTo("Test Project");
        assertThat(domainProj.state()).isEqualTo(ProjectState.ACTIVE);
        assertThat(domainProj.workspace()).isNotNull();
        assertThat(domainProj.workspace().activeBranch()).isEqualTo("main");
        assertThat(domainProj.workspace().trackedPaths()).hasSize(1);
        assertThat(domainProj.modules()).hasSize(1);
        assertThat(domainProj.features()).hasSize(1);
        assertThat(domainProj.tasks()).hasSize(1);
    }

    @Test
    void testContextSaveAndRetrieve() {
        ProjectId projectId = ProjectId.generate();
        Context context = new Context(projectId, new TokenBudget(1000));

        ContextSnapshot snapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(1),
            Timestamp.now(),
            new ContextSummary(5, 200, List.of("EntityA", "EntityB")),
            List.of(new EngineeringEvidence(new Path("src/Main.java"), 1, 10, new Hash("a".repeat(64))))
        );
        context.addSnapshot(snapshot);

        contextRepository.save(context);

        Optional<ContextSnapshot> retrieved = contextRepository.findSnapshotById(snapshot.id());
        assertThat(retrieved).isPresent();
        ContextSnapshot retrievedSnapshot = retrieved.get();
        assertThat(retrievedSnapshot.summary().tokenFootprint()).isEqualTo(200);
        assertThat(retrievedSnapshot.summary().primaryEntities()).containsExactly("EntityA", "EntityB");
        assertThat(retrievedSnapshot.evidences()).hasSize(1);
        assertThat(retrievedSnapshot.evidences().get(0).filePath().value()).isEqualTo("src/Main.java");
    }

    @Test
    void testContextSnapshotPruning() throws InterruptedException {
        ProjectId projectId = ProjectId.generate();
        Context context = new Context(projectId, new TokenBudget(1000));

        Timestamp now = Timestamp.now();
        ContextSnapshot oldSnapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(1),
            now,
            new ContextSummary(1, 10, List.of("A")),
            List.of(new EngineeringEvidence(new Path("src/A.java"), 1, 5, new Hash("a".repeat(64))))
        );
        context.addSnapshot(oldSnapshot);
        contextRepository.save(context);

        Thread.sleep(50);
        Timestamp cutoff = Timestamp.now();
        Thread.sleep(50);

        ContextSnapshot newSnapshot = new ContextSnapshot(
            SnapshotId.generate(),
            projectId,
            new Version(2),
            Timestamp.now(),
            new ContextSummary(1, 10, List.of("B")),
            List.of(new EngineeringEvidence(new Path("src/B.java"), 1, 5, new Hash("b".repeat(64))))
        );
        Context context2 = new Context(projectId, new TokenBudget(1000));
        context2.addSnapshot(newSnapshot);
        contextRepository.save(context2);

        int pruned = contextRepository.pruneOldSnapshots(projectId, cutoff);
        assertThat(pruned).isGreaterThanOrEqualTo(1);

        entityManager.clear();

        assertThat(contextRepository.findSnapshotById(oldSnapshot.id())).isEmpty();
        assertThat(contextRepository.findSnapshotById(newSnapshot.id())).isPresent();
    }

    @Test
    void testAISessionSaveAndRetrieve() {
        SessionId sessionId = SessionId.generate();
        ProjectId projectId = ProjectId.generate();
        AISession session = new AISession(sessionId, projectId, "VSCode");
        session.activate();

        aiSessionRepository.save(session);

        Optional<AISession> retrieved = aiSessionRepository.findById(sessionId);
        assertThat(retrieved).isPresent();
        AISession retrievedSession = retrieved.get();
        assertThat(retrievedSession.clientApp()).isEqualTo("VSCode");
        assertThat(retrievedSession.status()).isEqualTo(SessionState.ACTIVE);
    }

    @Test
    void testTransactionCommit() {
        ProjectId id = ProjectId.generate();
        Project project = new Project(id, new Path("/workspace/tx-commit"), "Tx Commit Project");

        transactionManager.executeInTransaction(() -> {
            projectRepository.save(project);
            return null;
        });

        Optional<Project> retrieved = projectRepository.findById(id);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().title()).isEqualTo("Tx Commit Project");
    }

    @Test
    void testTransactionRollback() {
        ProjectId id = ProjectId.generate();
        Project project = new Project(id, new Path("/workspace/tx-rollback"), "Tx Rollback Project");

        try {
            transactionManager.executeInTransaction(() -> {
                projectRepository.save(project);
                throw new RuntimeException("Simulated error to trigger rollback");
            });
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Simulated error to trigger rollback");
        }

        // Force clearing the persistence context so findById goes to the database
        entityManager.clear();

        Optional<Project> retrieved = projectRepository.findById(id);
        assertThat(retrieved).isEmpty();
    }
}
