# Multi-stage Dockerfile for Memora MCP Server

# --- Stage 1: Build Stage ---
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig*.json ./

# Install dependencies (including devDependencies for build)
RUN npm ci

# Copy source code and version file
COPY src ./src
COPY VERSION ./VERSION

# Build production bundle
RUN npm run build

# --- Stage 2: Production Stage ---
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production \
    MEMORA_MCP_SERVER_PORT=8080 \
    MEMORA_MCP_SERVER_HOST=0.0.0.0

# Copy package files and install production-only dependencies
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

# Copy compiled dist, VERSION, and notice/license files
COPY --from=builder /app/dist ./dist
COPY VERSION LICENSE NOTICE ./

# Create non-root node user permissions
USER node

# Expose server port
EXPOSE 8080

# Healthcheck definition
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Start command
CMD ["node", "dist/index.js"]
