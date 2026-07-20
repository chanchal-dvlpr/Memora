# Tool Execution Lifecycle & Middleware Pipeline

This document explains the execution contexts, middleware pipeline layers, and standard tool exceptions.

## Tool Execution Context

Every tool invocation initiates a decoupled `ToolExecutionContext` payload carrying:
- `requestId`: Globally unique random sequence identifying the request.
- `sessionId`: Session UUID from transport connections.
- `protocolVersion`: Negotatied MCP protocol string.
- `timestamp`: Epoch execution start time.
- `logger`: Structured logging engine instance.
- `params`: Arguments map parsed from transport message.
- `metadata`: Generic thread-safe Map to propagate transactional properties.

## Middleware Pipeline Execution

The server implements a Koa-style onion middleware pipeline executing layers sequentially in deterministic order:

1. **Auditing Middleware**: Intercepts requests, writes audit tracking records.
2. **Authorization Middleware**: Plugs validation rules or checks permission blockages in context metadata.
3. **Logging Middleware**: Logs initialization, results, and runtime crashes.
4. **Timing Middleware**: Captures performance latency benchmarks.
5. **Validation Middleware**: Executes input validations before handlers, and output validations afterwards.

## Standard Tool Error Mappings

Crashes are parsed and mapped to the standard error hierarchy:
- `ToolValidationError`: Parameter mismatch (JSON-RPC code `-32602`).
- `ToolNotFoundError`: Method not found (JSON-RPC code `-32601`).
- `ToolExecutionError`: Handler execution error (JSON-RPC code `-32603`).
- `ToolOutputValidationError`: Malformed output (JSON-RPC code `-32603`).
- `ToolRegistrationError`: Duplicate definition registration (JSON-RPC code `-32603`).
