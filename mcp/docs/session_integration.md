# Session Integration Architecture

This document describes how the Session & Context framework integrates into the Memora MCP Server dispatchers while strictly preserving Clean Architecture boundaries.

## 1. Clean Architecture Alignment

Session management operates exclusively at the protocol/middleware layer:

```
Prompts / Tools / Resources (MCP Protocol Layer)
        ↓
Session & Security Middlewares
        ↓
Application Services (CLI Core Domain)
        ↓
Domain Models
        ↓
Persistence
```

Session management never bypasses or pollutes the underlying Tool, Resource, or Prompt abstractions.

## 2. Dispatcher Integration

- **`ToolDispatcher`**: Chains session middleware to validate session, auto-create missing session, touch activity, and propagate `SessionContext` into `ToolExecutionContext`.
- **`ResourceDispatcher`**: Chains session middleware into resource reading pipeline. Resources remain strictly read-only.
- **`PromptDispatcher`**: Chains session middleware into prompt retrieval pipeline.

## 3. Session Propagation

`SessionContext` is attached to execution contexts (`ToolExecutionContext`, `ResourceExecutionContext`, `PromptExecutionContext`), propagating:

- `sessionId`
- `correlationId`
- `requestMetadata`
- `securityContext`
- `clientMetadata`
