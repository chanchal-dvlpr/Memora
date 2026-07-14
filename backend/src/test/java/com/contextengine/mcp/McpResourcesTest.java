package com.contextengine.mcp;

import com.contextengine.domain.entity.Bug;
import com.contextengine.domain.entity.Constraint;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.Task;
import com.contextengine.domain.valueobject.BugId;
import com.contextengine.domain.valueobject.ConstraintId;
import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.TaskId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.Version;
import com.contextengine.mcp.McpException;
import com.contextengine.mcp.resource.McpResource;
import com.contextengine.mcp.resource.McpResourceContent;
import com.contextengine.mcp.resource.McpResourceService;
import com.contextengine.mcp.stub.StubContextRepository;
import com.contextengine.mcp.stub.StubKnowledgeGraphRepository;
import com.contextengine.mcp.stub.StubProjectRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpResourcesTest {

    private StubProjectRepository projectRepository;
    private CustomContextRepository contextRepository;
    private CustomGraphRepository graphRepository;
    private McpResourceService resourceService;

    private ProjectId projectId;
    private Project project;

    private static class CustomContextRepository extends StubContextRepository {
        Optional<ContextSnapshot> latestSnapshot = Optional.empty();

        @Override
        public Optional<ContextSnapshot> findLatestSnapshotForProject(ProjectId id) {
            return latestSnapshot;
        }
    }

    private static class CustomGraphRepository extends StubKnowledgeGraphRepository {
        List<KnowledgeNode> nodes = Collections.emptyList();

        @Override
        public Collection<KnowledgeNode> findNodesByProject(ProjectId id) {
            return nodes;
        }
    }

    @BeforeEach
    void setUp() {
        projectId = ProjectId.generate();
        project = new Project(projectId, new Path("/repo"), "Sample Project");
        project.activate();

        projectRepository = new StubProjectRepository() {
            @Override
            public Collection<Project> findAllActive() {
                return List.of(project);
            }
            @Override
            public Optional<Project> findById(ProjectId id) {
                if (projectId.equals(id)) {
                    return Optional.of(project);
                }
                return Optional.empty();
            }
        };
        contextRepository = new CustomContextRepository();
        graphRepository = new CustomGraphRepository();
        resourceService = new McpResourceService(projectRepository, contextRepository, graphRepository);
    }

    @Test
    void testListResourcesEmptyProject() {
        List<McpResource> resources = resourceService.listResources();
        // General resources: workspaces, architecture, constraints, roadmaps
        Assertions.assertEquals(4, resources.size());
        Assertions.assertEquals("contextengine://projects/" + projectId.value().toString() + "/workspaces", resources.get(0).uri());
    }

    @Test
    void testListResourcesWithElements() {
        Feature feature = new Feature(FeatureId.generate(), projectId, "Feature A", com.contextengine.domain.valueobject.Priority.HIGH);
        Task task = new Task(TaskId.generate(), null, projectId, "Task A description", com.contextengine.domain.valueobject.Priority.HIGH, Collections.emptyList());
        Bug bug = new Bug(BugId.generate(), projectId, new Path("/repo/main.java"), 10, 20, "abc1234");
        Decision decision = new Decision(DecisionId.generate(), projectId, "Decision A", new Path("/repo/adr-001.md"));
        Constraint constraint = new Constraint(ConstraintId.generate(), projectId, "Constraint statement", new com.contextengine.domain.valueobject.Metadata(Collections.singletonMap("limit", "license")));

        project.addFeature(feature);
        project.addTask(task);
        project.addBug(bug);
        project.addDecision(decision);
        project.addConstraint(constraint);

        KnowledgeNode mockNode = new KnowledgeNode(NodeId.generate(), "FILE", new com.contextengine.domain.valueobject.Metadata(Collections.singletonMap("name", "Class.java")));
        graphRepository.nodes = List.of(mockNode);

        ContextSnapshot mockSnapshot = new ContextSnapshot(
            SnapshotId.generate(), projectId, new Version(1), Timestamp.now(), new com.contextengine.domain.valueobject.ContextSummary(2, 10, List.of("Entity1")), Collections.emptyList()
        );
        contextRepository.latestSnapshot = Optional.of(mockSnapshot);

        List<McpResource> resources = resourceService.listResources();
        // 4 general + 1 feature + 1 task + 1 bug + 1 decision + 1 node + 1 snapshot = 10 resources
        Assertions.assertEquals(10, resources.size());
    }

    @Test
    void testReadResourceGeneral() {
        McpResourceContent content = resourceService.readResource("contextengine://projects/" + projectId.value().toString() + "/workspaces");
        Assertions.assertNotNull(content);
        Assertions.assertTrue(content.text().contains("Sample Project"));
    }

    @Test
    void testReadResourceNotFound() {
        Assertions.assertThrows(McpException.class, () -> 
            resourceService.readResource("contextengine://projects/" + projectId.value().toString() + "/features/nonexistent")
        );
    }
}
