import { RequestBuilder } from './builder';
import { HttpResponse } from './client';
import { HttpTransport } from './transport';
import { ExecutionContext } from '../models/context';
import { Logger } from '../logger/logger';
import { CommandError, HttpError } from '../errors/errors';

/**
 * Context container routed through HTTP middlewares.
 */
export interface HttpMiddlewareContext {
  builder: RequestBuilder;
  readonly ctx: ExecutionContext;
  timeoutMs: number;
  maxRetries: number;
  signal?: AbortSignal;
}

/**
 * Standard contract for intercepting HTTP client requests and responses.
 */
export interface HttpMiddleware {
  readonly name: string;
  execute(
    context: HttpMiddlewareContext,
    next: () => Promise<HttpResponse<unknown>>,
    transport: HttpTransport,
  ): Promise<HttpResponse<unknown>>;
}

/**
 * HTTP Middleware execution pipeline orchestrator.
 */
export class HttpPipeline {
  constructor(
    private readonly middlewares: HttpMiddleware[],
    private readonly transport: HttpTransport,
  ) {}

  public async run<T>(context: HttpMiddlewareContext): Promise<HttpResponse<T>> {
    const dispatch = async (i: number): Promise<HttpResponse<unknown>> => {
      if (i === this.middlewares.length) {
        return this.transport.send<T>(context.builder, context.ctx, {
          timeoutMs: context.timeoutMs,
          signal: context.signal,
        });
      }

      const middleware = this.middlewares[i];
      let activeCalls = 0;
      return middleware.execute(
        context,
        async () => {
          if (activeCalls > 0) {
            throw new Error(`next() called multiple times in middleware ${middleware.name}`);
          }
          activeCalls++;
          try {
            return await dispatch(i + 1);
          } finally {
            activeCalls--;
          }
        },
        this.transport,
      );
    };

    return dispatch(0) as Promise<HttpResponse<T>>;
  }
}

/**
 * Interceptor appending default content-type and correlation context headers.
 */
export class HeadersMiddleware implements HttpMiddleware {
  readonly name = 'Headers';

  public async execute(
    context: HttpMiddlewareContext,
    next: () => Promise<HttpResponse<unknown>>,
  ): Promise<HttpResponse<unknown>> {
    let builder = context.builder;

    if (!builder.getHeaders()['content-type'] && builder.getBody() !== undefined) {
      builder = builder.header('content-type', 'application/json');
    }
    if (!builder.getHeaders()['accept']) {
      builder = builder.header('accept', 'application/json');
    }

    builder = builder
      .header('x-correlation-id', context.ctx.correlationId)
      .header('x-request-id', context.ctx.requestId);

    context.builder = builder;
    return next();
  }
}

/**
 * Interceptor logging requests, statuses, and execution latencies.
 */
export class RequestLoggerMiddleware implements HttpMiddleware {
  readonly name = 'Logger';
  constructor(private readonly logger: Logger) {}

  public async execute(
    context: HttpMiddlewareContext,
    next: () => Promise<HttpResponse<unknown>>,
  ): Promise<HttpResponse<unknown>> {
    const builder = context.builder;
    this.logger.debug(`[HTTP] Sending ${builder.getMethod()} request to path: ${builder.getPath()}`);
    const start = Date.now();
    try {
      const response = await next();
      const elapsed = Date.now() - start;
      this.logger.debug(
        `[HTTP] Resolved ${builder.getMethod()} ${builder.getPath()} with status ${response.status} in ${elapsed}ms`,
      );
      return response;
    } catch (err: unknown) {
      const elapsed = Date.now() - start;
      this.logger.error(
        `[HTTP] Failed ${builder.getMethod()} ${builder.getPath()} in ${elapsed}ms: ${String(err)}`,
      );
      throw err;
    }
  }
}

/**
 * Interceptor binding AbortControllers and timeout rejections.
 */
export class TimeoutMiddleware implements HttpMiddleware {
  readonly name = 'Timeout';

  public async execute(
    context: HttpMiddlewareContext,
    next: () => Promise<HttpResponse<unknown>>,
  ): Promise<HttpResponse<unknown>> {
    const timeoutMs = context.builder.getTimeoutMs() || context.timeoutMs;
    const controller = new AbortController();
    context.signal = controller.signal;

    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const res = await next();
      clearTimeout(timeoutId);
      return res;
    } catch (err: unknown) {
      clearTimeout(timeoutId);

      const errorName =
        err && typeof err === 'object' && 'name' in err
          ? (err as { name?: string }).name
          : '';
      if (errorName === 'AbortError') {
        throw new CommandError(`HTTP request timed out after ${timeoutMs}ms`);
      }
      throw err;
    }
  }
}

/**
 * Interceptor executing transient connections and server error retries.
 */
export class RetryMiddleware implements HttpMiddleware {
  readonly name = 'Retry';
  constructor(private readonly logger: Logger) {}

  public async execute(
    context: HttpMiddlewareContext,
    next: () => Promise<HttpResponse<unknown>>,
  ): Promise<HttpResponse<unknown>> {
    const maxRetries = context.maxRetries;
    let delay = 100;

    for (let retries = 0; retries <= maxRetries; retries++) {
      try {
        return await next();
      } catch (err: unknown) {
        if (retries === maxRetries || !this.isRetryable(err)) {
          throw err;
        }

        this.logger.warn(
          `[HTTP] Retryable failure detected. Retrying request (${retries + 1}/${maxRetries}) in ${delay}ms...`,
        );

        await new Promise((resolve) => setTimeout(resolve, delay));
        delay *= 2;
      }
    }
    throw new Error('Unreachable');
  }

  private isRetryable(err: unknown): boolean {
    const msg = err instanceof Error ? err.message : String(err);
    if (
      msg.includes('ENOTFOUND') ||
      msg.includes('getaddrinfo') ||
      msg.includes('ECONNREFUSED') ||
      msg.includes('connect ECONNREFUSED')
    ) {
      return true;
    }

    if (err instanceof HttpError) {
      return err.statusCode === 503 || err.statusCode === 504 || err.statusCode === 429;
    }

    return false;
  }
}
