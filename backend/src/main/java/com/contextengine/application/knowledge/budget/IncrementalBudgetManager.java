package com.contextengine.application.knowledge.budget;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe memory cache manager for optimizing incremental context budgeting runs.
 */
public class IncrementalBudgetManager {

    private static class CachedState {
        final String structuralHash;
        final BudgetResult cachedResult;
        final Map<String, BudgetedFragment> fragmentCache;

        CachedState(String structuralHash, BudgetResult cachedResult, Map<String, BudgetedFragment> fragmentCache) {
            this.structuralHash = structuralHash;
            this.cachedResult = cachedResult;
            this.fragmentCache = fragmentCache;
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
     * Retrieves the cached BudgetResult.
     *
     * @param projectId project ID
     * @return cached BudgetResult, or null
     */
    public BudgetResult getCachedResult(String projectId) {
        CachedState state = cache.get(projectId);
        return state != null ? state.cachedResult : null;
    }

    /**
     * Retrieves the cached individual budgeted fragments map.
     *
     * @param projectId project ID
     * @return map of fragment ID to BudgetedFragment
     */
    public Map<String, BudgetedFragment> getCachedFragments(String projectId) {
        CachedState state = cache.get(projectId);
        return state != null ? state.fragmentCache : Map.of();
    }

    /**
     * Caches the budget result.
     *
     * @param projectId      project ID
     * @param structuralHash structural hash
     * @param result         budget result to cache
     */
    public void cacheResult(String projectId, String structuralHash, BudgetResult result) {
        if (structuralHash == null || structuralHash.isEmpty()) {
            return;
        }
        Map<String, BudgetedFragment> fragmentCache = new ConcurrentHashMap<>();
        for (BudgetedFragment bf : result.budgetedFragments()) {
            fragmentCache.put(bf.rankedFragment().fragment().fragmentId(), bf);
        }
        cache.put(projectId, new CachedState(structuralHash, result, fragmentCache));
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        cache.clear();
    }
}
