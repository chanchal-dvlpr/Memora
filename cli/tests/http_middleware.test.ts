import { HttpClientService } from '../src/http/clientService';
import { RequestBuilder } from '../src/http/builder';
import { HttpTransport } from '../src/http/transport';
import { HttpMiddleware, TimeoutMiddleware, RetryMiddleware } from '../src/http/middleware';
import { BackendHealthChecker } from '../src/http/health';
import { ConfigService } from '../src/config/service';
import { ConfigLoader } from '../src/config/loader';
import { DefaultConfigSource } from '../src/config/source';
import { Logger } from '../src/logger/logger';
import { ExecutionContext } from '../src/models/context';
import { HttpError } from '../src/errors/errors';

describe('HTTP Middleware & Retry Subsystem', () => {
  let service: ConfigService;
  let logger: Logger;
  const mockCtx = {
    workingDir: '/workspace',
    requestId: 'req-123',
    correlationId: 'corr-456',
    env: {},
  } as unknown as ExecutionContext;

  beforeEach(() => {
    service = new ConfigService(new ConfigLoader([new DefaultConfigSource()]));
    logger = new Logger({ quiet: true });
  });

  it('should run middlewares in sequential onion order', async () => {
    await service.load();
    const order: number[] = [];

    const m1: HttpMiddleware = {
      name: 'm1',
      execute: async (_context, next) => {
        order.push(1);
        const res = await next();
        order.push(4);
        return res;
      },
    };

    const m2: HttpMiddleware = {
      name: 'm2',
      execute: async (_context, next) => {
        order.push(2);
        const res = await next();
        order.push(3);
        return res;
      },
    };

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        return { status: 200, headers: {}, data: 'ok' as unknown as T };
      },
    };

    const clientService = new HttpClientService(service, mockTransport, [m1, m2]);
    const res = await clientService.execute(RequestBuilder.get('/'), mockCtx);

    expect(res.data).toBe('ok');
    expect(order).toEqual([1, 2, 3, 4]);
  });

  it('should retry only for retryable status codes and errors', async () => {
    await service.load();
    let attempts = 0;

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        attempts++;
        if (attempts < 3) {
          throw new HttpError('Transient error', 503, 'GET', '/');
        }
        return { status: 200, headers: {}, data: 'success' as unknown as T };
      },
    };

    const clientService = new HttpClientService(service, mockTransport, [
      new RetryMiddleware(logger),
    ]);
    const res = await clientService.execute(RequestBuilder.get('/'), mockCtx);

    expect(res.data).toBe('success');
    expect(attempts).toBe(3);
  });

  it('should reject immediately for non-retryable 404 error codes', async () => {
    await service.load();
    let attempts = 0;

    const mockTransport: HttpTransport = {
      send: async () => {
        attempts++;
        throw new HttpError('Not found', 404, 'GET', '/');
      },
    };

    const clientService = new HttpClientService(service, mockTransport, [
      new RetryMiddleware(logger),
    ]);
    await expect(clientService.execute(RequestBuilder.get('/'), mockCtx)).rejects.toThrow(
      'Not found',
    );
    expect(attempts).toBe(1);
  });

  it('should trigger timeout rejections', async () => {
    await service.load();

    const mockTransport: HttpTransport = {
      send: async <T>(_builder: RequestBuilder, _ctx: ExecutionContext, options: { timeoutMs: number; signal?: AbortSignal }) => {
        await new Promise<void>((resolve, reject) => {
          const timer = setTimeout(resolve, 500);
          if (options.signal) {
            options.signal.addEventListener('abort', () => {
              clearTimeout(timer);
              const abortErr = new Error('The operation was aborted.');
              abortErr.name = 'AbortError';
              reject(abortErr);
            });
          }
        });
        return { status: 200, headers: {}, data: 'ok' as unknown as T };
      },
    };

    const clientService = new HttpClientService(service, mockTransport, [new TimeoutMiddleware()]);
    const builder = RequestBuilder.get('/').timeout(10);

    await expect(clientService.execute(builder, mockCtx)).rejects.toThrow('timed out after 10ms');
  });

  it('should query health checkers and measure UP status latency', async () => {
    await service.load();

    const mockTransport: HttpTransport = {
      send: async <T>() => {
        return { status: 200, headers: {}, data: { status: 'UP' } as unknown as T };
      },
    };

    const clientService = new HttpClientService(service, mockTransport, []);
    const checker = new BackendHealthChecker(clientService);
    const health = await checker.check(mockCtx);

    expect(health.status).toBe('UP');
    expect(health.latencyMs).toBeLessThan(100);
  });
});
