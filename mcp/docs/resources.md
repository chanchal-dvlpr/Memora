# MCP Resource Design and Execution Framework

This document details the architecture, execution pipeline, and validation rules governing Model Context Protocol (MCP) resources.

## Core Architectural Layout

The Memora MCP Server uses a modular, decoupled framework to manage resource lifecycles:

```
    [ Client resources/read Request ]
                   │
                   ▼
         [ ResourceDispatcher ]
                   │
                   ▼ (Onion Middleware Stack)
          [ ResourceExecutor ]
         ┌─────────┴─────────┐
         ▼                   ▼
[ ResourceRegistry ]   [ ResourceValidator ] (validateUri)
         │
         ▼
  [ Resource Handler ] ──> [ CLI ExecutionContext ]
         │
         ▼ (Clean Architecture)
  [ Application Services ] (ProjectApplicationService / ContextApplicationService / KnowledgeApplicationService)
         │
         ▼
  [ Domain / Persistence ]
```

---

## URI Model & Query Parameters

Resource identifiers conform to RFC 3986 URI format patterns. 

The server registers 5 primary canonical base URIs:
- `memora://project` (Exposes metadata and stats of the project)
- `memora://architecture` (Exposes design patterns and architecture documentation)
- `memora://knowledge` (Exposes semantic knowledge graphs and code symbols)
- `memora://tasks` (Exposes active/completed tasks parsed from context)
- `memora://decisions` (Exposes Architecture Decision Records (ADRs) parsed from context)

### Query Parameters & Content Negotiation
The Resource Registry performs lookup matching using the **base URI** (stripping query parameters and hash fragments). This enables passing dynamic client filtering options:
1. **URI Query Filter**: E.g., `memora://knowledge?query=Project` allows semantic symbol/text queries.
2. **Format Negotiation**: E.g., `memora://architecture?mimeType=application/json` (or via execution params) returns structured JSON representation, whereas `memora://architecture` returns the default `text/markdown` representation.

---

## Validation & Output Rules

1. **Input URI Validation**:
   - URIs must be non-empty and comply with the `memora://` scheme.
   - Scheme format must begin with a letter, followed by letters, digits, `+`, `-`, or `.`.
2. **Output Structure Validation**:
   - The result returned by handlers must resolve to an array of `ResourceContents`.
   - Each item in the array must carry a `uri` matching the canonical registered resource URI (e.g., `memora://project` or `memora://knowledge`).
   - At least one of `text` (string) or `blob` (base64 string) must be populated.
   - If `mimeType` is `application/json`, the validator parses and verifies the JSON string syntax.

---

## Onion Middleware Chain

Each resource read request executes through the following pipeline:
1. **Auditing Middleware**: Audits read execution.
2. **Authorization Middleware**: Verifies permission annotations.
3. **Logging Middleware**: Logs read requests.
4. **Timing Middleware**: Captures performance latency.
5. **Validation Middleware**: Performs input and output validations.
