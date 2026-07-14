package com.contextengine.application.knowledge.search;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe memory cache manager for optimizing search results using structural hashes.
 */
public class IncrementalSearchIndex {

    private static class CachedState {
        final String structuralHash;
        final SearchResult cachedResult;

        CachedState(String structuralHash, SearchResult cachedResult) {
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
     * Retrieves the cached SearchResult.
     *
     * @param projectId project ID
     * @return cached SearchResult, or null
     */
    public SearchResult getCachedResult(String projectId) {
        CachedState state = cache.get(projectId);
        return state != null ? state.cachedResult : null;
    }

    /**
     * Caches the search result.
     *
     * @param projectId      project ID
     * @param structuralHash structural hash
     * @param result         search result to cache
     */
    public void cacheResult(String projectId, String structuralHash, SearchResult result) {
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
