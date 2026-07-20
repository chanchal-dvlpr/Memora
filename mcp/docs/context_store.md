# Context Store & Context Propagation

This document describes the design of `ContextStore`, `SessionContextBuilder`, and `SessionContextResolver`.

## 1. Context Propagation Fields

`SessionContext` supports propagation of multi-layered execution and request context:

- **`requestId`**: Unique identifier for the immediate protocol request.
- **`correlationId`**: Distributed tracking identifier spanning multiple calls.
- **`requestMetadata`**: Protocol or HTTP request metadata headers.
- **`protocolMetadata`**: MCP protocol negotiation metadata.
- **`conversationMetadata`**: Dialogue state and chat history indicators.
- **`clientMetadata`**: Arbitrary client key-value parameters.
- **`clientInformation`**: Version, name, and environment details of the client.
- **`runtimeMetadata`**: Host environment configuration parameters.
- **`executionMetadata`**: Subsystem execution flags and parameters.
- **`securityContext`**: Attached `SecurityContext` containing principal identity and permissions.

## 2. Session Context Builder & Resolver

- **`SessionContextBuilder`**: Fluent builder API to construct immutable, deep-frozen `SessionContext` instances.
- **`SessionContextResolver`**: Provides static accessor methods (`resolveCorrelationId`, `resolveRequestMetadata`, `resolveClientInformation`, etc.) to safely unpack fields from a `Session`.
