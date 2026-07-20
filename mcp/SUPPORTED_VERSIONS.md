# Supported Versions & Compatibility Matrix

## Node.js & Runtime Compatibility

| Server Version | Node.js Version | MCP SDK Version | OS Support | Support Status |
| :--- | :--- | :--- | :--- | :--- |
| **1.0.x** | `>= 18.0.0` (LTS 18, 20, 22) | `^0.6.0` | macOS, Linux, Windows | **Active Support** |

## Environment Profiles

| Environment Profile | Key Characteristics | Default Port | Log Level | Timeout |
| :--- | :--- | :--- | :--- | :--- |
| `development` | Verbose logging, hot reloading | `8081` | `debug` | 30s |
| `testing` / `test` | Minimal logging, strict timeouts | `8082` | `error` | 5s |
| `staging` | Staging infrastructure integration | `8080` | `info` | 15s |
| `production` | High concurrency, production logging | `8080` | `info` | 10s |
