# MCP Resource Registry

This document describes the design, features, and interface specifications of the `ResourceRegistry`.

## Key Features

1. **Duplicate Registration Prevention**:
   - Attempts to register a resource with an already registered URI throw a `ResourceRegistrationError`.
2. **Alphabetical Sorting**:
   - `listResources()` returns registered definitions sorted alphabetically by their URIs.
3. **Immutability**:
   - Returns frozen list views (`Object.freeze`) and enforces resource metadata immutability using `deepFreeze`.
4. **Backward Compatibility**:
   - Bridges legacy test suites by translating non-array returns (like raw strings or structures) into array-wrapped `ResourceContents` payloads on the fly during execution.

---

## Metadata Attributes

The registry accepts `ResourceMetadata` configuration supporting:
- `displayName`: Client-facing label.
- `description`: Summary of resource contents.
- `longDescription`: In-depth details.
- `version`: SemVer string.
- `author`: Author identity.
- `category` & `categories`: Classification tags.
- `tags`: Resource keywords.
- `annotations`: Key-value configuration maps.
- `examples`: Usage payload snapshots.
- `mimeType`: Resource MIME type.
- `visibility`: Visibility boundary (`public` or `internal`).
- `experimentalFlag`: Experimental feature status.
- `deprecationFlag`: Deprecation alert.

---

## API Reference

- **`registerResource(resource: ResourceDefinition, metadata?: ResourceMetadata): void`**
  Registers a resource mapping its URI to a handler and metadata configuration.
- **`unregisterResource(uri: string): void`**
  Removes a registered resource definition. Throws `ResourceNotFoundError` if missing.
- **`getResource(uri: string): ResourceDefinition | undefined`**
  Retrieves a resource definition reference by URI.
- **`getMetadata(uri: string): ResourceMetadata | undefined`**
  Retrieves metadata properties by URI.
- **`listResources(): ReadonlyArray<ResourceDefinition>`**
  Returns all registered resources in alphabetical URI order.
- **`hasResource(uri: string): boolean`**
  Returns `true` if a resource is registered for the specified URI.
