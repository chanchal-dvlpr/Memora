# Memora MCP Server — Operational Runbook

Operational procedures for running, monitoring, troubleshooting, backing up, and restoring the Memora MCP Server.

## 1. Startup & Shutdown Operations

### Node.js Process Execution
```bash
# Start server with production environment defaults
NODE_ENV=production MEMORA_MCP_SERVER_PORT=8080 node dist/index.js
```

### Docker Container Operations
```bash
# Start production container
docker-compose up -d memora-mcp-prod

# Graceful shutdown (sends SIGTERM, drains pending requests for 10s)
docker stop --time 10 memora-mcp-server-prod
```

---

## 2. Health Verification & Monitoring

### Probes & Diagnostic Status
- **Liveness & Readiness Endpoint**: `http://localhost:8080/health`
- **MCP System Status Tool**: Invoke `status` tool via JSON-RPC.
- **MCP Doctor Tool**: Invoke `doctor` tool via JSON-RPC.

---

## 3. Log Inspection & Metrics

### Log Formatting
Logs are output as structured JSON to `stderr` (`console.error`):
```json
{
  "timestamp": "2026-07-20T17:30:00.000Z",
  "level": "INFO",
  "component": "memora-mcp-server",
  "message": "Server initialized successfully."
}
```

### Inspecting Logs
```bash
# Docker logs
docker logs -f memora-mcp-server-prod
```

---

## 4. Troubleshooting & Common Scenarios

| Issue | Cause | Action |
| :--- | :--- | :--- |
| **Port Conflict** | Port 8080/8081 in use | Change `MEMORA_MCP_SERVER_PORT` |
| **High Backpressure Rejections** | Exceeded `maxConcurrentRequests` | Increase `MEMORA_MCP_MAX_CONCURRENT_REQUESTS` |
| **Auth Failures** | Token mismatch | Check `MEMORA_MCP_SECURITY_ENABLED` & auth headers |

---

## 5. Configuration Backup & Rollback

### Backup
```bash
cp .memorarc .memorarc.bak
```

### Rollback Procedure
```bash
# Revert container image
docker stop memora-mcp-server-prod
docker run -d --name memora-mcp-server-prod -p 8080:8080 memora-mcp:previous-stable
```
