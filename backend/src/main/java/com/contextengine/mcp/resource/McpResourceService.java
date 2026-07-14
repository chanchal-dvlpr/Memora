package com.contextengine.mcp.resource;

import com.contextengine.domain.entity.Bug;
import com.contextengine.domain.entity.Constraint;
import com.contextengine.domain.entity.ContextSnapshot;
import com.contextengine.domain.entity.Decision;
import com.contextengine.domain.entity.Feature;
import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.entity.Project;
import com.contextengine.domain.entity.ProjectState;
import com.contextengine.domain.entity.Task;
import com.contextengine.domain.repository.ContextRepository;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.domain.repository.ProjectRepository;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.mcp.McpConstants;
import com.contextengine.mcp.McpException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that handles Model Context Protocol (MCP) Resource operations.
 *
 * <p>Architecture Reference: SRS-CE-CH4-4.4 / SRS-CE-FR-020 (MCP Resource Model)
 * Responsibility: List all discoverable resources across active projects and read contents of specific URIs.
 * Dependencies: {@link ProjectRepository}, {@link ContextRepository}, {@link KnowledgeGraphRepository}
 * Future Usage: Bound to the MCP protocol handler for servicing resources/list and resources/read JSON-RPC packets.
 */
public class McpResourceService {

    private static final Pattern URI_PATTERN_CANONICAL = Pattern.compile(
        "^contextengine://projects/([^/]+)/([^/]+)(?:/([^/]+))?$"
    );

    private static final Pattern URI_PATTERN_FR020 = Pattern.compile(
        "^context-engine://([^/]+)/([^/]+)(?:/([^/]+))?$"
    );

    private final ProjectRepository projectRepository;
    private final ContextRepository contextRepository;
    private final KnowledgeGraphRepository graphRepository;

