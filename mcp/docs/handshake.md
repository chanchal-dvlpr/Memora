# Connection Handshake & Capability Negotiation

This document outlines the standard Model Context Protocol (MCP) handshake sequence.

## Handshake Flow

```
Client                                      Server (Memora)
  │                                               │
  │  1. initialize request                        │
  ├──────────────────────────────────────────────►│ (State: INITIALIZING)
  │                                               │
  │  2. initialize response (capabilities, info)  │
  │◄──────────────────────────────────────────────┤
  │                                               │
  │  3. initialized notification                  │
  ├──────────────────────────────────────────────►│ (State: INITIALIZED)
  │                                               │
  │            (Idle Session State)               │
```

---

## Capability Negotiation

The `initialize` request contains the client's supported features. In return, the server responds with its supported features. 

For the current phase, the server explicitly negotiates and returns `false` for all capabilities:
- `tools`: `false`
- `resources`: `false`
- `prompts`: `false`
- `logging`: `false`

These capabilities will be enabled in future implementation phases by registering handlers on the `MessageRouter` without modifying the core state machine.
