import * as crypto from 'crypto';
import { ExecutionContext } from '../models/context';
import { Logger } from '../logger/logger';
import { Pipeline } from '../utils/middleware';
import { CliError } from '../errors/errors';
import { setExitCode } from '../cli';
import { commandEventPublisher } from '../events/commandEvents';
import { configService } from '../config/service';
import { createCommandContext, CommandContext } from './commandContext';

/**
 * Command execution orchestrator that manages context lifecycles and middleware Onion executions.
 */
export class CommandDispatcher {
  private pipeline = new Pipeline();

  constructor() {
    this.setupMiddleware();
  }

  /**
   * Configures global middleware handlers compliant with the CliMiddleware contract.
   */
  private setupMiddleware(): void {
    // 1. Error interceptor
    this.pipeline.use({
      name: 'ErrorCatcher',
      execute: async (ctx, next) => {
        try {
          await next();
        } catch (error: unknown) {
          const err = error as Error;
          if (err instanceof CliError) {
            ctx.logger.error(err.message);
            throw err;
          } else {
            ctx.logger.error(`Unexpected exception occurred: ${err.message}`);
            throw err;
          }
        }
      },
    });

    // 2. Timing logging
    this.pipeline.use({
      name: 'Timing',
      execute: async (ctx, next) => {
        const start = Date.now();
        ctx.logger.trace(`Starting command execution: ${ctx.commandName}`, {
          requestId: ctx.requestId,
        });
        await next();
        const elapsed = Date.now() - start;
        ctx.logger.trace(`Completed execution of '${ctx.commandName}' in ${elapsed}ms`, {
          elapsedMs: elapsed,
        });
      },
    });

    // 3. Diagnostics trace logging
    this.pipeline.use({
      name: 'Diagnostics',
      execute: async (ctx, next) => {
        ctx.logger.debug('Execution Context metadata resolved:', {
          requestId: ctx.requestId,
          correlationId: ctx.correlationId,
          workingDir: ctx.workingDir,
          outputMode: ctx.outputMode,
          verbosity: ctx.verbosity,
        });
        await next();
      },
    });
  }

  /**
   * Creates execution context and runs action callbacks inside middleware pipeline.
   */
  public async dispatch(
    commandName: string,
    args: string[],
    options: Record<string, unknown>,
    action: (cmdCtx: CommandContext) => void | Promise<void>,
  ): Promise<number> {
    const isQuiet = !!options.quiet;
    const isJson = !!options.json;
    const isVerbose = !!options.verbose;

    const logger = new Logger({ verbose: isVerbose, json: isJson, quiet: isQuiet });
    const requestId = crypto.randomUUID();
    const correlationId = (options.correlationId as string) || crypto.randomUUID();

    const verbosity = isQuiet ? 'quiet' : isVerbose ? 'verbose' : 'normal';

    const ctx: ExecutionContext = {
      commandName,
      arguments: args,
      options,
      workingDir: process.cwd(),
      env: process.env,
      requestId,
      correlationId,
      outputMode: isJson ? 'json' : 'text',
      verbosity,
      logger,
      timestamp: new Date(),
    };

    const cmdCtx = createCommandContext(configService, commandEventPublisher, ctx);

    try {
      await this.pipeline.run(ctx, async () => {
        await action(cmdCtx);
      });
      return 0;
    } catch (error: unknown) {
      const exitCode = error instanceof CliError ? error.exitCode : 1;
      setExitCode(exitCode);
      commandEventPublisher.publish({
        type: 'ProjectCommandFailed',
        timestamp: new Date(),
        payload: {
          commandName,
          error: error instanceof Error ? error.message : String(error),
        },
      });
      return exitCode;
    }
  }
}
export const dispatcher = new CommandDispatcher();
