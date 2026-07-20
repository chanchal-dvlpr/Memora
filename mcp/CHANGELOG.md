# Changelog

All notable changes to the Memora MCP Server will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-20

### Added
- Complete Model Context Protocol (MCP) server implementation supporting MCP SDK v0.6.0+.
- Full support for standard MCP primitives: Tools, Resources, and Prompts.
- Integrated Security Subsystem featuring Authentication Manager, RBAC/ABAC Authorization Manager, and Structured Audit Logger.
- In-memory Session & Context Management with configurable expiration policies (sliding, absolute, manual, none) and orphan session cleanup.
- Performance & Reliability framework with backpressure strategies (reject, queue, timeout), active request draining, and health/metrics observability.
- Multi-environment release configuration profiles (`development`, `test`, `staging`, `production`).
- Docker containerization support with non-root security context and multi-stage builds.
- Complete documentation suite and integration test coverage.
