# Reliability Testing & Fault Injection

This document describes the reliability test methodology, fault injection strategy, and stability validation for the Memora MCP Server.

## 1. Reliability Test Subsystems

- **Concurrency & Backpressure**: Validates active slot execution limits and queue backpressure strategies (`reject`, `queue`, `timeout`).
- **Timeout Protection**: `TimeoutManager.withTimeout` guarantees request cancellation if handler or middleware execution exceeds configured deadlines.
- **Graceful Draining**: `ShutdownManager` drains queued requests with `ShutdownError` and awaits active execution slots before running cleanup hooks.

## 2. Controlled Fault Injection Scenarios

Fault injection integration tests (`tests/integration-fault-injection.test.ts`) verify server resilience against controlled failures:
- **Queue Overflow**: Throws `QueueOverflowError` when queue slots exceed capacity.
- **Timeout Expiration**: Throws `RequestTimeoutError` when operations exceed deadlines.
- **Invalid Security Credentials**: Throws `InvalidCredentialError` on unauthorized tokens.
- **Expired Session Validation**: Throws `SessionExpiredError` when accessing expired sessions.

## 3. Stability & Leak Validation

- **Repeated Execution**: 100 continuous dispatch cycles across tools, resources, and prompts verifying context store cleanup.
- **Session Purging**: 100 repeated session creation and absolute timeout cycles verifying 100% removal of expired sessions and context keys.
