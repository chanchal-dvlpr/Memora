# Memora MCP Server — Deployment Guide

This guide covers deployment strategies, container topologies, environment profiles, and health monitoring for the Memora MCP Server.

## 1. Environment Profiles

The server configures runtime defaults based on `NODE_ENV`:

| Profile | `NODE_ENV` | Port | Log Level | Max Concurrency | Timeout |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Development** | `development` | 8081 | `debug` | 50 | 30,000ms |
| **Testing** | `test` / `testing` | 8082 | `error` | 10 | 5,000ms |
| **Staging** | `staging` | 8080 | `info` | 100 | 15,000ms |
| **Production** | `production` | 8080 | `info` | 200 | 10,000ms |

---

## 2. Docker & Kubernetes Deployment

### Container Security
- Non-root user execution (`USER node`).
- Read-only root filesystem compatible.
- Ephemeral container storage for session caches.

### Health Probes
- **Liveness Probe**: `GET /health` or process health status.
- **Readiness Probe**: Verifies transport initialization, tool registry loading, and health checks.

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 30
readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
```

---

## 3. Production Hardening Checklist

- Enable security checks (`MEMORA_MCP_SECURITY_ENABLED=true`).
- Set appropriate concurrency limits (`MEMORA_MCP_MAX_CONCURRENT_REQUESTS=200`).
- Enable structured audit logging (`MEMORA_MCP_AUDIT_LOG_ENABLED=true`).
- Configure graceful shutdown timeouts (`MEMORA_MCP_SHUTDOWN_TIMEOUT_MS=10000`).
