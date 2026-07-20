import { SecurityContext } from './security';

export type SessionId = string;

export type SessionState = 'active' | 'inactive' | 'expired' | 'closed';

export interface SessionMetadata {
  readonly createdAt: number;
  readonly clientMetadata?: Record<string, unknown>;
  readonly userAgent?: string;
  readonly ipAddress?: string;
}

export interface SessionAttributes {
  [key: string]: unknown;
}

export interface SessionStatus {
  readonly state: SessionState;
  readonly lastAccessedAt: number;
  readonly isExpired: boolean;
  readonly durationMs: number;
}

export interface SessionExpirationPolicy {
  readonly expirationType: 'sliding' | 'absolute' | 'manual' | 'none';
  readonly maxIdleTimeMs?: number;
  readonly absoluteTimeoutMs?: number;
  readonly isManuallyExpired?: boolean;
}

export interface SessionConfiguration {
  readonly expirationPolicy: SessionExpirationPolicy;
  readonly maxAttributesCount?: number;
  readonly cleanupIntervalMs?: number;
}

export interface SessionStatistics {
  readonly totalSessionsCreated: number;
  readonly activeSessionsCount: number;
  readonly expiredSessionsCount: number;
  readonly closedSessionsCount: number;
  readonly peakActiveSessionsCount: number;
  readonly averageSessionDurationMs: number;
  readonly touchCount: number;
  readonly cleanupCount: number;
  readonly removedContextsCount: number;
}

export interface SessionCleanupStatistics {
  readonly cleanupRuns: number;
  readonly removedSessionsCount: number;
  readonly removedContextsCount: number;
  readonly lastCleanupTimestamp?: number;
}

export interface SessionContext {
  readonly requestId?: string;
  readonly correlationId?: string;
  readonly requestMetadata?: Record<string, unknown>;
  readonly protocolMetadata?: Record<string, unknown>;
  readonly conversationMetadata?: Record<string, unknown>;
  readonly clientMetadata?: Record<string, unknown>;
  readonly clientInformation?: Record<string, unknown>;
  readonly runtimeMetadata?: Record<string, unknown>;
  readonly executionMetadata?: Record<string, unknown>;
  readonly securityContext?: SecurityContext;
}

export interface Session {
  readonly id: SessionId;
  readonly state: SessionState;
  readonly metadata: SessionMetadata;
  readonly attributes: SessionAttributes;
  readonly context: SessionContext;
  readonly lastAccessedAt: number;
  readonly isManuallyExpired?: boolean;
}

