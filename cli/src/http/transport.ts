import { RequestBuilder } from './builder';
import { HttpResponse } from './client';
import { ResponseHandler } from './handler';
import { ExecutionContext } from '../models/context';
import { ConfigService } from '../config/service';

/**
 * Interface contract representing network connection executors.
 */
export interface HttpTransport {
  send<T>(
    builder: RequestBuilder,
    ctx: ExecutionContext,
    options: { timeoutMs: number; signal?: AbortSignal },
  ): Promise<HttpResponse<T>>;
}

/**
 * Standard fetch-based executor mapping builder specifications dynamically.
 */
export class NodeFetchTransport implements HttpTransport {
  constructor(private readonly configService: ConfigService) {}

  public async send<T>(
    builder: RequestBuilder,
    _ctx: ExecutionContext,
    options: { timeoutMs: number; signal?: AbortSignal },
  ): Promise<HttpResponse<T>> {
    const config = this.configService.getConfig();
    const baseUrl = config.backend.url;

    const cleanBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
    const cleanPath = builder.getPath().startsWith('/')
      ? builder.getPath()
      : '/' + builder.getPath();
    let url = cleanBase + cleanPath;

    const queryKeys = Object.keys(builder.getQueryParams());
    if (queryKeys.length > 0) {
      const searchParams = new URLSearchParams();
      for (const key of queryKeys) {
        searchParams.append(key, builder.getQueryParams()[key]);
      }
      url += '?' + searchParams.toString();
    }

    const headers = builder.getHeaders();
    const method = builder.getMethod();

    const init: RequestInit = {
      method,
      headers,
      signal: options.signal,
    };

    if (builder.getBody() !== undefined) {
      init.body = JSON.stringify(builder.getBody());
    }

    const res = await fetch(url, init);
    return await ResponseHandler.handle<T>(res, method, url);
  }
}
