# Changelog

All notable changes to the Memora platform will be documented in this file.

---

## [1.0.0] - 2026-07-19

### Major Features
- **Semantic Codebase Indexing**: Performs fast AST-like scans of project directories to extract file dependencies, declarations, structural hashes, and symbol maps.
- **Hierarchical Project Resolution**: Enables the CLI to automatically detect registered projects when invoked from nested subdirectories (matching git root behaviors).
- **AI Handoff Generation**: Renders complete markdown reports of current project state, active tasks, and modified files to prompt LLM context windows.
- **Model Context Protocol (MCP) Server**: Provides full compliance with the Anthropic Model Context Protocol, exposing project tools, prompts, and resources.
- **Robust CLI Tooling**: Native Node CLI supporting commands like `init`, `projects`, `refresh`, `handoff`, `search`, `doctor`, `status`, and `unregister`.

### Architecture
- **Local-First Infrastructure**: Local Spring Boot backend backed by an H2 database and JSON cache store. No telemetry or source code leaves the developer's computer.
- **Decoupled CLI & Backend**: The TypeScript CLI connects via standard REST APIs, allowing other IDE extension clients to easily reuse the engine.

### Bug Fixes & Stabilization
- **CLI Executable Permissions**: Added execution permissions and fixed packaging scripts to bundle correct executable scripts for macOS, Linux, and Windows.
- **Hierarchical Path Resolution**: Fixed lookup failures when executing commands from project module subfolders (e.g. `cli/`, `backend/`).
- **Endpoint Routing Consolidation**: Resolved backend HTTP routing issues (`/api/v1/projects/{id}/refresh`, `/api/v1/context/{id}`, `/api/v1/knowledge/query`) returning 500 status when static resources were requested.
- **Version Reporting Consistency**: Aligned CLI version and status reporting checks to ensure consistent output across commands.
- **Graceful Unregistration**: Added support for gracefully unregistering paths to resolve overlaps and duplicate registrations.

### Performance
- **Scalable Scanner**: Scans 5,000+ files and symbols in under 550ms on incremental runs.
- **Scale Limits**: Successfully validated indexing scale up to 25,000 entities in under 11 seconds.

### Known Limitations
- Overlapping nested project roots must be manually unregistered to prevent scanner conflicts.
- Large binary files (such as `.vscode-test/` binaries) should be excluded from scans to optimize disk space.
