# Runtime Optimization & Operational Resilience

This document details runtime performance optimizations and error recovery mechanisms in the Memora MCP Server.

## 1. Registry Array Allocation Caching

In previous implementations, methods like `listTools()`, `listResources()`, and `listPrompts()` dynamically constructed new arrays, mapped entries, and sorted string titles on every invocation. 

`ToolRegistry`, `ResourceRegistry`, and `PromptRegistry` now utilize invalidating array caches (`cachedList`), which freeze and cache the sorted array output upon first read. The cache is automatically invalidated whenever a registration, unregistration, or clear operation occurs. This eliminates garbage collection pressure during frequent discovery requests.

## 2. Operational Error Recovery

- **Queue Cleanup on Shutdown**: Pending requests in queue are rejected with `ShutdownError` immediately upon entering the draining phase, preventing requests from hanging indefinitely.
- **Queue Wait Timeout Cleanup**: Timers scheduled for queued items are cleared immediately upon dequeuing or timing out, preventing memory leaks.
- **Resource Cleanup Guarantees**: Dispatcher pipelines execute middleware `finally` blocks to guarantee active request counters are accurately decremented regardless of unhandled exceptions.
