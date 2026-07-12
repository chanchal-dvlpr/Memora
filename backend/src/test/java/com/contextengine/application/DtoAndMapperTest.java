package com.contextengine.application;

import com.contextengine.application.dto.*;
import com.contextengine.application.mapper.*;
import com.contextengine.domain.entity.*;
import com.contextengine.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DtoAndMapperTest {

    private final ProjectId projectId = ProjectId.generate();
    private final Path rootPath = new Path("/path/to/project");

    @Test
    void testProjectMapper() {
        Project domain = new Project(projectId, rootPath, "System Test");
        ProjectDto dto = ProjectMapper.toDto(domain);

        assertThat(dto.id()).isEqualTo(projectId.value().toString());
        assertThat(dto.absoluteRootPath()).isEqualTo(rootPath.value());
        assertThat(dto.title()).isEqualTo("System Test");

        Project mappedDomain = ProjectMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(projectId);
        assertThat(mappedDomain.rootDirectory()).isEqualTo(rootPath);
        assertThat(mappedDomain.title()).isEqualTo("System Test");
    }

    @Test
    void testWorkspaceMapper() {
        WorkspaceId workspaceId = WorkspaceId.generate();
        Workspace domain = new Workspace(workspaceId, projectId);
        domain.trackPath(new Path("src/main/java"));
        domain.updateGitMetadata("main", "abc123commit");

        WorkspaceDto dto = WorkspaceMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(workspaceId.value().toString());
        assertThat(dto.projectId()).isEqualTo(projectId.value().toString());
        assertThat(dto.trackedPaths()).containsExactly("src/main/java");
        assertThat(dto.activeBranch()).isEqualTo("main");
        assertThat(dto.activeCommitHash()).isEqualTo("abc123commit");

        Workspace mappedDomain = WorkspaceMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(workspaceId);
        assertThat(mappedDomain.projectId()).isEqualTo(projectId);
        assertThat(mappedDomain.trackedPaths()).contains(new Path("src/main/java"));
        assertThat(mappedDomain.activeBranch()).isEqualTo("main");
        assertThat(mappedDomain.activeCommitHash()).isEqualTo("abc123commit");
    }

    @Test
    void testFeatureMapper() {
        FeatureId featureId = FeatureId.generate();
        Feature domain = new Feature(featureId, projectId, "Feature A", Priority.HIGH);
        domain.startProgress();

        FeatureDto dto = FeatureMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(featureId.value().toString());
        assertThat(dto.projectId()).isEqualTo(projectId.value().toString());
        assertThat(dto.title()).isEqualTo("Feature A");
        assertThat(dto.status()).isEqualTo("IN_PROGRESS");
        assertThat(dto.priority()).isEqualTo("HIGH");

        Feature mappedDomain = FeatureMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(featureId);
        assertThat(mappedDomain.projectId()).isEqualTo(projectId);
        assertThat(mappedDomain.title()).isEqualTo("Feature A");
        assertThat(mappedDomain.status()).isEqualTo(FeatureState.IN_PROGRESS);
        assertThat(mappedDomain.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void testTaskMapper() {
        TaskId taskId = TaskId.generate();
        FeatureId featureId = FeatureId.generate();
        Task domain = new Task(taskId, featureId, projectId, "Task A", Priority.MEDIUM, List.of());
        
        TaskDto dto = TaskMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(taskId.value().toString());
        assertThat(dto.projectId()).isEqualTo(projectId.value().toString());
        assertThat(dto.featureId()).isEqualTo(featureId.value().toString());
        assertThat(dto.description()).isEqualTo("Task A");
        assertThat(dto.status()).isEqualTo("READY");
        assertThat(dto.priority()).isEqualTo("MEDIUM");

        Task mappedDomain = TaskMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(taskId);
        assertThat(mappedDomain.projectId()).isEqualTo(projectId);
        assertThat(mappedDomain.featureId()).isEqualTo(featureId);
        assertThat(mappedDomain.description()).isEqualTo("Task A");
        assertThat(mappedDomain.status()).isEqualTo(TaskState.READY);
        assertThat(mappedDomain.priority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void testDecisionMapper() {
        DecisionId decisionId = DecisionId.generate();
        Path markdown = new Path("docs/adr/0001-test.md");
        Decision domain = new Decision(decisionId, projectId, "ADR Title", markdown);
        domain.approve();
        domain.supersede(DecisionId.generate());

        DecisionDto dto = DecisionMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(decisionId.value().toString());
        assertThat(dto.projectId()).isEqualTo(projectId.value().toString());
        assertThat(dto.title()).isEqualTo("ADR Title");
        assertThat(dto.markdownPath()).isEqualTo(markdown.value());
        assertThat(dto.status()).isEqualTo("SUPERSEDED");
        assertThat(dto.supersededBy()).isNotNull();

        Decision mappedDomain = DecisionMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(decisionId);
        assertThat(mappedDomain.projectId()).isEqualTo(projectId);
        assertThat(mappedDomain.title()).isEqualTo("ADR Title");
        assertThat(mappedDomain.markdownPath()).isEqualTo(markdown);
        assertThat(mappedDomain.status()).isEqualTo(DecisionState.SUPERSEDED);
        assertThat(mappedDomain.supersededBy().value().toString()).isEqualTo(dto.supersededBy());
    }

    @Test
    void testDependencyMapper() {
        DependencyId dependencyId = DependencyId.generate();
        Path manifest = new Path("package.json");
        Dependency domain = new Dependency(dependencyId, projectId, "react", new SemanticVersion("18.2.0"), manifest);

        DependencyDto dto = DependencyMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(dependencyId.value().toString());
        assertThat(dto.projectId()).isEqualTo(projectId.value().toString());
        assertThat(dto.name()).isEqualTo("react");
        assertThat(dto.version()).isEqualTo("18.2.0");
        assertThat(dto.manifestPath()).isEqualTo(manifest.value());

        Dependency mappedDomain = DependencyMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(dependencyId);
        assertThat(mappedDomain.projectId()).isEqualTo(projectId);
        assertThat(mappedDomain.packageName()).isEqualTo("react");
        assertThat(mappedDomain.version()).isEqualTo(new SemanticVersion("18.2.0"));
        assertThat(mappedDomain.manifestPath()).isEqualTo(manifest);
    }

    @Test
    void testContextSnapshotMapper() {
        SnapshotId snapshotId = SnapshotId.generate();
        Instant now = Instant.now();
        ContextSnapshot domain = new ContextSnapshot(
            snapshotId,
            projectId,
            new Version(1),
            new Timestamp(now),
            new ContextSummary(10, 1500, List.of("react")),
            List.of()
        );

        ContextSnapshotDto dto = ContextSnapshotMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(snapshotId.value().toString());
        assertThat(dto.projectId()).isEqualTo(projectId.value().toString());
        assertThat(dto.tokensUsed()).isEqualTo(1500);
        assertThat(dto.timestamp()).isEqualTo(now.toString());

        ContextSnapshot mappedDomain = ContextSnapshotMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(snapshotId);
        assertThat(mappedDomain.projectId()).isEqualTo(projectId);
        assertThat(mappedDomain.summary().tokenFootprint()).isEqualTo(1500);
        assertThat(mappedDomain.createdAt().value()).isEqualTo(now);
    }

    @Test
    void testKnowledgeNodeMapper() {
        NodeId nodeId = NodeId.generate();
        KnowledgeNode domain = new KnowledgeNode(nodeId, "FILE", new Metadata(Map.of("name", "app.py")));

        KnowledgeNodeDto dto = KnowledgeNodeMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(nodeId.value().toString());
        assertThat(dto.type()).isEqualTo("FILE");
        assertThat(dto.attributes()).containsEntry("name", "app.py");

        KnowledgeNode mappedDomain = KnowledgeNodeMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(nodeId);
        assertThat(mappedDomain.type()).isEqualTo("FILE");
        assertThat(mappedDomain.attributes().get("name")).isEqualTo("app.py");
    }

    @Test
    void testKnowledgeRelationshipMapper() {
        RelationshipId relId = RelationshipId.generate();
        NodeId source = NodeId.generate();
        NodeId target = NodeId.generate();
        KnowledgeRelationship domain = new KnowledgeRelationship(relId, source, target, "CALLS", new GraphWeight(2.5));

        KnowledgeRelationshipDto dto = KnowledgeRelationshipMapper.toDto(domain);
        assertThat(dto.id()).isEqualTo(relId.value().toString());
        assertThat(dto.sourceNodeId()).isEqualTo(source.value().toString());
        assertThat(dto.targetNodeId()).isEqualTo(target.value().toString());
        assertThat(dto.type()).isEqualTo("CALLS");
        assertThat(dto.weight()).isEqualTo(2.5);

        KnowledgeRelationship mappedDomain = KnowledgeRelationshipMapper.toDomain(dto);
        assertThat(mappedDomain.id()).isEqualTo(relId);
        assertThat(mappedDomain.sourceNodeId()).isEqualTo(source);
        assertThat(mappedDomain.targetNodeId()).isEqualTo(target);
        assertThat(mappedDomain.type()).isEqualTo("CALLS");
        assertThat(mappedDomain.weight().value()).isEqualTo(2.5);
    }
}
