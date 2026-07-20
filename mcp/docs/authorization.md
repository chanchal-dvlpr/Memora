# Authorization Framework

This document describes the permission and policy evaluators.

## 1. Components

- **`PermissionEvaluator`**: Resolves permissions from context scopes, validating actions against target URIs (with glob support).
- **`PolicyEvaluator`**: Registers and runs evaluation policies.
- **`AuthorizationManager`**: Orchestrates permission checking and policy execution.

## 2. Glob Match Scoping

Permission scopes specify target paths using standard glob strings (e.g. `memora://tools/*` or `*`). The evaluator automatically compiles target strings to regular expressions for checking.

## 3. Allow-All Baseline Policy

By default, the framework registers and defaults to the `allow-all` policy:
- Ensures out-of-the-box requests are allowed until custom restrictive policies are defined.
- Configurable globally or per-request.
