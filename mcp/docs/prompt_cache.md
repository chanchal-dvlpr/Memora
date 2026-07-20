# MCP Prompt Caching Abstraction

This document defines the caching abstractions, models, and interfaces designed to optimize prompt execution.

---

## 1. Caching Interfaces

To support future caching implementations without introducing tight coupling to specific caching backends, the framework exposes the following core interfaces:

```typescript
export interface PromptCacheEntry {
  key: string;
  result: PromptInvocationResult;
  timestamp: number;
  expiresAt?: number;
}

export interface PromptCachePolicy {
  ttl?: number;
  maxEntries?: number;
}

export interface PromptCache {
  /**
   * Looks up a cached prompt invocation result.
   */
  lookup(key: string): Promise<PromptCacheEntry | undefined>;

  /**
   * Stores a prompt invocation result in the cache.
   */
  store(key: string, entry: PromptCacheEntry, policy?: PromptCachePolicy): Promise<void>;

  /**
   * Invalidates a specific cache entry.
   */
  invalidate(key: string): Promise<void>;

  /**
   * Clears all cache entries.
   */
  clear(): Promise<void>;
}
```

---

## 2. Expected Caching Lifecycles

In future production integrations, the cache will be used as follows:

1. **Key Generation**: Prompt parameters, requested template names, and execution metadata are hashed to generate a unique query key.
2. **Lookup**: Before executing the handler, the system queries `lookup(key)`. If found and not expired, the cached result is returned directly, avoiding execution latency.
3. **Storage**: Successful invocations store entries via `store(key, entry, policy)`. Policies enforce TTL and eviction rules (e.g. LRU).
4. **Invalidation**: Modifying project configurations or files triggers cache invalidation for affected prompts.
