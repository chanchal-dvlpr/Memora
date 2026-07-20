# Session Cleanup Architecture

This document describes the `SessionCleanupManager` responsible for automated and manual session/context purging.

## 1. Cleanup Operations

`SessionCleanupManager` performs two critical cleanup actions in each pass:

1. **Expired & Closed Session Purging**:
   - Evaluates active sessions against the active `SessionExpirationPolicy` using `ExpirationEvaluator`.
   - Removes expired or closed sessions from `SessionRegistry`.

2. **Orphan Context Purging**:
   - Scans `ContextStore` for entries associated with session IDs that no longer exist in `SessionRegistry`.
   - Clears orphan context maps to prevent memory leaks in long-running processes.

## 2. Automated Cleanup Timer

- **`startAutoCleanup(intervalMs)`**: Configures a recurring timer (`setInterval`) executing `cleanup()` periodically.
- **`stopAutoCleanup()`**: Cancels the recurring timer (`clearInterval`).
- **`getStatistics()`**: Exposes `cleanupRuns`, `removedSessionsCount`, `removedContextsCount`, and `lastCleanupTimestamp`.
