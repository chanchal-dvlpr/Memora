# Server Startup & Performance Metrics

This document details the startup workflow.

## Startup Sequence

1. **Instantiation**: Constructor sets up the logger, registries, and transition managers.
2. **Initialize (`initialize()`)**:
   - Runs configuration validations.
   - Clears registries.
   - Measures registry build times.
3. **Start (`start()`)**:
   - Starts and connects transport layers.
   - Connects JSON-RPC server channel.
   - Logs performance metrics and readiness details.

---

## Startup Performance Metrics

Measurements are captured using Node's `performance.now()` API and output to logs:
- `configLoadMs`: Configuration parsing time.
- `registryInitMs`: Registry allocation time.
- `transportInitMs`: Transport start and link duration.
- `totalStartupMs`: End-to-end initialization duration.
