# Audit Framework

This document describes the audit logging system.

## 1. Objectives

The audit framework captures security and operational activities:
- Authentication success & failures.
- Authorization decisions.
- Tool, resource, and prompt execution trace details.

## 2. In-Memory Audit Logger

An `InMemoryAuditLogger` is supplied:
- Captures `AuditEntry` telemetry.
- Filters log entries by `level`, `category`, `actor`, and `outcome`.
- Supports clean clearing and immutable history reads.
