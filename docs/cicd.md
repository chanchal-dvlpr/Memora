# Memora MCP Server — CI/CD Documentation

Documentation for continuous integration and release automation pipelines.

## 1. CI Pipeline (`ci.yml`)

Triggers on pull requests and pushes to `main` and `develop` branches:
- **Matrix**: Node.js 18.x, 20.x, 22.x.
- **Steps**: Dependency installation (`npm ci`), `typecheck`, `lint`, test suite execution (`npm test`), production compilation (`npm run build`).
- **Docker Validation**: Static Docker build dry-run (`Dockerfile` compilation test).

---

## 2. Release Pipeline (`release.yml`)

Triggers via manual workflow dispatch (`workflow_dispatch`):
- Executes full quality gate.
- Generates npm package artifact preview (`npm pack --dry-run`).
- Builds release preview Docker image.
