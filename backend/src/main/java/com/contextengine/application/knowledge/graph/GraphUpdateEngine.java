package com.contextengine.application.knowledge.graph;

import com.contextengine.application.knowledge.engine.KnowledgeEngineContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe engine managing cached project graph structures
 * and executing delta updates for incremental runs.
 */
public class GraphUpdateEngine {

    private static final Map<String, KnowledgeGraph> graphCache = new ConcurrentHashMap<>();
    private static final Map<String, String> graphHashMap = new ConcurrentHashMap<>();

    /**
     * Retrieves the cached graph for the target project.
     *
     * @param projectId unique project identifier
     * @return cached KnowledgeGraph, or null if missing
     */
    public KnowledgeGraph getCachedGraph(String projectId) {
        return graphCache.get(projectId);
    }

    /**
     * Checks if the cached graph structural hash matches the current workspace hash.
     *
     * @param projectId project target identifier
     * @param currentHash current workspace Merkle hash
     * @return true if unchanged, false otherwise
     */
    public boolean isUnchanged(String projectId, String currentHash) {
        if (currentHash == null || currentHash.isEmpty()) {
            return false;
        }
        return currentHash.equals(graphHashMap.get(projectId));
    }

    /**
     * Cache/registers a completed validated graph.
     *
     * @param projectId project target ID
     * @param graph completed KnowledgeGraph
     * @param hash corresponding workspace structural hash
     */
    public void cacheGraph(String projectId, KnowledgeGraph graph, String hash) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(graph, "Graph must not be null");
        graphCache.put(projectId, graph);
        if (hash != null) {
            graphHashMap.put(projectId, hash);
        }
    }

    /**
     * Prunes modified and deleted paths from a cached graph, preparing it for partial incremental rebuild.
     *
     * @param cached previously completed KnowledgeGraph
     * @param dirtyPaths collection of relative file paths that changed or were deleted
     * @return a pruned in-memory KnowledgeGraph containing only unaffected branches
     */
    public KnowledgeGraph prune(KnowledgeGraph cached, Collection<String> dirtyPaths) {
        Objects.requireNonNull(cached, "Cached graph must not be null");
        Objects.requireNonNull(dirtyPaths, "DirtyPaths must not be null");

        KnowledgeGraph pruned = new KnowledgeGraph(cached.projectId(), cached.configuration());
        Set<String> removedNodeIds = new HashSet<>();

        // 1. Identify and remove dirty nodes
        for (GraphNode node : cached.nodes()) {
            String filePathProp = (String) node.properties().get("filePath");
            boolean isDirtyFile = node.type() == GraphNode.Type.FILE && dirtyPaths.contains(node.id().replace("file:", ""));
            boolean isDirtyChild = filePathProp != null && dirtyPaths.contains(filePathProp);

            if (isDirtyFile || isDirtyChild) {
                removedNodeIds.add(node.id());
            } else {
                pruned.addNode(node);
            }
        }

        // 2. Re-populate relationships excluding pruned endpoints
        for (GraphRelationship rel : cached.relationships()) {
            if (!removedNodeIds.contains(rel.sourceNodeId()) && !removedNodeIds.contains(rel.targetNodeId())) {
                pruned.addRelationship(rel);
            }
        }

        pruned.statistics().setNodeCount(pruned.nodes().size());
        pruned.statistics().setRelationshipCount(pruned.relationships().size());

        return pruned;
    }

    /**
     * Clears the cache for a project (useful in testing).
     *
     * @param projectId target project ID
     */
    public void clearCache(String projectId) {
        graphCache.remove(projectId);
        graphHashMap.remove(projectId);
    }
}
