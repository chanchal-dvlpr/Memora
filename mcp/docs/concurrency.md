# Concurrency, Backpressure & Queue Management Architecture

This document describes the `ConcurrencyManager` component.

## 1. Concurrency Control Model

`ConcurrencyManager` manages request concurrency using a counting semaphore model with backpressure strategy support:

```mermaid
graph TD
    Incoming[Incoming Request] --> CheckActive{Active < MaxConcurrent?}
    CheckActive -- Yes --> IncrementActive[Increment Active Count & Execute]
    CheckActive -- No --> CheckStrategy{Backpressure Strategy?}
    CheckStrategy -- "reject" --> RejectDirect[Throw QueueOverflowError]
    CheckStrategy -- "queue" / "timeout" --> CheckQueue{Queued < MaxQueued?}
    CheckQueue -- Yes --> PushQueue[Enqueue Request Task with Wait Timeout Guard]
    CheckQueue -- No --> RejectOverflow[Throw QueueOverflowError]
```

## 2. Backpressure Strategies

- **`reject`**: Immediately rejects new incoming requests when active limit (`maxConcurrentRequests`) is reached by throwing `QueueOverflowError`.
- **`queue`**: Queues incoming requests up to `maxQueuedRequests` capacity until active slots free up.
- **`timeout`**: Queues requests up to `maxQueuedRequests`, but cancels queued tasks and throws `RequestTimeoutError` if waiting time exceeds `queueWaitTimeoutMs`.

## 3. Queue Inspection & Telemetry

- `getActiveRequestsCount()`: Returns active task count.
- `getQueuedRequestsCount()`: Returns queued task count.
- `getPeakConcurrentRequests()`: Peak active concurrency recorded.
- `getAverageQueueWaitTimeMs()`: Mean wait duration of queued tasks.
- `getQueueDetails()`: Returns snapshot array of queued items metadata.
