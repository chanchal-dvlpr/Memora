# Server Lifecycle Management

The Memora MCP Server uses a state machine to orchestrate bootstrap, start, stop, and restart procedures.

## Lifecycle States

```
                 [UNINITIALIZED]
                        │
                        ▼  initialize()
                  [INITIALIZED]
                        │
                        ▼  start() (Transitions via STARTING)
                    [STARTED]
                        │
                        ▼  stop() (Transitions via STOPPING)
                    [STOPPED]
```

### Invalid Transitions
- Calling `initialize()` while already initialized or running throws an error.
- Calling `start()` on an uninitialized or already started server throws an error.
- Calling `stop()` on a stopped or uninitialized server throws an error.

---

## Extension Guidelines

To add post-startup hook routines (such as telemetry registers or daemon check-ins), insert callbacks directly within the `start` transition logic of `LifecycleManager` inside [lifecycle.ts](file:///Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of/mcp/src/server/lifecycle.ts).
