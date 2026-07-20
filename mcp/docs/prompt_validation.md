# MCP Prompt Validation Architecture

This document outlines the validation rules, type conversions, and output requirements enforced by `PromptValidator`.

---

## 1. Input Parameter Constraints

Input parameters are validated against a schema schema:

### Type Parsing
Prompt arguments sent over the protocol are represented as string key-values. The validator performs automatic type parsing before executing type checks:
- **`NUMBER`**: Parses string using `Number(val)`. Rejects non-numeric representations.
- **`BOOLEAN`**: Requires either `'true'` or `'false'` and converts to native booleans.
- **`ARRAY`**: Performs `JSON.parse(val)` to extract and validate element items.
- **`OBJECT`**: Performs `JSON.parse(val)` to validate nested JSON fields.

### Constraints Check
- **Default Values**: Omitted parameters with `defaultValue` are automatically populated.
- **Enum Validation**: Checks string values against defined whitelist sets.
- **Regex Pattern**: Verifies strings match defined regular expression structures.
- **Numeric Limits**: Enforces `minimum` and `maximum` boundaries.
- **String Lengths**: Verifies `minLength` and `maxLength` constraints.
- **Nested Structures**: Recursively validates child object keys and items in arrays.
- **Unknown Arguments**: Blocks unknown keys to prevent configuration leak.

---

## 2. Output Structural Invariants

The output returned by handler callbacks is validated to prevent protocol drift:

- **Non-Empty Response**: Resolves to a valid `PromptInvocationResult` containing a non-empty `messages` array.
- **Supported Roles**: Each message must have a role matching either `user`, `assistant`, or `system`.
- **Message Ordering**: All `system` messages must precede any `user` or `assistant` messages in the array. Out-of-order system prompts are rejected.
- **Content Invariants**:
  - `text`: Requires a string `text` property.
  - `image`: Requires base64 encoded `data` and a `mimeType`.
  - `resource`: Requires a nested resource object with a `uri` string.
- **JSON-serializability**: The output must be JSON-serializable, and circular references are blocked.
