import * as fs from 'fs';
import * as path from 'path';

export interface ServerConfig {
  readonly serverName: string;
  readonly version: string;
  readonly buildMetadata?: string;
  readonly releaseMetadata?: string;
  readonly host: string;
  readonly port: number;
  readonly environment: 'development' | 'test' | 'testing' | 'staging' | 'production';
  readonly timeout: number;
  readonly logLevel: 'debug' | 'info' | 'warn' | 'error';
  readonly usePlaceholder?: boolean;
  readonly securityEnabled?: boolean;
  readonly defaultAuthProvider?: string;
  readonly defaultAuthzPolicy?: string;
  readonly auditLogEnabled?: boolean;
  readonly requestTimeoutMs?: number;
  readonly handlerTimeoutMs?: number;
  readonly maxConcurrentRequests?: number;
  readonly maxQueuedRequests?: number;
  readonly maxPayloadSizeBytes?: number;
  readonly shutdownTimeoutMs?: number;
  readonly healthCheckIntervalMs?: number;
  readonly metricsIntervalMs?: number;
}

export class ConfigLoader {
  public static load(): ServerConfig {
    const envInput = (process.env.NODE_ENV || 'development').toLowerCase();
    const environment: 'development' | 'test' | 'testing' | 'staging' | 'production' = 
      (envInput === 'test' || envInput === 'testing') ? 'test' :
      (envInput === 'staging') ? 'staging' :
      (envInput === 'production') ? 'production' : 'development';

    // Profile-specific defaults
    let defaultPort = 8081;
    let defaultLogLevel: 'debug' | 'info' | 'warn' | 'error' = 'info';
    let defaultRequestTimeoutMs = 30000;
    let defaultMaxConcurrentRequests = 50;

    if (environment === 'test') {
      defaultPort = 8082;
      defaultLogLevel = 'error';
      defaultRequestTimeoutMs = 30000;
      defaultMaxConcurrentRequests = 50;
    } else if (environment === 'staging') {
      defaultPort = 8080;
      defaultLogLevel = 'info';
      defaultRequestTimeoutMs = 15000;
      defaultMaxConcurrentRequests = 100;
    } else if (environment === 'production') {
      defaultPort = 8080;
      defaultLogLevel = 'info';
      defaultRequestTimeoutMs = 10000;
      defaultMaxConcurrentRequests = 200;
    } else {
      // development
      defaultPort = 8081;
      defaultLogLevel = 'info';
      defaultRequestTimeoutMs = 30000;
    }

    // Resolve version from VERSION file or env fallback
    let versionFromFile = '1.0.0';
    try {
      const versionFilePath = path.resolve(__dirname, '../../VERSION');
      if (fs.existsSync(versionFilePath)) {
        versionFromFile = fs.readFileSync(versionFilePath, 'utf-8').trim();
      }
    } catch {
      // fallback if file read fails
    }

    const version = process.env.MEMORA_MCP_SERVER_VERSION || versionFromFile;
    const buildMetadata = process.env.MEMORA_MCP_BUILD_METADATA || '20260720+prod';
    const releaseMetadata = process.env.MEMORA_MCP_RELEASE_METADATA || 'release-1.0.0';

    return {
      serverName: process.env.MEMORA_MCP_SERVER_NAME || 'memora-mcp-server',
      version,
      buildMetadata,
      releaseMetadata,
      host: process.env.MEMORA_MCP_SERVER_HOST || '127.0.0.1',
      port: process.env.MEMORA_MCP_SERVER_PORT 
        ? parseInt(process.env.MEMORA_MCP_SERVER_PORT, 10) 
        : defaultPort,
      environment,
      timeout: process.env.MEMORA_MCP_SERVER_TIMEOUT 
        ? parseInt(process.env.MEMORA_MCP_SERVER_TIMEOUT, 10) 
        : defaultRequestTimeoutMs,
      logLevel: (process.env.MEMORA_MCP_SERVER_LOG_LEVEL || defaultLogLevel) as 'debug' | 'info' | 'warn' | 'error',
      securityEnabled: process.env.MEMORA_MCP_SECURITY_ENABLED !== 'false',
      defaultAuthProvider: process.env.MEMORA_MCP_DEFAULT_AUTH_PROVIDER || 'mock',
      defaultAuthzPolicy: process.env.MEMORA_MCP_DEFAULT_AUTHZ_POLICY || 'allow-all',
      auditLogEnabled: process.env.MEMORA_MCP_AUDIT_LOG_ENABLED !== 'false',
      requestTimeoutMs: process.env.MEMORA_MCP_REQUEST_TIMEOUT_MS
        ? parseInt(process.env.MEMORA_MCP_REQUEST_TIMEOUT_MS, 10)
        : defaultRequestTimeoutMs,
      handlerTimeoutMs: process.env.MEMORA_MCP_HANDLER_TIMEOUT_MS
        ? parseInt(process.env.MEMORA_MCP_HANDLER_TIMEOUT_MS, 10)
        : defaultRequestTimeoutMs,
      maxConcurrentRequests: process.env.MEMORA_MCP_MAX_CONCURRENT_REQUESTS
        ? parseInt(process.env.MEMORA_MCP_MAX_CONCURRENT_REQUESTS, 10)
        : defaultMaxConcurrentRequests,
      maxQueuedRequests: process.env.MEMORA_MCP_MAX_QUEUED_REQUESTS
        ? parseInt(process.env.MEMORA_MCP_MAX_QUEUED_REQUESTS, 10)
        : 100,
      maxPayloadSizeBytes: process.env.MEMORA_MCP_MAX_PAYLOAD_SIZE_BYTES
        ? parseInt(process.env.MEMORA_MCP_MAX_PAYLOAD_SIZE_BYTES, 10)
        : 10485760,
      shutdownTimeoutMs: process.env.MEMORA_MCP_SHUTDOWN_TIMEOUT_MS
        ? parseInt(process.env.MEMORA_MCP_SHUTDOWN_TIMEOUT_MS, 10)
        : 10000,
      healthCheckIntervalMs: process.env.MEMORA_MCP_HEALTH_CHECK_INTERVAL_MS
        ? parseInt(process.env.MEMORA_MCP_HEALTH_CHECK_INTERVAL_MS, 10)
        : 60000,
      metricsIntervalMs: process.env.MEMORA_MCP_METRICS_INTERVAL_MS
        ? parseInt(process.env.MEMORA_MCP_METRICS_INTERVAL_MS, 10)
        : 60000,
    };
  }
}

