# MCP Prompt Execution Pipeline

This document details the execution context, middleware orchestration, and error mapping of the MCP Prompt execution framework.

---

## 1. Prompt Execution Context

The `PromptExecutionContext` object is passed into every prompt handler, providing structured telemetry, request metadata, and cancellation tokens:

- **`requestId`**: Unique UUID/string token generated per request.
- **`sessionId`**: Connection session ID.
- **`protocolVersion`**: MCP Protocol version (e.g. `'2024-11-05'`).
- **`timestamp`**: Time of invocation.
- **`logger`**: Active `StructuredLogger` instance.
- **`params`**: Raw input parameters passed.
- **`metadata`**: Extensible key-value metadata map.
- **`cancellationToken`**: Placeholder interface to support request cancellation:
  ```typescript
  export interface CancellationToken {
    readonly isCancellationRequested: boolean;
    onCancellationRequested(callback: () => void): void;
  }
  ```

---

## 2. Onion Middleware Architecture

The middleware pipeline wraps execution in a deterministic order:

| Order | Middleware | Description |
| :--- | :--- | :--- |
| 1 | **Auditing** | Generates audit logs before and after handler execution. |
| 2 | **Authorization** | Intercepts unauthorized prompt invocations (placeholder). |
| 3 | **Logging** | Captures debug logs and execution status. |
| 4 | **Timing** | Gauges latency metrics of the prompt invocation. |
| 5 | **Validation** | Enforces input argument correctness and output structure. |

---

## 3. Error Hierarchy Mapping

The execution pipeline translates internal failures into standard JSON-RPC error codes:

- **`PromptValidationError`**: Parameter constraints violations (maps to Invalid Params `-32602`).
- **`PromptNotFoundError`**: Prompt missing (maps to Method Not Found `-32601`).
- **`PromptExecutionError`**: Handler failures (maps to Internal Error `-32603`).
- **`PromptOutputValidationError`**: Malformed response layout (maps to Internal Error `-32603`).
- **`PromptRegistrationError`**: Registration duplicate conflicts (maps to Internal Error `-32603`).
