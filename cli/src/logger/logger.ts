export enum LogLevel {
  TRACE = 0,
  DEBUG = 1,
  INFO = 2,
  WARN = 3,
  ERROR = 4,
}

/**
 * Centered logging class separating info logging from stdout commands payloads.
 */
export class Logger {
  private level: LogLevel = LogLevel.INFO;
  private isJson = false;
  private isQuiet = false;

  constructor(options: { verbose?: boolean; json?: boolean; quiet?: boolean } = {}) {
    if (options.quiet) {
      this.isQuiet = true;
    }
    if (options.verbose) {
      this.level = LogLevel.TRACE;
    }
    if (options.json) {
      this.isJson = true;
    }
  }

  private log(
    level: LogLevel,
    levelStr: string,
    message: string,
    meta?: Record<string, unknown>,
  ): void {
    if (this.isQuiet) return;
    if (level < this.level) return;

    const timestamp = new Date().toISOString();

    if (this.isJson) {
      const payload = {
        timestamp,
        level: levelStr,
        message,
        ...meta,
      };
      process.stderr.write(JSON.stringify(payload) + '\n');
    } else {
      let colorPrefix = '';
      const colorSuffix = '\x1b[0m';
      if (level === LogLevel.ERROR) {
        colorPrefix = '\x1b[1;31m[ERROR] ';
      } else if (level === LogLevel.WARN) {
        colorPrefix = '\x1b[1;33m[WARN] ';
      } else if (level === LogLevel.DEBUG) {
        colorPrefix = '\x1b[90m[DEBUG] ';
      } else if (level === LogLevel.TRACE) {
        colorPrefix = '\x1b[90m[TRACE] ';
      } else {
        colorPrefix = '\x1b[32m[INFO] ';
      }
      process.stderr.write(`${colorPrefix}${message}${colorSuffix}\n`);
    }
  }

  public trace(message: string, meta?: Record<string, unknown>): void {
    this.log(LogLevel.TRACE, 'TRACE', message, meta);
  }

  public debug(message: string, meta?: Record<string, unknown>): void {
    this.log(LogLevel.DEBUG, 'DEBUG', message, meta);
  }

  public info(message: string, meta?: Record<string, unknown>): void {
    this.log(LogLevel.INFO, 'INFO', message, meta);
  }

  public warn(message: string, meta?: Record<string, unknown>): void {
    this.log(LogLevel.WARN, 'WARN', message, meta);
  }

  public error(message: string, meta?: Record<string, unknown>): void {
    this.log(LogLevel.ERROR, 'ERROR', message, meta);
  }
}
