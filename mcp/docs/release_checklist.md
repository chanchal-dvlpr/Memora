# Production Release Checklist

Pre-flight verification items to be validated before authorizing a production release tag or package distribution.

## Checklist

### 1. Codebase & Type Safety
- [ ] `npm run typecheck` passes cleanly (0 errors).
- [ ] `npm run lint` passes cleanly (0 errors, 0 warnings).
- [ ] All unit and integration test suites pass (100% green, 0 skipped/flaky tests).
- [ ] Clean build compilation (`npm run build`).

### 2. Versioning & Package Manifest
- [ ] `VERSION` file contains target semver string (e.g., `1.0.0`).
- [ ] `package.json` contains matching `version` and complete metadata (`bin`, `files`, `repository`, `license`).
- [ ] `CHANGELOG.md` updated with release release notes.

### 3. Container & Deployment Verification
- [ ] Multi-stage `Dockerfile` builds without errors.
- [ ] Docker image passes vulnerability and non-root execution checks (`USER node`).
- [ ] `docker-compose.yml` services tested.

### 4. Legal & Licensing
- [ ] `LICENSE` (MIT) present.
- [ ] `NOTICE` file present with third-party dependency attributions.
- [ ] `SECURITY.md`, `CODE_OF_CONDUCT.md`, and `SUPPORTED_VERSIONS.md` present.

### 5. Documentation & Operations
- [ ] Operational runbook (`docs/runbook.md`) up to date.
- [ ] Upgrade guide (`docs/upgrade_guide.md`) up to date.
- [ ] Configuration reference (`docs/configuration_reference.md`) up to date.
