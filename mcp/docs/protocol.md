# Model Context Protocol (MCP) Message Flow

This document outlines the core protocol flow of the Memora MCP Server.

## Message Lifecycle

```
    Transport Input (raw string)
               │
               ▼
         MessageParser
               │
               ▼
        MessageValidator
               │
               ▼
      [State Validation Guards]
      (Reject commands before initialize)
               │
         ┌─────┴─────┐
         │           │
         ▼           ▼
      Request   Notification (no response)
         │           │
         ▼           ▼
   MessageRouter MessageRouter
         │           │
         ▼           ▼
    MethodHandler MethodHandler
         │
         ▼
MessageSerializer (JSON response string)
               │
               ▼
        Transport Output
```

## Protocol Operations
- **State Validation Guards**: Before transitioning to the `INITIALIZED` state, non-handshake methods are intercepted and rejected early.
- **Router Mapping**: Handlers are registered dynamically during startup and route incoming parsed JSON-RPC requests.
