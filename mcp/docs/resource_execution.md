# MCP Resource Execution Lifecycle

This document details the lifecycle, execution flow, and middleware components of the MCP Resource Framework.

## Execution Lifecycle

The lifecycle of an MCP resource read request proceeds as follows:

```
[Client Request] ──> [Dispatcher] ──> [Onion Middleware Chain] ──> [Executor] ──> [Resource Handler]
                                                                                        │
                                                                                        ▼ (Application Layer)
                                                                                  [CLI App Services]
                                                                                        │
                                                                                        ▼ (Error translation)
[Client Response] <── [Dispatcher] <── [Validation & Parsing] <── [Executor] <── [Standard ResourceError]
```

1. **Routing**: `ResourceDispatcher` matches the normalized URI against registered resources in the `ResourceRegistry` using the base URI (without parameters).
2. **Middleware Execution**: The dispatcher wraps the execution in a Koa/onion-style middleware stack.
3. **Execution**: `ResourceExecutor` executes the matched handler.
4. **Application Service Delegation**: Handlers invoke the corresponding Application Services (e.g. `ContextApplicationService`) via standard CLI `ExecutionContext`.
5. **Error Translation**: Any application or network errors are caught and translated:
   - "validation", "invalid", "required", "no project registered", "pass project id" -> `ResourceValidationError` (maps to JSON-RPC `-32602` Invalid Params)
   - "not found", "missing" -> `ResourceNotFoundError` (maps to JSON-RPC `404` equivalent)
   - Network connectivity drops, timeouts, other backend failures -> `ResourceExecutionError` (maps to JSON-RPC `-32603` Internal Error)
6. **Validation**: The pipeline runs URI and contents validators to confirm schema conformance.
7. **JSON-RPC Mapping**: Results are returned in standard protocol format, mapping any execution failures to JSON-RPC error codes.

---

## Middleware Pipeline Configuration

Middlewares execute in a deterministic order:

| Order | Middleware | Description |
| :--- | :--- | :--- |
| 1 | **Auditing** | Places audit logs before and after handler execution. |
| 2 | **Authorization** | Intercepts unauthorized resource requests (placeholder). |
| 3 | **Logging** | Captures debug logs and handles failure reporting. |
| 4 | **Timing** | Gauges request execution latency. |
| 5 | **Validation** | Enforces input URI correctness and validates output contents. |
