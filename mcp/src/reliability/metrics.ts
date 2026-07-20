import { MetricsSnapshot } from '../types/reliability';
import { ConcurrencyManager } from './concurrency';
import { ShutdownManager } from './shutdown';

export class MetricsManager {
  private totalRequests = 0;
  private successfulRequests = 0;
  private failedRequests = 0;
  private timedOutRequests = 0;
  private activeRequests = 0;

  private minExecutionDurationMs = Infinity;
  private maxExecutionDurationMs = 0;
  private totalExecutionDurationMs = 0;
  private completedDurationsCount = 0;

  private concurrencyManager?: ConcurrencyManager;
  private shutdownManager?: ShutdownManager;

  constructor(concurrencyManager?: ConcurrencyManager, shutdownManager?: ShutdownManager) {
    this.concurrencyManager = concurrencyManager;
    this.shutdownManager = shutdownManager;
  }

  public setConcurrencyManager(manager: ConcurrencyManager): void {
    this.concurrencyManager = manager;
  }

  public setShutdownManager(manager: ShutdownManager): void {
    this.shutdownManager = manager;
  }

  public recordRequestAccepted(): void {
    this.totalRequests++;
    this.activeRequests++;
  }

  public recordRequestCompleted(durationMs: number): void {
    this.activeRequests = Math.max(0, this.activeRequests - 1);
    this.successfulRequests++;
    this.recordDuration(durationMs);
  }

  public recordRequestFailed(durationMs?: number): void {
    this.activeRequests = Math.max(0, this.activeRequests - 1);
    this.failedRequests++;
    if (durationMs !== undefined) {
      this.recordDuration(durationMs);
    }
  }

  public recordRequestTimeout(durationMs?: number): void {
    this.activeRequests = Math.max(0, this.activeRequests - 1);
    this.timedOutRequests++;
    if (durationMs !== undefined) {
      this.recordDuration(durationMs);
    }
  }

  private recordDuration(durationMs: number): void {
    this.completedDurationsCount++;
    this.totalExecutionDurationMs += durationMs;
    if (durationMs < this.minExecutionDurationMs) {
      this.minExecutionDurationMs = durationMs;
    }
    if (durationMs > this.maxExecutionDurationMs) {
      this.maxExecutionDurationMs = durationMs;
    }
  }

  public getSnapshot(): MetricsSnapshot {
    const avgDuration = this.completedDurationsCount > 0
      ? this.totalExecutionDurationMs / this.completedDurationsCount
      : 0;

    const snapshot: MetricsSnapshot = {
      totalRequests: this.totalRequests,
      successfulRequests: this.successfulRequests,
      failedRequests: this.failedRequests,
      timedOutRequests: this.timedOutRequests,
      activeRequests: this.concurrencyManager
        ? this.concurrencyManager.getActiveRequestsCount()
        : this.activeRequests,
      queueLength: this.concurrencyManager ? this.concurrencyManager.getQueuedRequestsCount() : 0,
      averageQueueWaitTimeMs: this.concurrencyManager ? this.concurrencyManager.getAverageQueueWaitTimeMs() : 0,
      peakConcurrentRequests: this.concurrencyManager ? this.concurrencyManager.getPeakConcurrentRequests() : 0,
      shutdownDurationMs: this.shutdownManager ? this.shutdownManager.getShutdownDurationMs() : 0,
      cleanupDurationMs: this.shutdownManager ? this.shutdownManager.getCleanupDurationMs() : 0,
      averageExecutionDurationMs: avgDuration,
      maxExecutionDurationMs: this.maxExecutionDurationMs,
      minExecutionDurationMs: this.minExecutionDurationMs === Infinity ? 0 : this.minExecutionDurationMs,
      timestamp: Date.now(),
    };

    return Object.freeze(snapshot);
  }

  public reset(): void {
    this.totalRequests = 0;
    this.successfulRequests = 0;
    this.failedRequests = 0;
    this.timedOutRequests = 0;
    this.activeRequests = 0;
    this.minExecutionDurationMs = Infinity;
    this.maxExecutionDurationMs = 0;
    this.totalExecutionDurationMs = 0;
    this.completedDurationsCount = 0;
  }
}
