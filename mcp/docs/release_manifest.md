# Memora MCP Server — Production Release Manifest

**Release Version**: `1.0.0`  
**Build Date**: 2026-07-20  
**Package Name**: `memora-mcp`  
**Target Runtime**: Node.js `>= 18.0.0`  

---

## 1. Primary Distribution Artifacts

- **Compiled JavaScript Bundle**: `dist/index.js`
- **TypeScript Declarations**: `dist/index.d.ts`
- **CLI Executable Binary**: `memora-mcp` (`dist/index.js`)
- **Version Identifier File**: `VERSION` (`1.0.0`)

---

## 2. Container & Deployment Assets

- **Docker Image Definition**: `Dockerfile` (Node 20 Alpine, multi-stage, non-root execution `USER node`).
- **Docker Compose Spec**: `docker-compose.yml` (`memora-mcp-prod` & `memora-mcp-dev` services).
- **Docker Ignore Rules**: `.dockerignore`

---

## 3. Automation Workflows

- **Continuous Integration**: `.github/workflows/ci.yml`
- **Release Verification & Packaging**: `.github/workflows/release.yml`

---

## 4. Documentation & Operational Asset Set

- `docs/installation.md` — Installation guide (npm, Docker, manual)
- `docs/deployment.md` — Deployment guide & Kubernetes specs
- `docs/runbook.md` — Operational runbook & troubleshooting
- `docs/upgrade_guide.md` — Version upgrade & migration process
- `docs/release_checklist.md` — Pre-flight release checklist
- `docs/configuration_reference.md` — Complete environment variable reference
- `docs/cicd.md` — CI/CD automation documentation
- `docs/production_readiness_report.md` — Production readiness certification
- `docs/project_completion_report.md` — Phase 13 final completion report

---

## 5. Licensing & Legal Declarations

- `LICENSE` — MIT License
- `NOTICE` — Third-party attributions & acknowledgments
- `SECURITY.md` — Vulnerability disclosure policy
- `CODE_OF_CONDUCT.md` — Community standards
- `SUPPORTED_VERSIONS.md` — Support matrix
