# Memora MCP Production Tools Integration

This document describes the concrete integration of Memora Application Services with the Model Context Protocol (MCP) server tools layer.

## Architectural Boundaries

We strictly enforce Clean Architecture boundaries. Under no circumstances does the MCP layer directly access the database, persistence repositories, or lower-level domain layers. Instead, execution flows as follows:

```
[MCP Client] -> [Memora MCP Tool Handler] -> [CLI Application Services] -> [HTTP Clients] -> [Spring Boot Backend]
```

This guarantees complete separation of concerns and reuse of validated business rules encapsulated within the CLI Application Services.

---

## Tool Reference

### 1. `status`
- **Purpose**: Inspect health state of backend connection and services.
- **Input Schema**: `properties: {}, required: []`
- **Application Service**: Calls `DiagnosticsApplicationService.generateReport()`
- **Output Properties**:
  - `backendConnectivity`: `"UP" | "DOWN"`
  - `projectResolution`: `"AVAILABLE" | "UNAVAILABLE"`
  - `scannerAvailability`: `"AVAILABLE" | "UNAVAILABLE"`
  - `knowledgeEngineAvailability`: `"AVAILABLE" | "UNAVAILABLE"`
  - `mcpServerStatus`: `"RUNNING"`
  - `runtimeVersion`: Node.js process version string

### 2. `doctor`
- **Purpose**: Perform diagnostics validation checks on configuration and dependencies.
- **Input Schema**: `properties: {}, required: []`
- **Application Service**: Calls `DiagnosticsApplicationService.generateReport()`
- **Output Properties**:
  - `configurationValidation`: `"PASSED" | "FAILED"`
  - `dependencyChecks`: `"PASSED" | "FAILED"`
  - `filesystemChecks`: `"PASSED" | "FAILED"`
  - `backendHealth`: `"UP" | "DOWN"`
  - `transportHealth`: `"UP"`
  - `recommendations`: String array of actionable solutions for failure nodes.

### 3. `projects`
- **Purpose**: Retrieve deterministically ordered list of registered workspace projects.
- **Input Schema**: `properties: {}, required: []`
- **Application Service**: Calls `ProjectApplicationService.listProjects()`
- **Output Properties**:
  - `projects`: List of objects, each containing:
    - `id`: Project UUID
    - `name`: Human-readable name
    - `rootPath`: Project workspace absolute filesystem path
    - `createdAt`: ISO 8601 creation timestamp

### 4. `search`
- **Purpose**: Execute semantic search on registered workspace knowledge base documents.
- **Input Schema**:
  - `projectId` (string, required): Target project UUID
  - `query` (string, required): Semantic search keywords
  - `limit` (number, optional): Max results to retrieve (default: 10)
- **Application Service**: Calls `KnowledgeApplicationService.searchKnowledge()`
- **Output Properties**:
  - `documents`: List of matched knowledge artifacts:
    - `title`: Document title
    - `type`: Category indicator (`"knowledge_item"`)
    - `relevance`: Match confidence score `[0.0 - 1.0]`
    - `snippet`: Content fragment text
    - `source`: Backend source identifier

### 5. `handoff`
- **Purpose**: Generate dynamic handoff snapshot containing architecture, active tasks, decisions, and modules.
- **Input Schema**:
  - `projectId` (string, required): Target project UUID
- **Application Service**: Calls `ContextApplicationService.generateContext()`
- **Output Properties**:
  - `architecture`: Compiled architecture overview markdown text
  - `activeTasks`: Open task tracking details
  - `decisions`: Architectural log entries
  - `modules`: Code modules summary
  - `importantFiles`: Critical files watchlist
  - `projectSummary`: Brief project information summary

---

## Error Mapping & Safety

All tool execution paths are wrapped in translation logic converting internal errors or network timeouts into structured exceptions:
- **`ToolValidationError`** (Code `-32602`): Invalid parameter formats.
- **`ToolNotFoundError`** (Code `-32601`): Missing records or unknown tool names.
- **`ToolExecutionError`** (Code `-32603`): Unreachable backend or transport errors.

Stack traces are filtered from responses to prevent leaking internal directory structures or system secrets to client LLMs.
