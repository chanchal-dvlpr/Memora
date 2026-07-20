# Permission Model

This document outlines the permission namespaces, roles, and matching rules for the Memora MCP server.

## Namespaces

Required permissions are mapped to specific actions and namespace patterns:

- **Tools**: `memora://tools/<tool-name>`
- **Resources**: `memora://resources/<resource-name>`
- **Prompts**: `memora://prompts/<prompt-name>`

## Matching Rules & Globbing

The `PermissionEvaluator` supports wildcard characters (`*`) for glob pattern matching. 

### Examples:

- `*`: Matches any namespace target.
- `memora://tools/*`: Matches all tool execution requests.
- `memora://resources/*`: Matches all resource read requests.
- `memora://prompts/*`: Matches all prompt execution requests.

## Pluggable Policies

The framework supports the following built-in policies:

1. **`allow-all`**: Baseline policy that permits all actions. Useful for development or trusted client setups.
2. **`deny-all`**: Baseline policy that denies all actions.
3. **`permission-based`**: Enforces strict permission validation against the credentials supplied by the principal.
4. **Custom Policies**: Additional policies can be registered dynamically via the `PolicyEvaluator`.