    /**
     * Constructs a McpResourceService.
     *
     * @param projectRepository project repository
     * @param contextRepository context snapshot repository
     * @param graphRepository knowledge graph repository
     */
    public McpResourceService(
        ProjectRepository projectRepository,
        ContextRepository contextRepository,
        KnowledgeGraphRepository graphRepository
    ) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "ProjectRepository must not be null");
        this.contextRepository = Objects.requireNonNull(contextRepository, "ContextRepository must not be null");
        this.graphRepository = Objects.requireNonNull(graphRepository, "KnowledgeGraphRepository must not be null");
    }

    /**
     * Lists all discoverable resources across all active projects.
     *
     * @return a list of available resources
     */
    public List<McpResource> listResources() {
        List<McpResource> list = new ArrayList<>();
        Collection<Project> activeProjects = projectRepository.findAllActive();

        for (Project project : activeProjects) {
            String idStr = project.id().value().toString();

            // General Resources
            list.add(new McpResource(
                "contextengine://projects/" + idStr + "/workspaces",
                "Workspace Layout (" + project.title() + ")",
                "Repository paths and directory layouts registered for project: " + project.title(),
                "application/json"
            ));

            list.add(new McpResource(
                "contextengine://projects/" + idStr + "/architecture",
                "Architecture Layout (" + project.title() + ")",
                "System modules and components overview for project: " + project.title(),
                "application/json"
            ));

            list.add(new McpResource(
                "contextengine://projects/" + idStr + "/constraints",
                "Constraints Registry (" + project.title() + ")",
                "System architecture constraints and rules registered for project: " + project.title(),
                "application/json"
            ));

            list.add(new McpResource(
                "contextengine://projects/" + idStr + "/roadmaps",
                "Roadmaps (" + project.title() + ")",
                "Milestones and system features roadmap overview for project: " + project.title(),
                "application/json"
            ));

            // Features
            for (Feature feature : project.features()) {
                list.add(new McpResource(
                    "contextengine://projects/" + idStr + "/features/" + feature.id().value().toString(),
                    "Feature: " + feature.title(),
                    "Milestone progress: " + feature.progressPercentage() + "%",
                    "application/json"
                ));
            }

            // Tasks
            for (Task task : project.tasks()) {
                list.add(new McpResource(
                    "contextengine://projects/" + idStr + "/tasks/" + task.id().value().toString(),
                    "Task: " + task.description(),
                    "Status: " + task.status().toString(),
                    "application/json"
                ));
            }

            // Bugs
            for (Bug bug : project.bugs()) {
                list.add(new McpResource(
                    "contextengine://projects/" + idStr + "/bugs/" + bug.id().value().toString(),
                    "Bug manifesting in: " + (bug.filePath() != null ? bug.filePath().value() : bug.commitHash()),
                    "Status: " + bug.status().toString(),
                    "application/json"
                ));
            }

            // Decisions
            for (Decision decision : project.decisions()) {
                list.add(new McpResource(
                    "contextengine://projects/" + idStr + "/decisions/" + decision.id().value().toString(),
                    "ADR: " + decision.title(),
                    "Architectural Decision Record log",
                    "application/json"
                ));
            }

            // Graph nodes
            try {
                Collection<KnowledgeNode> nodes = graphRepository.findNodesByProject(project.id());
                for (KnowledgeNode node : nodes) {
                    list.add(new McpResource(
                        "contextengine://projects/" + idStr + "/knowledge-graph/nodes/" + node.id().value().toString(),
                        "Graph Node: " + node.type(),
                        "Attribute metadata for node: " + node.id().value().toString(),
                        "application/json"
                    ));
                }
            } catch (Exception ignored) {
                // Keep resilient
            }

            // Context snapshots
            try {
                Optional<ContextSnapshot> latestSnapshot = contextRepository.findLatestSnapshotForProject(project.id());
                latestSnapshot.ifPresent(snapshot -> list.add(new McpResource(
                    "contextengine://projects/" + idStr + "/context/snapshots/" + snapshot.id().value().toString(),
                    "Context Snapshot (" + snapshot.createdAt().value().toString() + ")",
                    "Stored context compilation metadata with " + snapshot.summary().tokenFootprint() + " tokens",
                    "text/plain"
                )));
            } catch (Exception ignored) {
                // Keep resilient
            }
        }

        return list;
    }

    /**
     * Reads the content of an MCP resource by URI.
     *
     * @param uri the resource URI
     * @return the content wrapper
     */
    public McpResourceContent readResource(String uri) {
        if (uri == null) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Resource URI must not be null");
        }

        String projectIdStr = null;
        String category = null;
        String entityId = null;

        Matcher m = URI_PATTERN_CANONICAL.matcher(uri);
        if (m.matches()) {
            projectIdStr = m.group(1);
            category = m.group(2);
            entityId = m.group(3);
        } else {
            m = URI_PATTERN_FR020.matcher(uri);
            if (m.matches()) {
                projectIdStr = m.group(1);
                category = m.group(2);
                entityId = m.group(3);
            }
        }

        if (projectIdStr == null || category == null) {
            throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Invalid Resource URI taxonomy: " + uri);
        }

        final String finalProjectIdStr = projectIdStr;
        ProjectId projectId = ProjectId.fromString(finalProjectIdStr);
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Project not found for ID: " + finalProjectIdStr));

        // Security check placeholder (e.g. workspace authorization check)
        if (project.state() == ProjectState.ARCHIVED) {
            throw new McpException(-32001, "Workspace access blocked: Project is archived.");
        }

        String contentText = resolveContentText(project, category, entityId);
        String mimeType = category.contains("snapshots") ? "text/plain" : "application/json";

        return new McpResourceContent(uri, mimeType, contentText);
    }

    private String resolveContentText(Project project, String category, String entityId) {
        switch (category.toLowerCase()) {
            case "workspaces":
            case "projects":
                return String.format(
                    "{\"projectId\":\"%s\",\"title\":\"%s\",\"rootPath\":\"%s\",\"state\":\"%s\"}",
                    project.id().value().toString(),
                    project.title(),
                    project.rootDirectory().value(),
                    project.state().toString()
                );

            case "architecture":
                return String.format(
                    "{\"projectId\":\"%s\",\"modulesCount\":%d}",
                    project.id().value().toString(),
                    project.modules().size()
                );

            case "constraints":
                return project.constraints().toString();

            case "roadmaps":
                return project.features().toString();

            case "features":
                if (entityId == null) {
                    return project.features().toString();
                }
                Feature feature = project.features().stream()
                    .filter(f -> f.id().value().toString().equals(entityId))
                    .findFirst()
                    .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Feature not found: " + entityId));
                return String.format(
                    "{\"id\":\"%s\",\"title\":\"%s\",\"progress\":%.2f}",
                    feature.id().value().toString(),
                    feature.title(),
                    feature.progressPercentage()
                );

            case "tasks":
                if (entityId == null) {
                    return project.tasks().toString();
                }
                Task task = project.tasks().stream()
                    .filter(t -> t.id().value().toString().equals(entityId))
                    .findFirst()
                    .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Task not found: " + entityId));
                return String.format(
                    "{\"id\":\"%s\",\"description\":\"%s\",\"status\":\"%s\"}",
                    task.id().value().toString(),
                    task.description(),
                    task.status().toString()
                );

            case "bugs":
                if (entityId == null) {
                    return project.bugs().toString();
                }
                Bug bug = project.bugs().stream()
                    .filter(b -> b.id().value().toString().equals(entityId))
                    .findFirst()
                    .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Bug not found: " + entityId));
                return String.format(
                    "{\"id\":\"%s\",\"manifest\":\"%s\",\"status\":\"%s\"}",
                    bug.id().value().toString(),
                    bug.filePath() != null ? bug.filePath().value() : bug.commitHash(),
                    bug.status().toString()
                );

            case "decisions":
                if (entityId == null) {
                    return project.decisions().toString();
                }
                Decision decision = project.decisions().stream()
                    .filter(d -> d.id().value().toString().equals(entityId))
                    .findFirst()
                    .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Decision not found: " + entityId));
                return String.format(
                    "{\"id\":\"%s\",\"title\":\"%s\",\"status\":\"%s\"}",
                    decision.id().value().toString(),
                    decision.title(),
                    decision.status().toString()
                );

            case "knowledge-graph":
            case "graph":
                if (entityId == null) {
                    return graphRepository.findNodesByProject(project.id()).toString();
                }
                KnowledgeNode node = graphRepository.findNodesByProject(project.id()).stream()
                    .filter(n -> n.id().value().toString().equals(entityId))
                    .findFirst()
                    .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Graph Node not found: " + entityId));
                return String.format(
                    "{\"id\":\"%s\",\"type\":\"%s\",\"attributes\":%s}",
                    node.id().value().toString(),
                    node.type(),
                    node.attributes().values().toString()
                );

            case "context":
                if (entityId != null && entityId.startsWith("snapshots/")) {
                    String snapId = entityId.substring("snapshots/".length());
                    ContextSnapshot snapshot = contextRepository.findSnapshotById(new SnapshotId(UUID.fromString(snapId)))
                        .orElseThrow(() -> new McpException(McpConstants.ERROR_INVALID_PARAMS, "Snapshot not found: " + snapId));
                    return String.format(
                        "Snapshot ID: %s\nToken Footprint: %d\nCreated At: %s",
                        snapshot.id().value().toString(),
                        snapshot.summary().tokenFootprint(),
                        snapshot.createdAt().value().toString()
                    );
                }
                throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Invalid Context Resource path: " + entityId);

            default:
                throw new McpException(McpConstants.ERROR_INVALID_PARAMS, "Unrecognized Resource path category: " + category);
        }
    }
}
