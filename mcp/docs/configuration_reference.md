# Memora MCP Server — Configuration Reference

Complete reference of all environment variables, runtime parameters, and configuration options.

## Environment Variables

| Variable Name | Type | Default (Prod / Dev) | Description |
| :--- | :--- | :--- | :--- |
| `NODE_ENV` | `string` | `development` | Profile: `development`, `test`, `staging`, `production` |
| `MEMORA_MCP_SERVER_NAME` | `string` | `memora-mcp-server` | Descriptive server instance identifier |
| `MEMORA_MCP_SERVER_VERSION` | `string` | Read from `VERSION` | Semantic version override |
| `MEMORA_MCP_BUILD_METADATA` | `string` | `20260720+prod` | Build metadata string |
| `MEMORA_MCP_RELEASE_METADATA` | `string` | `release-1.0.0` | Release channel string |
| `MEMORA_MCP_SERVER_HOST` | `string` | `127.0.0.1` | Network binding interface address |
| `MEMORA_MCP_SERVER_PORT` | `number` | `8080` (prod) / `8081` (dev) | Network port |
| `MEMORA_MCP_SERVER_LOG_LEVEL` | `enum` | `info` (prod) / `debug` (dev) | `debug`, `info`, `warn`, `error` |
| `MEMORA_MCP_SECURITY_ENABLED` | `boolean` | `true` | Enforces authentication and authorization |
| `MEMORA_MCP_DEFAULT_AUTH_PROVIDER` | `string` | `mock` | Default authentication manager provider |
| `MEMORA_MCP_DEFAULT_AUTHZ_POLICY` | `string` | `allow-all` | Authorization policy model |
| `MEMORA_MCP_AUDIT_LOG_ENABLED` | `boolean` | `true` | Enables structured audit log generation |
| `MEMORA_MCP_REQUEST_TIMEOUT_MS` | `number` | `10000` (prod) / `30000` (dev) | Maximum allowed total request processing duration |
| `MEMORA_MCP_HANDLER_TIMEOUT_MS` | `number` | `10000` | Maximum execution time for individual handler |
| `MEMORA_MCP_MAX_CONCURRENT_REQUESTS` | `number` | `200` (prod) / `50` (dev) | Concurrency limit before backpressure triggers |
| `MEMORA_MCP_MAX_QUEUED_REQUESTS` | `number` | `100` | Max queued requests under backpressure |
| `MEMORA_MCP_MAX_PAYLOAD_SIZE_BYTES` | `number` | `10485760` (10MB) | Max incoming payload size |
| `MEMORA_MCP_SHUTDOWN_TIMEOUT_MS` | `number` | `10000` | Graceful shutdown request draining timeout |
