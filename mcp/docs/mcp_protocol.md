# Model Context Protocol Specification

This document summarizes the compliance mapping of the Memora MCP server to the official Model Context Protocol specifications.

## Protocol Mappings

- **Protocol Version**: Supported version is `2024-11-05`.
- **JSON-RPC Version**: Standard JSON-RPC 2.0.
- **Transports**: Native support for Stdio, HTTP SSE, and WebSocket.
- **Capabilities**:
  - `tools`: `false` (Phase 13.2 target)
  - `resources`: `false` (Phase 13.2 target)
  - `prompts`: `false` (Phase 13.2 target)
  - `logging`: `false`
  - `sampling`: `false`
  - `roots`: `false`
