# Health Audit Architecture

This document describes the `HealthManager` subsystem under load.

## 1. Subsystem Health Evaluation

`HealthManager` aggregates component statuses across four core areas:

- **`server`**: Core runtime state and lifecycle.
- **`registry`**: Registry availability (tools, resources, prompts).
- **`session`**: Active session store and cleanup operations.
- **`security`**: Authentication and authorization services.

Overall status is calculated as:
- `healthy`: All components are healthy.
- `degraded`: One or more components are marked degraded.
- `unhealthy`: One or more components are marked unhealthy.

## 2. Health Auditing Under Load

Under high load or shutdown states:
- `HealthManager` tracks heap and RSS memory bounds via `getMemorySnapshot()`.
- Session expiration and cleanup passes continuously report status updates to `session` component health.
- `generateHealthReport()` returns deep-frozen immutable snapshots preventing caller mutation.
