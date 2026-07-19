import { ConfigService, configService } from '../config/service';
import { Logger } from '../logger/logger';
import { ExecutionContext } from '../models/context';
import { RequestBuilder } from './builder';
import { HttpResponse } from './client';
import { HttpTransport, NodeFetchTransport } from './transport';
import {
  HttpMiddleware,
  HttpMiddlewareContext,
  HttpPipeline,
  HeadersMiddleware,
  RequestLoggerMiddleware,
  TimeoutMiddleware,
  RetryMiddleware,
} from './middleware';

/**
 * Centered Service API orchestrating middleware pipelines and transport executions.
 */
export class HttpClientService {
  private pipeline: HttpPipeline;

  constructor(
    private readonly configService: ConfigService,
    transport: HttpTransport,
    middlewares: HttpMiddleware[],
  ) {
    this.pipeline = new HttpPipeline(middlewares, transport);
  }

  /**
   * Executes request mappings, setting up middleware contexts dynamically.
   */
  public async execute<T>(
    builder: RequestBuilder,
    ctx: ExecutionContext,
  ): Promise<HttpResponse<T>> {
    const config = this.configService.getConfig();
    const context: HttpMiddlewareContext = {
      builder,
      ctx,
      timeoutMs: config.backend.timeoutMs,
      maxRetries: config.backend.retryCount,
    };

    return this.pipeline.run<T>(context);
  }
}

const logger = new Logger({ verbose: true });

export const httpClientService = new HttpClientService(
  configService,
  new NodeFetchTransport(configService),
  [
    new HeadersMiddleware(),
    new RequestLoggerMiddleware(logger),
    new RetryMiddleware(logger),
    new TimeoutMiddleware(),
  ],
);
export default httpClientService;
