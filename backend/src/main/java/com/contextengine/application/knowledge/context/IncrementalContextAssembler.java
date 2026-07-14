package com.contextengine.application.knowledge.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service facilitating memory-only incremental caching and selective pruning of ContextFragments.
 */
public class IncrementalContextAssembler {

    private static final Map<String, ContextAssemblyResult> cache = new ConcurrentHashMap<>();
    private static final Map<String, String> hashes = new ConcurrentHashMap<>();

    /**
     * Checks if cached results are still valid using structural hash comparisons.
     */
    public boolean isUnchanged(String projectId, String currentHash) {
        if (currentHash == null || currentHash.isEmpty()) {
            return false;
        }
        return currentHash.equals(hashes.get(projectId));
    }

    /**
     * Fetches cached ContextAssemblyResult for a project.
     */
    public ContextAssemblyResult getCachedResult(String projectId) {
        return cache.get(projectId);
    }

    /**
     * Caches ContextAssemblyResult and associated structural hash.
     */
    public void cache(String projectId, ContextAssemblyResult result, String hash) {
        Objects.requireNonNull(projectId, "ProjectId must not be null");
        Objects.requireNonNull(result, "Result must not be null");
        cache.put(projectId, result);
        if (hash != null) {
            hashes.put(projectId, hash);
        }
    }

    /**
     * Prunes fragments affected by dirty paths from the cache.
     */
    public List<ContextFragment> prune(ContextAssemblyResult cached, Collection<String> dirtyPaths) {
        Objects.requireNonNull(cached, "Cached result must not be null");
        Objects.requireNonNull(dirtyPaths, "DirtyPaths must not be null");

        java.util.Set<String> dirtySet = (dirtyPaths instanceof java.util.Set) 
            ? (java.util.Set<String>) dirtyPaths 
            : new java.util.HashSet<>(dirtyPaths);

        List<ContextFragment> pruned = new ArrayList<>(cached.fragments().size());
        for (ContextFragment fragment : cached.fragments()) {
            boolean isDirty = false;
            if (dirtySet.contains(fragment.sourcePath())) {
                isDirty = true;
            }
            if (!isDirty) {
                pruned.add(fragment);
            }
        }
        return pruned;
    }

    /**
     * Clears cached contexts.
     */
    public void clearCache(String projectId) {
        cache.remove(projectId);
        hashes.remove(projectId);
    }
}
