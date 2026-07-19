# Memora CLI

Command Line Interface client for the Memora Context Engine daemon.

---

## 1. Project Directory Structure

```
cli/
├── src/
│   ├── commands/      # Subcommands execution handlers (init, handoff, projects, etc.)
│   ├── client/        # Loopback REST HTTP client wrappers
│   ├── config/        # User configuration loading and validation engines
│   ├── formatter/     # Output formatting services (ANSI color tables / JSON)
│   ├── logger/        # Standard logger wrappers for stdout / stderr
│   ├── models/        # Shared REST request/response schemas & DTOs
│   ├── services/      # Local repository auditing & diagnostics checks
│   ├── utils/         # Reusable string, array, and system utilities
│   ├── validators/    # Path checks and argument string checkers
│   ├── errors/        # Centralized CLI error mappings
│   ├── cli.ts         # Main bootstrap command router
│   └── index.ts       # CLI binary entry point (includes hashbang header)
├── tests/             # Jest unit and integration tests suites
├── package.json       # Metadata, scripts, and package dependencies
├── tsconfig.json      # TypeScript compiler configurations
├── .eslintrc.json     # ESLint static analysis checker parameters
├── .prettierrc        # Code formatting rules
└── LICENSE            # MIT license file
```

---

## 2. Compiler Selections Justifications

- **ES2022 Target**: Generates modern JavaScript syntax that executes natively on active LTS Node.js engines.
- **CommonJS Module Generation**: Ensures straightforward packaging, compatibility with binary compilation tools, and standard global/local script execution patterns.
- **Strict Mode Checking**: Catches potential runtime null pointers and typings issues at compilation time rather than runtime.
- **Unused Declarations Flags**: Prevents dead code and imports from accumulating in output dist packages.

---

## 3. Developer Onboarding & Development Workflow

### Requirements
- **Node.js**: Modern LTS version `(>=18.0.0)`
- **NPM**: Standard package manager `(>=9.0.0)`

### 1. Installation
Run clean installation to resolve devDependencies:
```bash
npm install
```

### 2. Development Execution
Run the CLI directly from typescript sources:
```bash
npm run dev
```
Execute with arguments:
```bash
npm run dev -- init --name "example"
```

### 3. Build & Local Verification
Compile typescript sources to commonJS assets:
```bash
npm run build
```
Verify generated output builds:
```bash
npm start
```
Check static code quality using linter and formatting engines:
```bash
npm run lint
npm run format
npm run typecheck
```

---

## 4. CLI Commands Reference

- `init [path]` - Register a workspace directory (defaults to current directory).
- `projects` - List all registered workspaces.
- `project register [path]` - Register a project directory.
- `project list` - List all registered projects.
- `project show <id>` - Show details for a specific project.
- `unregister [path|id]` - Unregister a project workspace (interactive confirmation required, bypass with `--force`).
- `project unregister [path|id]` - Subcommand alias to unregister a project workspace.
- `refresh [path|id]` - Trigger file scan and snapshot generation.
- `handoff` - Perform handoff checks and diagnostic scanning.
- `status` - Check backend connectivity and CLI status.
- `version` - Print CLI version metadata.

