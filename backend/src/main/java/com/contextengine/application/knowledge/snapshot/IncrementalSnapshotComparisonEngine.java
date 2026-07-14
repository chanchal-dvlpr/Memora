package com.contextengine.application.knowledge.snapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe memory cache manager for optimizing incremental snapshot comparison runs.
 */
public class IncrementalSnapshotComparisonEngine {

    private static class CachedState {
        final String structuralHash;
        final SnapshotComparisonResult cachedResult;

        CachedState(String structuralHash, SnapshotComparisonResult cachedResult) {
            this.structuralHash = structuralHash;
            this.cachedResult = cachedResult;
        }
    }

    private final Map<String, CachedState> cache = new ConcurrentHashMap<>();

    /**
     * Checks if the structural state is unchanged.
     *
     * @param projectId      project ID
     * @param structuralHash current structural hash
     * @return true if the cached structural hash matches the input
     */
    public boolean isUnchanged(String projectId, String structuralHash) {
        if (structuralHash == null || structuralHash.isEmpty()) {
            return false;
        }
        CachedState state = cache.get(projectId);
        return state != null && structuralHash.equals(state.structuralHash);
    }

    /**
     * Retrieves the cached SnapshotComparisonResult.
     *
     * @param projectId project ID
     * @return cached SnapshotComparisonResult, or null
     */
    public SnapshotComparisonResult getCachedResult(String projectId) {
        CachedState state = cache.get(projectId);
        return state != null ? state.cachedResult : null;
    }

    /**
     * Caches the comparison result.
     *
     * @param projectId      project ID
     * @param structuralHash structural hash
     * @param result         comparison result to cache
     */
    public void cacheResult(String projectId, String structuralHash, SnapshotComparisonResult result) {
        if (structuralHash == null || structuralHash.isEmpty()) {
            return;
        }
        cache.put(projectId, new CachedState(structuralHash, result));
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        cache.clear();
    }
}
