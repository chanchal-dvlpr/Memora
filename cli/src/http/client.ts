import { ConfigService } from '../config/service';
import { Logger } from '../logger/logger';
import { ExecutionContext } from '../models/context';
import { RequestBuilder } from './builder';
import { ResponseHandler } from './handler';
import { mapNetworkError } from './mapper';
import { CommandError } from '../errors/errors';

export interface HttpResponse<T> {
  readonly status: number;
  readonly headers: Record<string, string>;
  readonly data: T;
}

/**
 * Reusable HTTP client wrapper leveraging native fetch and structured CLI exceptions.
 */
export class CliHttpClient {
  private logger: Logger;

  constructor(
    private readonly configService: ConfigService,
    logger?: Logger,
  ) {
    this.logger = logger || new Logger({ verbose: true });
  }

  /**
   * Orchestrates constructing URL, merging headers, aborting on timeouts,
   * triggering fetch, and routing responses through the mapper.
   */
  public async execute<T>(
    builder: RequestBuilder,
    ctx: ExecutionContext,
  ): Promise<HttpResponse<T>> {
    const config = this.configService.getConfig();
    const baseUrl = config.backend.url;

    // Build URL path
    const cleanBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
    const cleanPath = builder.getPath().startsWith('/')
      ? builder.getPath()
      : '/' + builder.getPath();
    let url = cleanBase + cleanPath;

    // Append queries params
    const queryKeys = Object.keys(builder.getQueryParams());
    if (queryKeys.length > 0) {
      const searchParams = new URLSearchParams();
      for (const key of queryKeys) {
        searchParams.append(key, builder.getQueryParams()[key]);
      }
      url += '?' + searchParams.toString();
    }

    // Merge default and custom headers
    const headers: Record<string, string> = {
      'content-type': 'application/json',
      accept: 'application/json',
      'x-correlation-id': ctx.correlationId,
      'x-request-id': ctx.requestId,
      ...builder.getHeaders(),
    };

    const method = builder.getMethod();
    const timeoutMs = builder.getTimeoutMs() || config.backend.timeoutMs;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    const init: RequestInit = {
      method,
      headers,
      signal: controller.signal,
    };

    if (builder.getBody() !== undefined) {
      init.body = JSON.stringify(builder.getBody());
    }

    this.logger.debug(`[HTTP] Executing ${method} request to: ${url}`);
    const start = Date.now();

    try {
      const res = await fetch(url, init);
      clearTimeout(timeoutId);

      const elapsed = Date.now() - start;
      this.logger.debug(
        `[HTTP] Resolved ${method} ${url} with status ${res.status} in ${elapsed}ms`,
      );

      return await ResponseHandler.handle<T>(res, method, url);
    } catch (err: unknown) {
      clearTimeout(timeoutId);

      const errorName =
        err && typeof err === 'object' && 'name' in err
          ? (err as { name?: string }).name
          : '';
      if (errorName === 'AbortError') {
        throw new CommandError(`HTTP request timed out after ${timeoutMs}ms`);
      }

      this.logger.error(`[HTTP] Execution failed for ${method} ${url}: ${String(err)}`);
      throw mapNetworkError(err, url);
    }
  }
}
export default CliHttpClient;
