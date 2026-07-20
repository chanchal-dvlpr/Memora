import { QueueOverflowError, RequestTimeoutError } from '../errors';
import { BackpressureStrategy } from '../types/reliability';

interface QueuedItem {
  readonly id: string;
  readonly enqueuedAt: number;
  readonly resolve: () => void;
  readonly reject: (reason: Error) => void;
  timer?: NodeJS.Timeout;
}

export class ConcurrencyManager {
  private readonly maxConcurrentRequests: number;
  private readonly maxQueuedRequests: number;
  private readonly backpressureStrategy: BackpressureStrategy;
  private readonly queueWaitTimeoutMs: number;

  private activeRequests = 0;
  private peakConcurrentRequests = 0;
  private totalQueueWaitTimeMs = 0;
  private queuedTasksProcessed = 0;
  private readonly queue: QueuedItem[] = [];

  constructor(
    maxConcurrentRequests = 50,
    maxQueuedRequests = 100,
    backpressureStrategy: BackpressureStrategy = 'queue',
    queueWaitTimeoutMs = 5000
  ) {
    this.maxConcurrentRequests = maxConcurrentRequests;
    this.maxQueuedRequests = maxQueuedRequests;
    this.backpressureStrategy = backpressureStrategy;
    this.queueWaitTimeoutMs = queueWaitTimeoutMs;
  }

  public getActiveRequestsCount(): number {
    return this.activeRequests;
  }

  public getQueuedRequestsCount(): number {
    return this.queue.length;
  }

  public getMaxConcurrentRequests(): number {
    return this.maxConcurrentRequests;
  }

  public getMaxQueuedRequests(): number {
    return this.maxQueuedRequests;
  }

  public getPeakConcurrentRequests(): number {
    return this.peakConcurrentRequests;
  }

  public getAverageQueueWaitTimeMs(): number {
    return this.queuedTasksProcessed > 0
      ? this.totalQueueWaitTimeMs / this.queuedTasksProcessed
      : 0;
  }

  public getQueueDetails(): ReadonlyArray<{ id: string; enqueuedAt: number; waitTimeMs: number }> {
    const now = Date.now();
    return this.queue.map((q) => ({
      id: q.id,
      enqueuedAt: q.enqueuedAt,
      waitTimeMs: now - q.enqueuedAt,
    }));
  }

  /**
   * Executes a task subject to concurrency limits and configured backpressure strategies.
   */
  public async execute<T>(task: () => Promise<T>): Promise<T> {
    if (this.activeRequests < this.maxConcurrentRequests) {
      this.incrementActive();
    } else if (this.backpressureStrategy === 'reject') {
      throw new QueueOverflowError(
        `Backpressure reject: Maximum active concurrent limit (${this.maxConcurrentRequests}) reached.`
      );
    } else if (this.queue.length < this.maxQueuedRequests) {
      await this.enqueueAndWait();
      this.incrementActive();
    } else {
      throw new QueueOverflowError(
        `Queue overflow: Maximum queued requests limit (${this.maxQueuedRequests}) exceeded.`
      );
    }

    try {
      const result = await task();
      return result;
    } finally {
      this.activeRequests--;
      this.processNextInQueue();
    }
  }

  private incrementActive(): void {
    this.activeRequests++;
    if (this.activeRequests > this.peakConcurrentRequests) {
      this.peakConcurrentRequests = this.activeRequests;
    }
  }

  private enqueueAndWait(): Promise<void> {
    const id = Math.random().toString(36).substring(2, 9);
    const enqueuedAt = Date.now();

    return new Promise<void>((resolve, reject) => {
      let timer: NodeJS.Timeout | undefined;

      if (this.backpressureStrategy === 'timeout' || this.queueWaitTimeoutMs > 0) {
        timer = setTimeout(() => {
          const idx = this.queue.findIndex((item) => item.id === id);
          if (idx !== -1) {
            this.queue.splice(idx, 1);
            reject(
              new RequestTimeoutError(
                `Queue wait timeout: Request queued for longer than ${this.queueWaitTimeoutMs}ms`
              )
            );
          }
        }, this.queueWaitTimeoutMs);
      }

      const item: QueuedItem = {
        id,
        enqueuedAt,
        resolve: () => {
          if (timer) {
            clearTimeout(timer);
          }
          const waitTime = Date.now() - enqueuedAt;
          this.totalQueueWaitTimeMs += waitTime;
          this.queuedTasksProcessed++;
          resolve();
        },
        reject: (err) => {
          if (timer) {
            clearTimeout(timer);
          }
          reject(err);
        },
        timer,
      };

      this.queue.push(item);
    });
  }

  private processNextInQueue(): void {
    if (this.queue.length > 0 && this.activeRequests < this.maxConcurrentRequests) {
      const nextTask = this.queue.shift();
      if (nextTask) {
        nextTask.resolve();
      }
    }
  }

  /**
   * Rejects all currently queued requests (used during shutdown).
   */
  public drainQueue(error: Error): void {
    while (this.queue.length > 0) {
      const item = this.queue.shift();
      if (item) {
        if (item.timer) {
          clearTimeout(item.timer);
        }
        item.reject(error);
      }
    }
  }
}
