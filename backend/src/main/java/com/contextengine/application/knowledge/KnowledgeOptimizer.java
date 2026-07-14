package com.contextengine.application.knowledge;

import com.contextengine.application.knowledge.retrieval.RetrievalQuery;
import com.contextengine.application.knowledge.retrieval.RetrievalResult;
import com.contextengine.infrastructure.cache.CacheManager;
import com.contextengine.infrastructure.cache.MemoryCache;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Coordinates in-memory caching and query optimizations for context generation.
 * <p>
 * Bounded Context: Context Assembly / Operational Infrastructure
 * Architecture Reference: High-Level Design (Part 1) Section 3 (Lexical and Semantic Cache)
 * Responsibility: Caches retrieval query subgraphs to prevent redundant DB access and expensive graph traversals.
 * Dependencies: {@link CacheManager}, {@link RetrievalQuery}, {@link RetrievalResult}.
 * </p>
 */
public class KnowledgeOptimizer {

    private final MemoryCache<RetrievalQuery, RetrievalResult> cache;

    /**
     * Constructs a KnowledgeOptimizer.
     *
     * @param cacheManager the system cache manager
     */
    public KnowledgeOptimizer(CacheManager cacheManager) {
        Objects.requireNonNull(cacheManager, "CacheManager must not be null");
        this.cache = cacheManager.getOrCreateCache("retrieval.queries", 500);
    }

    /**
     * Executes retrieval optimizations by loading from the LRU cache if available,
     * or executing the fallback query and caching the result.
     *
     * @param query the retrieval query parameters
     * @param fallback the database/graph traversal callback
     * @return the optimized retrieval result
     */
    public RetrievalResult optimizeRetrieval(RetrievalQuery query, Supplier<RetrievalResult> fallback) {
        Objects.requireNonNull(query, "Query must not be null");
        Objects.requireNonNull(fallback, "Fallback supplier must not be null");

        return cache.get(query).orElseGet(() -> {
            RetrievalResult result = fallback.get();
            cache.put(query, result);
            return result;
        });
    }

    /**
     * Forcefully evicts the retrieval cache.
     */
    public void clearCache() {
        cache.clear();
    }
}
