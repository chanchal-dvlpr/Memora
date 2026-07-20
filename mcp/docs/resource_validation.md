# MCP Resource Validation

This document describes the validation policies, URI normalizations, and MIME type verification rules.

## URI Normalization Rules

To ensure resource identifier equivalence, URIs are normalized:
- **Scheme & Authority**: Lowercased (e.g. `MEMORA://Project` -> `memora://project`).
- **Query Parameters**: Sorted alphabetically (e.g. `?b=2&a=1` -> `?a=1&b=2`).
- **Fragments**: Empty fragments (`#`) are stripped.

---

## Output Validation Policies

All resource handlers must return valid contents matching these requirements:

### Circular Reference Prevention
- Payloads are checked for circular reference structures. Any cyclic dependencies trigger a `ResourceOutputValidationError`.

### MIME Type Validation
Supported MIME types:
- `text/plain`
- `text/markdown`
- `application/json` (automatically validated for valid JSON formatting)
- `application/yaml`
- `text/html`
- `application/octet-stream` (for binary/blob content)

### Payload Integrity
- Must contain at least one of `text` or `blob`.
- Each content item URI must match the requested resource URI.
