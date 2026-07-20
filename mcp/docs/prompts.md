# MCP Prompt Design and Execution Framework

This document outlines the architecture, execution pipeline, lifecycle, and validation rules governing Model Context Protocol (MCP) prompts in the Memora MCP Server.

## 1. Core Architectural Layout

The Prompt Framework is built as a generic, backend-independent layer designed to discover, validate, and execute prompt templates:

```
    [ Client prompts/get Request ]
                  │
                  ▼
         [ PromptDispatcher ]
                  │
                  ▼ (Onion Middleware Stack)
          [ PromptExecutor ]
         ┌────────┴────────┐
         ▼                 ▼
 [ PromptRegistry ]  [ PromptValidator ] (validateInput)
         │
         ▼
  [ Prompt Handler ]
         │
         ▼
 [ PromptValidator ] (validateOutput)
         │
         ▼
    [ PromptInvocationResult ]
```

---

## 2. Prompt Execution Lifecycle

Each request to retrieve or invoke a prompt undergoes a structured execution lifecycle:

1. **Client Request**: The client requests a prompt list (`prompts/list`) or gets a specific prompt execution (`prompts/get`) with string-based argument params.
2. **Registry Lookup**: `PromptDispatcher` retrieves the registered `PromptDefinition` from the `PromptRegistry`. If the prompt is missing, a `PromptNotFoundError` (mapping to JSON-RPC method not found `-32601`) is immediately thrown.
3. **Middleware Pipeline**: Execution is passed through the `PromptMiddlewarePipeline` containing:
   - **Auditing**: Log request intention before execution.
   - **Authorization**: Verify permissions to execute the prompt.
   - **Logging**: Captures debug traces and execution states.
   - **Timing**: Measures latency.
   - **Validation**: Enforces strict input validation and output schema validation.
4. **Invocation**: `PromptExecutor` runs the prompt handler callback.
5. **Response Serialization**: The result is serialized and returned to the client in standard MCP format.

---

## 3. Advanced Input and Output Validation

### Advanced Input Validation Rules
- **Default Values**: If a parameter is omitted, default values defined in `defaultValue` are automatically injected.
- **Type Conversions**: Input strings are parsed into their target types (`NUMBER`, `BOOLEAN`, `ARRAY`, or `OBJECT`) using parsing routines.
- **Constraints**:
  - `enum`: Verifies match against defined sets.
  - `pattern`: Validates string structure via regular expressions.
  - `minimum` & `maximum`: Enforces numeric boundaries.
  - `minLength` & `maxLength`: Enforces string lengths.
  - `items`: Validates types of array elements.
  - `properties`: Recursively validates nested object schemas.
- Unknown arguments are strictly blocked.

### Output Validation Rules
- Reject empty responses.
- Enforce that a `system` message must not appear after a non-system message.
- Verify JSON-serializability and block circular references.
