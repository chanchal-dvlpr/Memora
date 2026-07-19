import { HttpClientService } from './clientService';
import { RequestBuilder } from './builder';
import { ExecutionContext } from '../models/context';

/**
 * Strongly typed response envelope for server health checks.
 */
export interface BackendHealth {
  readonly status: 'UP' | 'DOWN';
  readonly latencyMs: number;
  readonly message?: string;
}

/**
 * Core utility verifier evaluating daemon connection state and latency statistics.
 */
export class BackendHealthChecker {
  constructor(private readonly clientService: HttpClientService) {}

  /**
   * Queries health endpoints and captures response timing cycles.
   */
  public async check(ctx: ExecutionContext): Promise<BackendHealth> {
    const start = Date.now();
    try {
      const builder = RequestBuilder.get('/health');
      const res = await this.clientService.execute<{ status: string }>(builder, ctx);
      const latencyMs = Date.now() - start;

      const status = res.data && res.data.status === 'UP' ? 'UP' : 'DOWN';
      return { status, latencyMs };
    } catch (err: unknown) {
      const latencyMs = Date.now() - start;
      const msg = err instanceof Error ? err.message : String(err);
      return {
        status: 'DOWN',
        latencyMs,
        message: msg,
      };
    }
  }
}
