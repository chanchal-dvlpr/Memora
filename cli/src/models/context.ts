import { Logger } from '../logger/logger';

/**
 * Standard execution context schema propagated across middleware pipeline actions.
 */
export interface ExecutionContext {
  commandName: string;
  arguments: string[];
  options: Record<string, unknown>;
  workingDir: string;
  env: Record<string, string | undefined>;
  requestId: string;
  correlationId: string;
  outputMode: 'text' | 'json';
  verbosity: 'normal' | 'verbose' | 'quiet';
  logger: Logger;
  timestamp: Date;
}
