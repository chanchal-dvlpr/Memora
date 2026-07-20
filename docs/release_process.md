# Memora MCP Server — Release Process

This document defines the release engineering workflow and validation procedures for releasing new versions of the Memora MCP Server.

## 1. Versioning Standard

The project strictly follows [Semantic Versioning 2.0.0](https://semver.org/):
- **MAJOR** (`X.0.0`): Incompatible API or protocol breaking changes.
- **MINOR** (`0.X.0`): Backward-compatible feature additions or protocol extensions.
- **PATCH** (`0.0.X`): Backward-compatible bug fixes or security patches.

---

## 2. Release Verification Checklist

Before tagging or publishing any release:

- [ ] `VERSION` file updated with target semver string.
- [ ] `package.json` version matches `VERSION`.
- [ ] `CHANGELOG.md` updated with release highlights.
- [ ] Production build succeeds (`npm run build`).
- [ ] Type check succeeds with 0 errors (`npm run typecheck`).
- [ ] Linter succeeds with 0 errors/warnings (`npm run lint`).
- [ ] Full test suite succeeds with 100% pass rate (`npm test`).
- [ ] Docker container builds cleanly (`docker build -t memora-mcp:latest .`).
- [ ] License and attribution files (`LICENSE`, `NOTICE`) present.

---

## 3. Packaging & Distribution Artifacts

Target release artifacts:
1. **npm Package**: `memora-mcp-1.0.0.tgz` (generated via `npm pack`).
2. **Docker Image**: `memora-mcp:1.0.0` published to Docker Hub / GitHub Container Registry.
