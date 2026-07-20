export enum LogLevel {
  TRACE = 0,
  DEBUG = 1,
  INFO = 2,
  WARN = 3,
  ERROR = 4,
  FATAL = 5,
}

export interface LogRecord {
  timestamp: string;
  level: string;
  component: string;
  message: string;
  metadata?: Record<string, unknown>;
  stack?: string;
}

export class StructuredLogger {
  private readonly component: string;
  private readonly threshold: LogLevel;

  private static readonly LEVEL_NAMES: Record<LogLevel, string> = {
    [LogLevel.TRACE]: 'TRACE',
    [LogLevel.DEBUG]: 'DEBUG',
    [LogLevel.INFO]: 'INFO',
    [LogLevel.WARN]: 'WARN',
    [LogLevel.ERROR]: 'ERROR',
    [LogLevel.FATAL]: 'FATAL',
  };

  private static readonly STRING_TO_LEVEL: Record<string, LogLevel> = {
    trace: LogLevel.TRACE,
    debug: LogLevel.DEBUG,
    info: LogLevel.INFO,
    warn: LogLevel.WARN,
    error: LogLevel.ERROR,
    fatal: LogLevel.FATAL,
  };

  constructor(component: string, levelName: string = 'info') {
    this.component = component;
    const normalized = levelName.toLowerCase();
    this.threshold = StructuredLogger.STRING_TO_LEVEL[normalized] !== undefined
      ? StructuredLogger.STRING_TO_LEVEL[normalized]
      : LogLevel.INFO;
  }

  private shouldLog(level: LogLevel): boolean {
    return level >= this.threshold;
  }

  private write(level: LogLevel, message: string, metadata?: Record<string, unknown>, error?: Error): void {
    if (!this.shouldLog(level)) {
      return;
    }

    const record: LogRecord = {
      timestamp: new Date().toISOString(),
      level: StructuredLogger.LEVEL_NAMES[level],
      component: this.component,
      message,
    };

    if (metadata && Object.keys(metadata).length > 0) {
      record.metadata = metadata;
    }

    if (error) {
      record.stack = error.stack;
      // Merge error message if no custom message was provided
      if (!message && error.message) {
        record.message = error.message;
      }
    }

    // CRITICAL: Write all MCP server logs to stderr (console.error)
    // stdio transport reserves stdout for JSON-RPC framing.
    console.error(JSON.stringify(record));
  }

  public trace(message: string, metadata?: Record<string, unknown>): void {
    this.write(LogLevel.TRACE, message, metadata);
  }

  public debug(message: string, metadata?: Record<string, unknown>): void {
    this.write(LogLevel.DEBUG, message, metadata);
  }

  public info(message: string, metadata?: Record<string, unknown>): void {
    this.write(LogLevel.INFO, message, metadata);
  }

  public warn(message: string, metadata?: Record<string, unknown>): void {
    this.write(LogLevel.WARN, message, metadata);
  }

  public error(message: string, error?: Error, metadata?: Record<string, unknown>): void {
    this.write(LogLevel.ERROR, message, metadata, error);
  }

  public fatal(message: string, error?: Error, metadata?: Record<string, unknown>): void {
    this.write(LogLevel.FATAL, message, metadata, error);
  }
}
