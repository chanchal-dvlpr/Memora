import { ConfigService } from '../config/service';
import { Logger } from '../logger/logger';
import { CommandEventPublisher } from '../events/commandEvents';
import { ExecutionContext } from '../models/context';

export interface CommandContext {
  readonly configService: ConfigService;
  readonly logger: Logger;
  readonly outputService: {
    write(data: unknown): void | Promise<void>;
  };
  readonly eventPublisher: CommandEventPublisher;
  readonly executionContext: ExecutionContext;
  readonly globalOptions: {
    readonly json: boolean;
    readonly verbose: boolean;
    readonly quiet: boolean;
    readonly config?: string;
  };
  readonly workspacePlaceholder?: unknown;
  readonly authPlaceholder?: unknown;
}

import { OutputService } from '../output/outputService';

/**
 * Factory creating the shared runtime CommandContext container.
 */
export function createCommandContext(
  configService: ConfigService,
  eventPublisher: CommandEventPublisher,
  execCtx: ExecutionContext,
): CommandContext {
  return {
    configService,
    logger: execCtx.logger,
    outputService: new OutputService(execCtx, eventPublisher),
    eventPublisher,
    executionContext: execCtx,
    globalOptions: {
      json: execCtx.outputMode === 'json',
      verbose: execCtx.verbosity === 'verbose',
      quiet: execCtx.verbosity === 'quiet',
      config: execCtx.options.config as string | undefined,
    },
  };
}
