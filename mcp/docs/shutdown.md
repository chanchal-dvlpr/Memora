# Graceful Shutdown Architecture

This document describes the `ShutdownManager` component and shutdown lifecycle in the Memora MCP Server.

## 1. Shutdown States & Lifecycle

`ShutdownManager` coordinates graceful termination across three states:

```mermaid
graph TD
    Accepting[1. Accepting] --> Initiate[initiateShutdown]
    Initiate --> Draining[2. Draining]
    Draining --> RejectNew[Reject New Requests with ShutdownError]
    Draining --> DrainQueue[Drain Queue in ConcurrencyManager]
    Draining --> WaitActive[Wait for Active Requests to Complete or Timeout]
    WaitActive --> CleanupHooks[Execute Registered Cleanup Hooks]
    CleanupHooks --> Stopped[3. Stopped]
```

## 2. Draining Phase

1. Transitions state to `'draining'`.
2. Emits `shutdownStarted` reliability event.
3. Rejects all currently queued tasks in `ConcurrencyManager` with `ShutdownError`.
4. Polls `activeRequestsCount` until active tasks complete or `shutdownTimeoutMs` elapses.

## 3. Cleanup Hooks & Forced Exit

1. Executes all hooks registered via `registerCleanupHook(name, hook)`.
2. Measures `cleanupDurationMs`.
3. Transitions state to `'stopped'`.
4. Emits `shutdownCompleted` reliability event.
