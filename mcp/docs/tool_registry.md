# Tool Registry and Discovery

This document details the registry system for registering, auditing, and listing tools.

## Immutable Metadata Schema

Each registered tool carries immutable metadata defined by `ToolMetadata`:
- `displayName`: Client-friendly presentation string.
- `description` / `longDescription`: Descriptions for runtime and developer portals.
- `version`: Semantic version of the tool.
- `author`: Owner of the tool definition.
- `categories`: Array of categorizations (`system`, `project`, `search`, `utility`).
- `tags`: Indexed tags for semantic query mappings.
- `examples`: Schema input/output examples.
- `annotations`: Custom extension attributes.
- `deprecationFlag` / `experimentalFlag`: Stability status indicators.
- `visibility`: Access permissions scope (`public`, `internal`).

## Deterministic Ordering on Discovery

The `tools/list` capabilities method returns registered tools sorted alphabetically by name to ensure stable and reproducible client layouts.
