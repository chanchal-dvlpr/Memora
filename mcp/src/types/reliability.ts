export type HealthStatus = 'healthy' | 'degraded' | 'unhealthy';
export type BackpressureStrategy = 'reject' | 'queue' | 'timeout';
export type ShutdownState = 'accepting' | 'draining' | 'stopped';

export interface HealthComponentStatus {
  readonly name: string;
  readonly status: HealthStatus;
  readonly message?: string;
  readonly timestamp: number;
  readonly details?: Record<string, unknown>;
}

export interface MemorySnapshot {
  readonly heapUsedBytes: number;
  readonly heapTotalBytes: number;
  readonly rssBytes: number;
  readonly externalBytes: number;
  readonly arrayBuffersBytes: number;
}

export interface HealthReport {
  readonly status: HealthStatus;
  readonly timestamp: number;
  readonly uptimeSeconds: number;
  readonly components: ReadonlyArray<HealthComponentStatus>;
  readonly memory: MemorySnapshot;
}

export interface MetricsSnapshot {
  readonly totalRequests: number;
  readonly successfulRequests: number;
  readonly failedRequests: number;
  readonly timedOutRequests: number;
  readonly activeRequests: number;
  readonly queueLength: number;
  readonly averageQueueWaitTimeMs: number;
  readonly peakConcurrentRequests: number;
  readonly shutdownDurationMs: number;
  readonly cleanupDurationMs: number;
  readonly averageExecutionDurationMs: number;
  readonly maxExecutionDurationMs: number;
  readonly minExecutionDurationMs: number;
  readonly timestamp: number;
}

export type ReliabilityEventType = 
  | 'timeout'
  | 'queueOverflow'
  | 'requestAccepted'
  | 'requestCompleted'
  | 'requestFailed'
  | 'healthDegradation'
  | 'metricsSnapshot'
  | 'shutdownStarted'
  | 'shutdownCompleted';

export interface ReliabilityEvent {
  readonly type: ReliabilityEventType;
  readonly timestamp: number;
  readonly requestId?: string;
  readonly details?: Record<string, unknown>;
}
