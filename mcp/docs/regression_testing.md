# Regression Test Strategy & Invariants

This document details regression testing invariants and assertion strategies for the Memora MCP Server.

## 1. Regression Invariants

Regression tests (`tests/integration-regression.test.ts`) protect against breaking changes in core invariants:

- **Pipeline Middleware Order**: Guaranteed sequence `Authentication -> Authorization -> Session -> Audit -> Subsystem Middleware -> Handler`.
- **Security Policy Evaluation**: Guaranteed default access rules and role permission evaluations.
- **Session Touch & Store Contracts**: Guaranteed session lookup, automatic session creation, touch updates, and context store access.
- **Dispatcher Handling**: Guaranteed error translation from domain exceptions to clean JSON-RPC error codes.

## 2. Assertion Strategy

- Strict equality assertions on lifecycle state transitions.
- Immutable data structure checks (`Object.isFrozen`) on configuration and reports.
- Comprehensive coverage across all built-in tools, resources, and prompts.
