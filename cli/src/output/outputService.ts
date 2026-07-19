import * as fs from 'fs';
import { ExecutionContext } from '../models/context';
import { CommandEventPublisher } from '../events/commandEvents';
import { RenderRequest, RenderResult, deepFreeze } from './types';
import { validateRenderRequest } from '../validators/output';
import { prettyRenderer } from './pretty';
import { jsonRenderer } from './json';
import { markdownRenderer } from './markdown';
import { tableRenderer } from './table';
import { ValidationError } from '../errors/errors';

/**
 * Orchestrates pre-render validation, renderer execution, lifecycle event publishing, and destination exporting.
 */
export class OutputService {
  constructor(
    private readonly execCtx: ExecutionContext,
    private readonly eventPublisher: CommandEventPublisher,
  ) {}

  /**
   * High-level entrypoint matching existing commands. Wraps raw data in RenderRequest and exports.
   */
  public async write(data: unknown): Promise<void> {
    const isRequest =
      data &&
      typeof data === 'object' &&
      'mode' in data &&
      'data' in data &&
      'destination' in data;

    if (isRequest) {
      await this.writeRequest(data as RenderRequest);
    } else {
      let mode: 'pretty' | 'json' | 'markdown' | 'table' = 'pretty';
      if (this.execCtx.outputMode === 'json') {
        mode = 'json';
      } else if (
        data &&
        typeof data === 'object' &&
        'columns' in data &&
        'rows' in data
      ) {
        mode = 'table';
      }

      const request: RenderRequest = {
        mode,
        data,
        destination: 'stdout',
      };
      await this.writeRequest(request);
    }
  }

  /**
   * Implements full validation, rendering, event and export pipelines.
   */
  public async writeRequest(request: RenderRequest): Promise<RenderResult> {
    // 1. Enforce Immutability of request
    const frozenRequest = deepFreeze({ ...request });

    const destStr =
      typeof frozenRequest.destination === 'string'
        ? frozenRequest.destination
        : frozenRequest.destination.type;

    // 2. Publish RenderingStarted Event
    this.eventPublisher.publish({
      type: 'RenderingStarted',
      timestamp: new Date(),
      payload: {
        mode: frozenRequest.mode,
        destination: destStr,
      },
    });

    try {
      // 3. Pre-render Validation
      validateRenderRequest(frozenRequest);
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : String(err);
      this.eventPublisher.publish({
        type: 'RenderingFailed',
        timestamp: new Date(),
        payload: {
          mode: frozenRequest.mode,
          destination: destStr,
          error: errMsg,
        },
      });
      throw err;
    }

    // 4. Select Renderer
    let renderer: { render(m: unknown): string };
    switch (frozenRequest.mode) {
      case 'pretty':
        renderer = prettyRenderer;
        break;
      case 'json':
        renderer = jsonRenderer;
        break;
      case 'markdown':
        renderer = markdownRenderer;
        break;
      case 'table':
        renderer = tableRenderer;
        break;
      default:
        throw new ValidationError(`Unsupported renderer mode: ${frozenRequest.mode}`);
    }

    let rendered: string;
    try {
      // 5. Render Output
      rendered = renderer.render(frozenRequest.data);
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : String(err);
      this.eventPublisher.publish({
        type: 'RenderingFailed',
        timestamp: new Date(),
        payload: {
          mode: frozenRequest.mode,
          destination: destStr,
          error: errMsg,
        },
      });
      throw err;
    }

    // 6. Publish RenderingCompleted Event
    this.eventPublisher.publish({
      type: 'RenderingCompleted',
      timestamp: new Date(),
      payload: {
        mode: frozenRequest.mode,
        destination: destStr,
        outputLength: rendered.length,
      },
    });

    // 7. Publish ExportStarted Event
    this.eventPublisher.publish({
      type: 'ExportStarted',
      timestamp: new Date(),
      payload: {
        destination: destStr,
      },
    });

    try {
      // 8. Execute Export Destination
      const dest = frozenRequest.destination;
      if (dest === 'stdout') {
        if (this.execCtx.verbosity !== 'quiet') {
          console.log(rendered);
        }
      } else if (dest === 'stderr') {
        console.error(rendered);
      } else {
        const type = dest.type;
        if (type === 'file' || type === 'html' || type === 'pdf' || type === 'csv') {
          fs.writeFileSync(dest.path, rendered, 'utf8');
        } else if (type === 'clipboard') {
          // FUTURE CLIPBOARD PLACEHOLDER
          // No-op placeholder, but log it in debug/verbose
          this.execCtx.logger.debug('[ExportService] Copy to clipboard placeholder triggered');
        }
      }
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : String(err);
      this.eventPublisher.publish({
        type: 'ExportFailed',
        timestamp: new Date(),
        payload: {
          destination: destStr,
          error: errMsg,
        },
      });
      throw err;
    }

    const bytesWritten = Buffer.byteLength(rendered, 'utf8');

    // 9. Publish ExportCompleted Event
    this.eventPublisher.publish({
      type: 'ExportCompleted',
      timestamp: new Date(),
      payload: {
        destination: destStr,
        bytesWritten,
      },
    });

    // 10. Construct, deep freeze, and return RenderResult
    const result: RenderResult = {
      output: rendered,
      request: frozenRequest,
      metadata: {
        timestamp: new Date(),
        format: frozenRequest.mode,
        length: rendered.length,
      },
    };

    return deepFreeze(result);
  }
}
