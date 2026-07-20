# Memora Integrated Prompts Reference

This document catalogs the production MCP prompts registered in the Memora MCP Server.

---

## 1. Mappings to Memora Application Services

All prompts adhere to clean architecture design principles and delegate directly to application services:

| Prompt | Application Service | Description / Objective |
| :--- | :--- | :--- |
| **`generate-handoff`** | `ContextApplicationService` | Queries the workspace developer session context to formulate a structured handoff. |
| **`review-architecture`** | `ContextApplicationService` | Analyzes modules, dependencies, and design boundaries to review alignment. |
| **`summarize-project`** | `ProjectApplicationService` | Generates a project-level executive state summary. |
| **`explain-module`** | `KnowledgeApplicationService` | Queries semantic knowledge indexing matching specific modules/folders. |
| **`review-tasks`** | `ContextApplicationService` | Audits session active and completed tasks checklist state. |

---

## 2. Prompt Details and Arguments Schema

### `generate-handoff`
- **Arguments**:
  - `projectId` (required, string): Unique project identifier.
- **Expected Output**: Structured markdown containing Project Summary, Active Tasks, Architecture Overview, Pending Work, Important Decisions, and Next Steps.

### `review-architecture`
- **Arguments**:
  - `projectId` (required, string): Unique project identifier.
- **Expected Output**: Markdown report listing architecture overview, modules, subsystem dependencies, design principles, and review focus items.

### `summarize-project`
- **Arguments**:
  - `projectId` (required, string): Unique project identifier.
- **Expected Output**: Markdown overview containing repository layout, technology stack, current development state, and recommended entry points.

### `explain-module`
- **Arguments**:
  - `projectId` (required, string): Unique project identifier.
  - `moduleName` (required, string): Target module directory or component name.
- **Expected Output**: Purpose of module, subsystem responsibilities, files list matching module name, and related code symbols.

### `review-tasks`
- **Arguments**:
  - `projectId` (required, string): Unique project identifier.
- **Expected Output**: Checklist representing all parsed active tasks (`- [ ]`) and completed tasks (`- [x]`) along with development priorities and recommendations.
