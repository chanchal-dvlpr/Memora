import { RequestTimeoutError } from '../errors';

export class TimeoutManager {
  private defaultTimeoutMs: number;

  constructor(defaultTimeoutMs = 30000) {
    this.defaultTimeoutMs = defaultTimeoutMs;
  }

  public getDefaultTimeoutMs(): number {
    return this.defaultTimeoutMs;
  }

  public setDefaultTimeoutMs(timeoutMs: number): void {
    this.defaultTimeoutMs = timeoutMs;
  }

  /**
   * Wraps an asynchronous operation with a timeout guard.
   */
  public async executeWithTimeout<T>(
    operation: () => Promise<T>,
    timeoutMs = this.defaultTimeoutMs,
    reason = 'Request timed out'
  ): Promise<T> {
    if (timeoutMs <= 0) {
      return operation();
    }

    let timer: NodeJS.Timeout | null = null;

    const timeoutPromise = new Promise<never>((_, reject) => {
      timer = setTimeout(() => {
        reject(new RequestTimeoutError(`${reason} after ${timeoutMs}ms`));
      }, timeoutMs);
    });

    try {
      const result = await Promise.race([operation(), timeoutPromise]);
      return result;
    } finally {
      if (timer) {
        clearTimeout(timer);
      }
    }
  }

  /**
   * Static utility helper function for wrapping operations with a timeout.
   */
  public static async withTimeout<T>(
    operation: () => Promise<T>,
    timeoutMs: number,
    reason = 'Operation timed out'
  ): Promise<T> {
    const manager = new TimeoutManager(timeoutMs);
    return manager.executeWithTimeout(operation, timeoutMs, reason);
  }
}
