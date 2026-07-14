package com.contextengine.application.knowledge.ranking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory-only cache provider to optimize incremental updates.
 */
public class IncrementalRankingEngine {

    private static class CachedState {
        final String structuralHash;
        final Map<String, RankedFragment> fragmentCache;
        final RankingResult cachedResult;

        CachedState(String structuralHash, Map<String, RankedFragment> fragmentCache, RankingResult cachedResult) {
            this.structuralHash = structuralHash;
            this.fragmentCache = fragmentCache;
            this.cachedResult = cachedResult;
        }
    }

    private final Map<String, CachedState> cache = new ConcurrentHashMap<>();

    /**
     * Checks if the structural state is unchanged for a given project.
     *
     * @param projectId      project ID
     * @param structuralHash current structural hash
     * @return true if cached structural hash matches the input
     */
    public boolean isUnchanged(String projectId, String structuralHash) {
        if (structuralHash == null || structuralHash.isEmpty()) {
            return false;
        }
        CachedState state = cache.get(projectId);
        return state != null && structuralHash.equals(state.structuralHash);
    }

    /**
     * Retrieves the cached RankingResult for the project.
     *
     * @param projectId project ID
     * @return cached RankingResult or null
     */
    public RankingResult getCachedResult(String projectId) {
        CachedState state = cache.get(projectId);
        return state != null ? state.cachedResult : null;
    }

    /**
     * Retrieves the cached ranked fragments for the project.
     *
     * @param projectId project ID
     * @return map of fragment ID to RankedFragment
     */
    public Map<String, RankedFragment> getCachedFragments(String projectId) {
        CachedState state = cache.get(projectId);
        return state != null ? state.fragmentCache : Map.of();
    }

    /**
     * Caches the computed ranking results.
     *
     * @param projectId       project ID
     * @param structuralHash  structural hash
     * @param rankedFragments list of ranked fragments to cache
     * @param result          completed ranking result
     */
    public void cacheResult(String projectId, String structuralHash, Collection<RankedFragment> rankedFragments, RankingResult result) {
        if (structuralHash == null || structuralHash.isEmpty()) {
            return;
        }
        Map<String, RankedFragment> fragmentCache = new HashMap<>();
        for (RankedFragment rf : rankedFragments) {
            fragmentCache.put(rf.fragment().fragmentId(), rf);
        }
        cache.put(projectId, new CachedState(structuralHash, fragmentCache, result));
    }

    /**
     * Clears all cached states.
     */
    public void clear() {
        cache.clear();
    }
}
