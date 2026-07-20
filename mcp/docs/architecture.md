# MCP Server Runtime Architecture

This document describes the architectural layout and dependency direction of the Memora Model Context Protocol (MCP) Server.

## Clean Architecture Boundaries

The MCP Server implements a modular structure separating transport, registry, lifecycle, and configuration concerns.

```
       ┌────────────────────────┐
       │     Configuration      │
       └───────────┬────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│         MemoraMcpServer              │◄───────┐
└──────────────────┬───────────────────┘        │
                   │                            │
                   ▼                            │
┌──────────────────────────────────────┐        │
│          LifecycleManager            │        │
└──────────────────────────────────────┘        │
                   │                            │
                   ▼                            │
┌──────────────────────────────────────┐        │
│            McpTransport              │        │
│ (Stdio, Http, WebSocket Placeholders)│        │
└──────────────────────────────────────┘        │
                   │                            │
                   ▼                            │
┌──────────────────────────────────────┐        │
│             Registries               ├────────┘
│ (ToolRegistry, ResourceRegistry, etc.)│
└──────────────────────────────────────┘
```

## Dependency Rules & Principles
- All components depend strictly on interfaces/abstractions rather than concrete bindings.
- Registries and loggers are initialized at server construction, avoiding global states.
- No business logic or protocol logic exists within this foundational framework layer.
