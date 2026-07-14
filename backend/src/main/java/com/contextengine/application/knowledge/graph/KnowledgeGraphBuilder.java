package com.contextengine.application.knowledge.graph;

import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import com.contextengine.application.scanner.ScanCandidate;
import com.contextengine.application.scanner.SourceSymbol;
import com.contextengine.application.scanner.dependency.ProjectDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pipeline coordinating deterministic assembly, cache lookup, and delta-updating of KnowledgeGraph states.
 */
public class KnowledgeGraphBuilder {

    private final GraphUpdateEngine updateEngine;

    /**
     * Constructs a KnowledgeGraphBuilder.
     *
     * @param updateEngine caches and manages graph delta-updates
     */
    public KnowledgeGraphBuilder(GraphUpdateEngine updateEngine) {
        this.updateEngine = Objects.requireNonNull(updateEngine, "UpdateEngine must not be null");
    }

    /**
     * Builds or updates the KnowledgeGraph based on the context.
     *
     * @param context execution context carrying inputs and options
     * @return constructed KnowledgeGraph
     */
    public KnowledgeGraph build(KnowledgeEngineContext context) {
        Objects.requireNonNull(context, "Context must not be null");

        // 1. Check for complete reuse if structural hash matches
        if (updateEngine.isUnchanged(context.projectId(), context.structuralHash())) {
            KnowledgeGraph cached = updateEngine.getCachedGraph(context.projectId());
            if (cached != null) {
                return cached;
            }
        }

        KnowledgeGraph graph;
        KnowledgeGraphConfiguration graphConfig = new KnowledgeGraphConfiguration(
            "LAX".equalsIgnoreCase(context.configuration().validationMode()),
            true
        );

        // 2. Incremental partial reuse check
        if (context.isIncremental()) {
            KnowledgeGraph cached = updateEngine.getCachedGraph(context.projectId());
            if (cached != null) {
                // Collect dirty relative paths (both modified and deleted)
                Collection<String> dirtyPaths = new ArrayList<>();
                dirtyPaths.addAll(context.modifiedPaths());
                dirtyPaths.addAll(context.deletedPaths());

                graph = updateEngine.prune(cached, dirtyPaths);
            } else {
                graph = new KnowledgeGraph(context.projectId(), graphConfig);
            }
        } else {
            graph = new KnowledgeGraph(context.projectId(), graphConfig);
        }

        // 3. Establish Builders
        KnowledgeNodeBuilder nodeBuilder = new KnowledgeNodeBuilder(graph);
        KnowledgeRelationshipBuilder relBuilder = new KnowledgeRelationshipBuilder(graph);

        // 4. Create base Project and Workspace nodes
        String projectNodeId = "project:" + context.projectId();
        String workspaceNodeId = "workspace:" + context.workspaceId();

        nodeBuilder.createOrGetNode(projectNodeId, GraphNode.Type.PROJECT, "Project (" + context.projectId() + ")", null);
        nodeBuilder.createOrGetNode(workspaceNodeId, GraphNode.Type.WORKSPACE, "Workspace (" + context.workspaceId() + ")", null);
        relBuilder.createOrGetRelationship(projectNodeId, workspaceNodeId, GraphRelationship.Type.CONTAINS, null);

        // 5. Deterministic sorting of input lists
        List<ScanCandidate> sortedCandidates = new ArrayList<>(context.candidates());
        sortedCandidates.sort((a, b) -> a.relativePath().compareTo(b.relativePath()));

        List<SourceSymbol> sortedSymbols = new ArrayList<>(context.symbols());
        sortedSymbols.sort((a, b) -> {
            int cmp = a.filePath().compareTo(b.filePath());
            if (cmp != 0) return cmp;
            return a.name().compareTo(b.name());
        });

        List<ProjectDependency> sortedDeps = new ArrayList<>(context.dependencies());
        sortedDeps.sort((a, b) -> a.name().compareTo(b.name()));

        // 6. Build directory structures, file nodes, and CONTAINS relationships
        for (ScanCandidate candidate : sortedCandidates) {
            // Build parent tree segments
            buildDirectoryTree(candidate.relativePath(), workspaceNodeId, nodeBuilder, relBuilder);

            // Build file node
            String fileNodeId = "file:" + candidate.relativePath();
            Map<String, Object> fileProps = new HashMap<>();
            fileProps.put("absolutePath", candidate.absolutePath());
            fileProps.put("language", candidate.language().name());

            nodeBuilder.createOrGetNode(fileNodeId, GraphNode.Type.FILE, getFilename(candidate.relativePath()), fileProps);

            // Link parent directory or workspace to this file node
            String parentNodeId = getParentNodeId(candidate.relativePath(), workspaceNodeId);
            relBuilder.createOrGetRelationship(parentNodeId, fileNodeId, GraphRelationship.Type.CONTAINS, null);
        }

        // 7. Build symbol nodes and DEFINES relationships
        for (SourceSymbol symbol : sortedSymbols) {
            String fileNodeId = "file:" + symbol.filePath();
            // Only add symbols if the parent file node exists in the graph
            if (graph.getNode(fileNodeId) != null) {
                String symbolNodeId = "symbol:" + symbol.filePath() + ":" + symbol.name();
                Map<String, Object> symProps = new HashMap<>();
                symProps.put("filePath", symbol.filePath());
                symProps.put("kind", symbol.kind());
                symProps.put("startLine", symbol.startLine());
                symProps.put("endLine", symbol.endLine());

                nodeBuilder.createOrGetNode(symbolNodeId, GraphNode.Type.SYMBOL, symbol.name(), symProps);
                relBuilder.createOrGetRelationship(fileNodeId, symbolNodeId, GraphRelationship.Type.DEFINES, null);
            }
        }

        // 8. Build dependency nodes and DEPENDS_ON relationships (attached to Project node)
        for (ProjectDependency dep : sortedDeps) {
            String depNodeId = "dep:" + dep.name();
            Map<String, Object> depProps = new HashMap<>();
            depProps.put("version", dep.version());
            depProps.put("type", dep.type());
            depProps.put("scope", dep.scope());

            nodeBuilder.createOrGetNode(depNodeId, GraphNode.Type.DEPENDENCY, dep.name(), depProps);
            relBuilder.createOrGetRelationship(projectNodeId, depNodeId, GraphRelationship.Type.DEPENDS_ON, null);
        }

        // 9. Cache computed graph state
        updateEngine.cacheGraph(context.projectId(), graph, context.structuralHash());

        return graph;
    }

