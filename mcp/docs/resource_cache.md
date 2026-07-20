# MCP Resource Caching Abstraction

This document outlines the interfaces and abstractions designed to support resource caching strategies.

## Interfaces

### `CacheEntry<T>`
Defines the structure of a cached item:
- `key`: The cache key (usually the normalized URI).
- `data`: The cached resource contents.
- `createdAt`: Timestamp when entry was stored.
- `expiresAt`: Optional timestamp for TTL expiration.
- `etag`: Optional entity tag for validation.

### `CachePolicy`
Controls caching configuration parameters:
- `ttlMs`: Time-to-live limit in milliseconds.
- `maxSize`: Maximum size bounds.
- `useEtag`: Flag to enable entity tags checks.

### `ResourceCache<T>`
Primary interface mapping cache operations:
- `lookup(key: string): Promise<CacheEntry<T> | undefined>`
- `store(key: string, data: T, policy?: CachePolicy): Promise<void>`
- `invalidate(key: string): Promise<void>`
- `clear(): Promise<void>`
