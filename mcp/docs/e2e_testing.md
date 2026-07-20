# End-to-End Test Suite Documentation

This document summarizes the end-to-end integration test suites for the Memora MCP Server.

## 1. Test Suite Overview

The integration test suite consists of 34 test files covering 100% of major subsystems:

| Test Suite File | Subsystem Target | Key Assertions |
|---|---|---|
| `tests/e2e-mcp-protocol.test.ts` | Protocol layer | Discovery listings, error translation |
| `tests/e2e-tools-pipeline.test.ts` | Tools pipeline | All built-in tools executed through middleware |
| `tests/e2e-resources-pipeline.test.ts` | Resources pipeline | All registered resources read |
| `tests/e2e-prompts-pipeline.test.ts` | Prompts pipeline | All prompts generated |
| `tests/e2e-security-session.test.ts` | Security & Session | Token auth, authorization, session touch & reuse |
| `tests/integration-transport-all.test.ts` | Multi-transport | STDIO, HTTP, WebSocket initialization & close |
| `tests/integration-lifecycle-full.test.ts` | Lifecycle | Startup, initialization, health, shutdown, restart |
| `tests/integration-reliability-workload.test.ts` | Reliability | Concurrency bounds, telemetry metrics, health |
| `tests/integration-fault-injection.test.ts` | Fault injection | Queue overflow, timeout, bad auth, expired sessions |
| `tests/integration-stability.test.ts` | Stability | 100 repeated dispatch cycles & memory leak checks |
| `tests/integration-regression.test.ts` | Regression | Pipeline ordering & security policy invariants |

## 2. Test Execution & Quality Gate

- Total Test Suites: **34 / 34 passing**
- Total Tests: **263 / 263 passing**
- Linting & Typecheck: Clean (0 errors)