    private void buildDirectoryTree(String relativePath, String workspaceNodeId, KnowledgeNodeBuilder nodeBuilder, KnowledgeRelationshipBuilder relBuilder) {
        String[] parts = relativePath.split("/");
        if (parts.length <= 1) {
            return;
        }

        String currentPath = "";
        String parentNodeId = workspaceNodeId;

        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                currentPath += "/";
            }
            currentPath += parts[i];
            String dirNodeId = "dir:" + currentPath;

            Map<String, Object> props = new HashMap<>();
            props.put("relativePath", currentPath);

            nodeBuilder.createOrGetNode(dirNodeId, GraphNode.Type.DIRECTORY, parts[i], props);
            relBuilder.createOrGetRelationship(parentNodeId, dirNodeId, GraphRelationship.Type.CONTAINS, null);

            parentNodeId = dirNodeId;
        }
    }

    private String getParentNodeId(String relativePath, String workspaceNodeId) {
        String[] parts = relativePath.split("/");
        if (parts.length <= 1) {
            return workspaceNodeId;
        }
        StringBuilder parentPath = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                parentPath.append("/");
            }
            parentPath.append(parts[i]);
        }
        return "dir:" + parentPath.toString();
    }

    private String getFilename(String relativePath) {
        int idx = relativePath.lastIndexOf('/');
        return idx >= 0 ? relativePath.substring(idx + 1) : relativePath;
    }
}
