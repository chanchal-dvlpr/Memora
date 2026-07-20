# Memora MCP Production Resources

This document outlines the 5 production resources exposed by the Memora Model Context Protocol (MCP) server, including their metadata, parameters, formatting options, and schemas.

---

## 1. Project Resource (`memora://project`)
Exposes read-only metadata and stats of the project registered in the current directory or requested explicitly.

- **Display Name**: `Memora Project Details`
- **Category**: `project`
- **MIME Type**: `application/json`
- **Tags**: `project-info`, `settings`
- **Annotations**: `{ stability: 'stable' }`
- **Example URI**: `memora://project`
- **JSON Schema**:
  ```json
  {
    "projectId": "string (UUID or local id)",
    "projectName": "string",
    "rootPath": "string (absolute file path)",
    "languages": ["string (e.g. TypeScript, JavaScript)"],
    "createdDate": "string (ISO timestamp)",
    "updatedDate": "string (ISO timestamp)",
    "metadata": {
      "source": "memora-mcp"
    }
  }
  ```

---

## 2. Architecture Resource (`memora://architecture`)
Exposes the system architecture overview, module structure, design boundaries, and subsystem details.

- **Display Name**: `Memora Architecture Design`
- **Category**: `project`
- **Default MIME Type**: `text/markdown`
- **Tags**: `architecture`, `design-patterns`
- **Annotations**: `{ stability: 'stable' }`
- **Format Negotiation**:
  - `mimeType` param `text/markdown` (default): Returns formatted Markdown under `# Architecture Details`.
  - `mimeType` param `application/json`: Returns structured JSON containing specific architectural fields:
    ```json
    {
      "architectureSummary": "string",
      "moduleOverview": "string",
      "subsystemRelationships": "string",
      "technologyStack": ["string"],
      "designPrinciples": ["string"]
    }
    ```

---

## 3. Knowledge Resource (`memora://knowledge`)
Provides semantic search queries and symbol references from the indexed knowledge base.

- **Display Name**: `Memora Knowledge Base Search`
- **Category**: `search`
- **MIME Type**: `application/json`
- **Tags**: `knowledge-graph`, `semantic-search`
- **Annotations**: `{ stability: 'stable' }`
- **URI Parameters**:
  - `query` / `filter`: The text or symbol name to search for (default: `architecture`).
  - `limit`: Max documents to return (default: `10`).
- **JSON Schema**:
  ```json
  {
    "indexedKnowledge": [
      { "id": "string", "title": "string", "path": "string" }
    ],
    "symbols": [
      { "name": "string", "path": "string" }
    ],
    "relationships": [
      { "title": "string", "path": "string" }
    ],
    "summaries": [
      { "title": "string", "summary": "string" }
    ]
  }
  ```

---

## 4. Tasks Resource (`memora://tasks`)
Exposes active and completed tasks parsed dynamically from the context.

- **Display Name**: `Memora Workspace Tasks`
- **Category**: `project`
- **MIME Type**: `application/json`
- **Tags**: `tasks-management`, `status`
- **Annotations**: `{ stability: 'stable' }`
- **JSON Schema**:
  ```json
  {
    "activeTasks": [
      {
        "id": "string",
        "title": "string",
        "priority": "HIGH | MEDIUM | LOW",
        "status": "ACTIVE",
        "ownership": "string"
      }
    ],
    "completedTasks": [
      {
        "id": "string",
        "title": "string",
        "priority": "MEDIUM",
        "status": "COMPLETED",
        "ownership": "string"
      }
    ]
  }
  ```

---

## 5. Decisions Resource (`memora://decisions`)
Exposes Architecture Decision Records (ADRs) and design rationale parsed dynamically from context.

- **Display Name**: `Memora Architecture Decisions (ADRs)`
- **Category**: `project`
- **MIME Type**: `application/json`
- **Tags**: `adr`, `design-decisions`
- **Annotations**: `{ stability: 'stable' }`
- **JSON Schema**:
  ```json
  {
    "adrs": [
      {
        "title": "string",
        "rationale": "string",
        "timestamp": "string (ISO timestamp)",
        "reference": "string"
      }
    ],
    "importantDecisions": [
      {
        "title": "string",
        "rationale": "string"
      }
    ],
    "timestamps": ["string"],
    "references": ["string"]
  }
  ```
