# Transport Decoupling & Abstraction

To support SOLID design principles, the Memora MCP Server decouples the communication layer from the core server logic using the `McpTransport` interface.

## Transport Types

- **StdioTransport**: Standard input/output transport for host-process communication (fully functional).
- **HttpTransport**: Placeholder for future REST SSE integrations.
- **WebSocketTransport**: Placeholder for future WebSocket endpoints.

---

## Extension Guidelines

To add a new transport layer:
1. Create a class implementing the `McpTransport` interface defined in [types.ts](file:///Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of/mcp/src/transport/types.ts).
2. Wire it into the `MemoraMcpServer` constructor at startup. The server does not depend on concrete classes, making it fully pluggable.
