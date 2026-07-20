# Memora MCP Server — Installation Guide

This document details the installation options and first startup procedure for the Memora MCP Server.

## Prerequisites

- **Node.js**: `>= 18.0.0` (LTS 18, 20, or 22 recommended)
- **npm**: `>= 9.0.0`
- **Docker** *(Optional)*: `>= 20.10.0` (for containerized deployments)

---

## 1. Installation Methods

### Option A: npm Global or Local Installation

```bash
# Global installation via npm
npm install -g memora-mcp

# Or local workspace installation
cd memora/mcp
npm install
npm run build
```

### Option B: Docker Container Deployment

```bash
# Pull and build multi-stage Docker image
docker build -t memora-mcp:latest .

# Run container with production environment profile
docker run -d \
  --name memora-mcp-server \
  -p 8080:8080 \
  -e NODE_ENV=production \
  memora-mcp:latest
```

### Option C: Docker Compose

```bash
# Start production service
docker-compose up -d memora-mcp-prod

# Start development profile
docker-compose --profile dev up -d memora-mcp-dev
```

---

## 2. Environment Variables & First Startup

Configure environment variables before initiating server execution:

```bash
export NODE_ENV=production
export MEMORA_MCP_SERVER_PORT=8080
export MEMORA_MCP_SERVER_HOST=0.0.0.0
export MEMORA_MCP_SERVER_LOG_LEVEL=info
export MEMORA_MCP_SECURITY_ENABLED=true
```

Start the server:

```bash
# Node runtime execution
node dist/index.js

# Or executable CLI invocation
memora-mcp
```
