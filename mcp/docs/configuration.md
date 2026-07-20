# Configuration Parameters & Validation

The server configuration loader reads variables from the environment and maps defaults.

## Environment Variables

| Variable | Description | Default | Validation |
| :--- | :--- | :--- | :--- |
| `MEMORA_MCP_SERVER_NAME` | Server registration identifier | `memora-mcp-server` | String, Required |
| `MEMORA_MCP_SERVER_VERSION` | Version code | `1.0.0` | String, Required |
| `MEMORA_MCP_SERVER_LOG_LEVEL` | Logging threshold | `info` | trace, debug, info, warn, error, fatal |
| `MEMORA_MCP_SERVER_PORT` | Listen port | `8081` | 1 - 65535 |
| `MEMORA_MCP_SERVER_TIMEOUT` | Request timeout | `10000` | Positive integer |

---

## Validation Stage

During initialization, `MemoraMcpServer` performs strict type and boundary validations. Invalid values trigger a `ConfigurationValidationError` and halt bootstrapping immediately.
