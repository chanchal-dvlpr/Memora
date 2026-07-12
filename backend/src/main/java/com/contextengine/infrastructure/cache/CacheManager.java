package com.contextengine.infrastructure.cache;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Technical cache manager regulating and partitioning named LRU memory pools.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Cache Infrastructure
 * </p>
 */
public class CacheManager {

    private final Map<String, MemoryCache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * Resolves or registers a named memory cache.
     *
     * @param name name of the cache pool
     * @param capacity maximum pool capacity
     * @param <K> key type
     * @param <V> value type
     * @return the memory cache instance
     */
    @SuppressWarnings("unchecked")
    public <K, V> MemoryCache<K, V> getOrCreateCache(String name, int capacity) {
        Objects.requireNonNull(name, "Cache name must not be null");
        return (MemoryCache<K, V>) caches.computeIfAbsent(name, k -> new MemoryCache<Object, Object>(capacity));
    }

    /**
     * Evicts all managed cache pools.
     */
    public void evictAll() {
        caches.values().forEach(MemoryCache::clear);
    }
}
