# Multi-stage Dockerfile for Memora MCP Server (Monorepo Aware)

# --- Stage 1: Build Stage ---
FROM node:20-alpine AS builder

WORKDIR /app

# Copy cli package files and source, then build cli
COPY cli/package*.json ./cli/
COPY cli/tsconfig*.json ./cli/
COPY cli/src ./cli/src

WORKDIR /app/cli
RUN npm ci && npm run build

WORKDIR /app

# Copy mcp package files, source, and version file
COPY mcp/package*.json ./mcp/
COPY mcp/tsconfig*.json ./mcp/
COPY mcp/src ./mcp/src
COPY mcp/VERSION ./mcp/VERSION

WORKDIR /app/mcp
RUN npm ci && npm run build

# --- Stage 2: Production Stage ---
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production \
    MEMORA_MCP_SERVER_PORT=8080 \
    MEMORA_MCP_SERVER_HOST=0.0.0.0

# Copy built cli and mcp packages
COPY --from=builder /app/cli ./cli
COPY --from=builder /app/mcp ./mcp

WORKDIR /app/mcp

# Create non-root node user permissions
USER node

# Expose server port
EXPOSE 8080

# Healthcheck definition
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Start command
CMD ["node", "dist/index.js"]
