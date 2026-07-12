package com.contextengine.infrastructure;

import com.contextengine.infrastructure.cache.CacheManager;
import com.contextengine.infrastructure.cache.MemoryCache;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CacheInfrastructureTest {

    @Test
    void testMemoryCacheLRUBehavior() {
        MemoryCache<String, String> cache = new MemoryCache<>(2);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertThat(cache.size()).isEqualTo(2);

        // Fetch k1 to update access order
        cache.get("k1");

        // Insert k3, should evict k2 (the eldest and least recently used)
        cache.put("k3", "v3");

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get("k1")).isPresent();
        assertThat(cache.get("k3")).isPresent();
        assertThat(cache.get("k2")).isEmpty();
    }

    @Test
    void testCacheManager() {
        CacheManager manager = new CacheManager();
        MemoryCache<String, Integer> cache = manager.getOrCreateCache("symbols", 100);
        cache.put("key", 42);

        assertThat(manager.getOrCreateCache("symbols", 100).get("key")).contains(42);

        manager.evictAll();
        assertThat(cache.size()).isEqualTo(0);
    }
}
