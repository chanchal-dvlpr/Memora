# Testing Strategy & Structure

This document outlines the testing strategy for the MCP Server Foundation.

## Test Suites

All tests reside under [tests/](file:///Users/chanchalkumar/Documents/Codex/2026-07-12/we-are-starting-the-implementation-of/mcp/tests):
1. **config.test.ts**: Verifies configuration environments and validation.
2. **logger.test.ts**: Verifies JSON format outputs, level filters, and stderr emissions.
3. **lifecycle.test.ts**: Asserts lifecycle managers, valid transitions, and restarts.
4. **transport.test.ts**: Verifies Stdio transport and HTTP/WS compile placeholders.
5. **registry.test.ts**: Verifies duplication prevention, registries lookups, and immutability.
6. **startup.test.ts**: Asserts boundary validations and initialization metrics.
7. **shutdown.test.ts**: Asserts isolated signal traps and exit status values.
8. **validation.test.ts**: Asserts diagnostic reports.
9. **server.test.ts**: Confirms mock transport connection.

## Execution
Run all tests using:
```bash
npm run test
```
