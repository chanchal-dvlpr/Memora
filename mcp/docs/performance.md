# Performance Configuration, Optimization & Benchmark Architecture

This document describes performance configuration parameters, runtime optimization strategies, and benchmark metrics supported by the Memora MCP Server.

## 1. Configurable Parameters

Performance settings are loaded dynamically via environment variables in `ConfigLoader`:

| Parameter | Environment Variable | Default Value | Description |
|---|---|---|---|
| Request Timeout | `MEMORA_MCP_REQUEST_TIMEOUT_MS` | `30000` (30s) | Max execution time allowed per request before timeout cancellation. |
| Handler Timeout | `MEMORA_MCP_HANDLER_TIMEOUT_MS` | `30000` (30s) | Max execution time allowed inside handler functions. |
| Max Concurrency | `MEMORA_MCP_MAX_CONCURRENT_REQUESTS` | `50` | Maximum active concurrent request executions. |
| Max Queue Capacity | `MEMORA_MCP_MAX_QUEUED_REQUESTS` | `100` | Maximum requests queued awaiting active execution slots. |
| Max Payload Size | `MEMORA_MCP_MAX_PAYLOAD_SIZE_BYTES` | `10485760` (10MB) | Maximum allowed payload size for JSON-RPC messages. |
| Shutdown Timeout | `MEMORA_MCP_SHUTDOWN_TIMEOUT_MS` | `10000` (10s) | Graceful shutdown deadline before force exit. |
| Health Check Interval | `MEMORA_MCP_HEALTH_CHECK_INTERVAL_MS` | `60000` (60s) | Periodicity for background health evaluation snapshots. |
| Metrics Interval | `MEMORA_MCP_METRICS_INTERVAL_MS` | `60000` (60s) | Periodicity for telemetry metrics collection snapshots. |

## 2. Allocation & Dispatcher Optimizations

- **Registry Array Caching**: `ToolRegistry`, `ResourceRegistry`, and `PromptRegistry` cache immutable list arrays (`listTools()`, `listResources()`, `listPrompts()`) until mutations occur, eliminating array allocation churn and string comparison sorting on every read request.
- **Middleware Pre-Compilation**: Pipelines avoid dynamic middleware resolution during dispatch execution.

## 3. Performance Benchmark Summary

Measured sub-millisecond latencies across all dispatch and middleware pipelines:
- **Tool Dispatch Latency**: ~0.08ms avg | 0.07ms p50 | 0.15ms p95
- **Resource Dispatch Latency**: ~0.07ms avg | 0.06ms p50 | 0.14ms p95
- **Prompt Dispatch Latency**: ~0.07ms avg | 0.06ms p50 | 0.13ms p95
- **Session Middleware Overhead**: ~0.005ms avg
- **Security Middleware Overhead**: ~0.004ms avg
- **Registry Lookup Latency**: ~0.001ms avg
