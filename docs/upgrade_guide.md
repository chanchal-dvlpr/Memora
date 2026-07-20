# Memora MCP Server — Upgrade & Migration Guide

Instructions for version upgrades, configuration migration, backward compatibility expectations, and rollback.

## 1. Version Upgrade Process

### Upgrading npm Installation
```bash
# Update globally installed package
npm update -g memora-mcp

# Or rebuild in workspace
git pull origin main
cd mcp
npm ci
npm run build
```

### Upgrading Docker Containers
```bash
# Pull new image and restart container
docker-compose pull memora-mcp-prod
docker-compose up -d memora-mcp-prod
```

---

## 2. Configuration Migration

`ServerConfig` loader maintains backward compatibility for legacy environment parameters while recognizing new `MEMORA_MCP_*` variables.

---

## 3. Compatibility Expectations

- **SemVer Guarantee**: Minor and Patch releases (1.x.y) retain full protocol and API backward compatibility.
- **MCP Protocol**: Compatible with MCP SDK `v0.6.0+`.

---

## 4. Rollback Guidance

If an upgrade fails:
1. Stop running process / container.
2. Restore previous version binary or Docker tag (`memora-mcp:1.0.0`).
3. Restore backup configuration `.memorarc.bak`.
