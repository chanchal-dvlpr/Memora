# Extension Registries

The Memora MCP Server maintains decoupled registries for tools, resources, and prompts.

## Core Registries

- **ToolRegistry**: Manages custom tools definitions and validation schemas.
- **ResourceRegistry**: Manages resources and files content loaders.
- **PromptRegistry**: Manages pre-baked prompt templates.

---

## Capabilities & Registration APIs

All registries enforce the following rules:
- Duplicate registration throws an immediate runtime error.
- Public retrieval returns frozen immutable list objects.
- Registry initialization occurs automatically during the server initialization phase.
