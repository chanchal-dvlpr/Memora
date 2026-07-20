# Integration Testing Architecture & Infrastructure

This document details the end-to-end integration test architecture for the Memora MCP Server.

## 1. Test Harness Infrastructure

Integration tests use `McpTestHarness` located in `tests/harness/mcp_test_client.ts`. The harness provisions an in-memory `MemoraMcpServer` instance with test configuration (`usePlaceholder: true`), providing isolated, deterministic execution without disk side effects.

### Key Capabilities
- **Server Setup/Teardown**: Automated `initialize()` and `stop()` lifecycle handling.
- **Typed Dispatch Helpers**:
  - `callTool(name, args, metadata)`: Dispatches tool calls through `ToolDispatcher`.
  - `readResource(uri, metadata)`: Dispatches resource read requests through `ResourceDispatcher`.
  - `getPrompt(name, args, metadata)`: Dispatches prompt invocations through `PromptDispatcher`.

## 2. Execution Pipeline & Subsystem Coverage

Every dispatch invocation executes the full server middleware pipeline:

```
JSON-RPC Request
      ↓
Authentication Middleware
      ↓
Authorization Policy
      ↓
Session Validation & Creation
      ↓
Session Touch Telemetry
      ↓
Audit Log Recording
      ↓
Custom Subsystem Middleware
      ↓
Handler Execution & Response Serialization
```

## 3. Test Suites Overview

- **Protocol Layer**: `tests/e2e-mcp-protocol.test.ts`
- **Tool Pipeline**: `tests/e2e-tools-pipeline.test.ts`
- **Resource Pipeline**: `tests/e2e-resources-pipeline.test.ts`
- **Prompt Pipeline**: `tests/e2e-prompts-pipeline.test.ts`
- **Security + Session**: `tests/e2e-security-session.test.ts`
- **Transport Subsystem**: `tests/integration-transport-all.test.ts`
- **Full Server Lifecycle**: `tests/integration-lifecycle-full.test.ts`
- **Reliability Workloads**: `tests/integration-reliability-workload.test.ts`
- **Controlled Fault Injection**: `tests/integration-fault-injection.test.ts`
- **Long-Running Stability**: `tests/integration-stability.test.ts`
- **Regression Invariants**: `tests/integration-regression.test.ts`
