import { ShutdownError } from '../errors';
import { ShutdownState } from '../types/reliability';
import { ConcurrencyManager } from './concurrency';
import { ReliabilityEventEmitter } from './events';

export type CleanupHook = () => Promise<void>;

export class ShutdownManager {
  private state: ShutdownState = 'accepting';
  private readonly cleanupHooks = new Map<string, CleanupHook>();
  private readonly concurrencyManager?: ConcurrencyManager;
  private readonly eventEmitter?: ReliabilityEventEmitter;

  private shutdownDurationMs = 0;
  private cleanupDurationMs = 0;

  constructor(concurrencyManager?: ConcurrencyManager, eventEmitter?: ReliabilityEventEmitter) {
    this.concurrencyManager = concurrencyManager;
    this.eventEmitter = eventEmitter;
  }

  public getState(): ShutdownState {
    return this.state;
  }

  public isAcceptingRequests(): boolean {
    return this.state === 'accepting';
  }

  public getShutdownDurationMs(): number {
    return this.shutdownDurationMs;
  }

  public getCleanupDurationMs(): number {
    return this.cleanupDurationMs;
  }

  public registerCleanupHook(name: string, hook: CleanupHook): void {
    this.cleanupHooks.set(name, hook);
  }

  /**
   * Initiates graceful shutdown: stops new requests, drains queue, waits for active tasks, runs cleanup hooks.
   */
  public async initiateShutdown(timeoutMs = 10000): Promise<void> {
    if (this.state !== 'accepting') {
      return;
    }

    const startTime = Date.now();
    this.state = 'draining';

    if (this.eventEmitter) {
      await this.eventEmitter.emit({
        type: 'shutdownStarted',
        timestamp: startTime,
      });
    }

    // Drain queued requests
    if (this.concurrencyManager) {
      this.concurrencyManager.drainQueue(
        new ShutdownError('Server is shutting down. Queued request cancelled.')
      );
    }

    // Wait for active requests to finish or timeout
    const activeWaitPromise = this.waitForActiveRequests();
    let timer: NodeJS.Timeout | null = null;
    const timeoutPromise = new Promise<void>((resolve) => {
      timer = setTimeout(resolve, timeoutMs);
    });

    await Promise.race([activeWaitPromise, timeoutPromise]);
    if (timer) {
      clearTimeout(timer);
    }

    // Execute cleanup hooks
    const cleanupStartTime = Date.now();
    for (const [name, hook] of this.cleanupHooks.entries()) {
      try {
        await hook();
      } catch (err) {
        console.error(`Error executing shutdown cleanup hook "${name}":`, err);
      }
    }
    this.cleanupDurationMs = Date.now() - cleanupStartTime;

    this.state = 'stopped';
    this.shutdownDurationMs = Date.now() - startTime;

    if (this.eventEmitter) {
      await this.eventEmitter.emit({
        type: 'shutdownCompleted',
        timestamp: Date.now(),
        details: {
          shutdownDurationMs: this.shutdownDurationMs,
          cleanupDurationMs: this.cleanupDurationMs,
        },
      });
    }
  }

  private async waitForActiveRequests(): Promise<void> {
    if (!this.concurrencyManager) {
      return;
    }

    while (this.concurrencyManager.getActiveRequestsCount() > 0) {
      await new Promise((resolve) => setTimeout(resolve, 50));
    }
  }
}
