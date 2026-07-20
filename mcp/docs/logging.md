# Structured Logging Architecture

The Memora MCP Server logging framework is structured around JSON formatted standard error logs.

## Core Logger Implementation

The `StructuredLogger` class supports structured metadata mapping, error exception parsing with complete stack traces, and severity thresholds:
- `TRACE`
- `DEBUG`
- `INFO`
- `WARN`
- `ERROR`
- `FATAL`

### Example Output

```json
{
  "timestamp": "2026-07-19T17:42:00.000Z",
  "level": "INFO",
  "component": "memora-mcp-server",
  "message": "Transport connected and server started",
  "metadata": {
    "transport": "stdio",
    "port": 8081,
    "host": "127.0.0.1"
  }
}
```

---

## Extension Guidelines

### Redirecting Logs
To redirect logs to an external transport, modify the private `write` method in `StructuredLogger` located inside [logger.ts](file:///Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of/mcp/src/logging/logger.ts).

### Custom Level Actions
Add third-party alert dispatchers inside the `fatal` or `error` logging helpers to hook up operational monitoring layers.
