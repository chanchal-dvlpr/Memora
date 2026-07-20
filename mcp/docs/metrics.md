# Metrics & Observability Telemetry Framework

This document describes the `MetricsManager` and benchmark telemetry components.

## 1. Metrics Collected

`MetricsManager` tracks real-time runtime telemetry:

- **`totalRequests`**: Total request volume received.
- **`successfulRequests`**: Total successfully completed requests.
- **`failedRequests`**: Total failed requests.
- **`timedOutRequests`**: Requests terminated due to timeouts.
- **`activeRequests`**: Current active requests being processed.
- **`queueLength`**: Current queued requests awaiting active execution.
- **`averageQueueWaitTimeMs`**: Mean time queued requests spent waiting before execution.
- **`peakConcurrentRequests`**: Highest concurrent active request count achieved.
- **`shutdownDurationMs`**: Time spent performing server shutdown.
- **`cleanupDurationMs`**: Time spent executing lifecycle cleanup hooks.
- **`averageExecutionDurationMs`**: Mean execution latency across completed requests.
- **`maxExecutionDurationMs`**: Maximum execution latency recorded.
- **`minExecutionDurationMs`**: Minimum execution latency recorded.

## 2. Benchmark Metrics

`BenchmarkRunner` captures percentile distributions (`averageMs`, `medianMs` p50, `p95Ms`, `maxMs`, `sampleCount`) for:
- `toolDispatchLatency`
- `resourceDispatchLatency`
- `promptDispatchLatency`
- `sessionMiddlewareOverhead`
- `securityMiddlewareOverhead`
- `registryLookupLatency`
- `contextPropagationOverhead`
