package com.contextengine.infrastructure.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Generic Least-Recently-Used (LRU) in-memory cache implementation.
 *
 * @param <K> key type
 * @param <V> value type
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: Cache Infrastructure
 * </p>
 */
public class MemoryCache<K, V> {

    private final int maxEntries;
    private final Map<K, V> cacheMap;

    /**
     * Constructs a MemoryCache.
     *
     * @param maxEntries maximum allowed entries before eviction
     */
    public MemoryCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("Cache capacity must be positive");
        }
        this.maxEntries = maxEntries;
        this.cacheMap = Collections.synchronizedMap(new LinkedHashMap<K, V>(maxEntries, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MemoryCache.this.maxEntries;
            }
        });
    }

    /**
     * Stores a value mapped by key.
     *
     * @param key the key
     * @param value the value
     */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "Key must not be null");
        Objects.requireNonNull(value, "Value must not be null");
        cacheMap.put(key, value);
    }

    /**
     * Resolves value mapped by key.
     *
     * @param key the key
     * @return optional containing value, or empty
     */
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "Key must not be null");
        return Optional.ofNullable(cacheMap.get(key));
    }

    /**
     * Evicts value mapped by key.
     *
     * @param key the key to invalidate
     */
    public void invalidate(K key) {
        Objects.requireNonNull(key, "Key must not be null");
        cacheMap.remove(key);
    }

    /**
     * Clears all cache entries.
     */
    public void clear() {
        cacheMap.clear();
    }

    /**
     * Returns current size of the cache.
     *
     * @return cache size
     */
    public int size() {
        return cacheMap.size();
    }
}
