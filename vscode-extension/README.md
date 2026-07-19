# Memora — Local-first AI Context Engine for VS Code

Memora is a local-first development tool that connects your IDE workspace directly with a local context engine. It scans workspace directory trees, maps code patterns, registers architecture decisions, tracks active features, and structures developers' context for AI pair programming.

---

## Features

- **📊 Project Dashboard**: Monitor real-time status of workspace directory scans, active files, and symbol processing metrics.
- **📐 Architecture Explorer**: View structures and registers of key system topography and patterns.
- **📝 Work Explorer**: List active feature tasks, roadmap status, and developer progress.
- **🔍 Knowledge Search**: Run rapid indexing searches across local code symbols and compiled snapshot trees.
- **📋 AI Handoff**: Construct structured context markdown assemblies from local repository snapshots to easily pass context to LLMs.
- **💾 Local-First Persistence**: Leverages a local file-based database for seamless project metadata caching and immediate reload across restarts.

---

## Requirements

### Local Backend Daemon
Memora requires the local **Spring Boot backend daemon** to be running.
- **Default Port**: `9876` (or configure via settings)
- **Database**: H2 file-based local persistence

To run the backend daemon locally:
```bash
cd backend
./mvnw spring-boot:run
```

---

## Installation

1. Open Visual Studio Code.
2. Go to **Extensions** (Ctrl+Shift+X or Cmd+Shift+X).
3. Search for **Memora** and click **Install**, or package and install from VSIX:
   ```bash
   code --install-extension memora-vscode-0.0.1.vsix
   ```

---

## Commands

- `Memora: Register Project` — Scans the active workspace folder and registers it with the local backend context engine.
- `Memora: Refresh Context` — Refreshes the local metadata dashboard and tree views.
- `Memora: Search Knowledge` — Queries symbols and snapshot directories interactively via the QuickPick menu.
- `Memora: Generate AI Handoff` — Generates a comprehensive markdown handoff and copies it to the system clipboard.

---

## Architecture Overview

```
+------------------+             REST API             +------------------------+
|                  |  ----------------------------->  |                        |
|  VS Code         |        (Register Project)        |  Context Engine        |
|  Extension       |                                  |  Backend (Spring Boot) |
|  (Frontend UI)   |  <-----------------------------  |                        |
|                  |       (Lightweight Sync)         +-----------+------------+
+------------------+                                              |
                                                                  | JPA / Hibernate
                                                                  v
                                                      +------------------------+
                                                      |  Local Database        |
                                                      |  (Persistent H2 File)  |
                                                      +------------------------+
```

- **Extension Frontend**: Written in TypeScript, handles tree views, custom webviews, hover decoration providers, and command triggers.
- **Core Engine (Backend)**: Built with Spring Boot, orchestrates asynchronous workspace directories scanning, builds file tree structures, registers architectural decisions, and stores snapshot structures.
- **Persistent Database**: Leverages a file-backed H2 engine running in AUTO_SERVER mode to safely preserve registered projects and context states across restarts.

---

## Screenshots

*(Screenshots coming soon)*

---

## Development Instructions

### Prerequisites
- Node.js (v18 or newer)
- npm

### Setup
1. Clone the repository.
2. Open the `vscode-extension` subdirectory in VS Code.
3. Install dependencies:
   ```bash
   npm install
   ```

### Compile & Build
- Compile the TypeScript extension source:
  ```bash
  npm run compile
  ```
- Package the extension into a VSIX file:
  ```bash
  npx vsce package
  ```

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
