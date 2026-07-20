# Protocol Flow and Dispatcher Pipeline

This document explains the pipeline stages when a message is dispatched to the Memora MCP Server.

## JSON-RPC Processing Pipeline

```
          [ Raw Input ]
               │
               ▼
         MessageParser
               │
               ▼
        MessageValidator
               │
               ▼
      [State Validation Guards]
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
         [ Raw Output ]
```

## State Transitions
1. **NOT_INITIALIZED**: Reject all queries except `initialize` and `exit`.
2. **INITIALIZING**: Transitions after `initialize`. Reject all queries except `initialized` and `exit`.
3. **INITIALIZED**: Active session. Ready for standard protocol requests.
4. **SHUTDOWN_REQUESTED**: Reject all queries except `exit`.
5. **EXITED**: Process terminated.
