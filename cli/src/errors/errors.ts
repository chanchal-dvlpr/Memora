/**
 * Centralized list of standard CLI status exit codes.
 */
export enum CliExitCode {
  SUCCESS = 0,
  GENERAL_FAILURE = 1,
  VALIDATION_FAILURE = 2,
  CONFIGURATION_ERROR = 3,
  COMMAND_ERROR = 4,
  PERMISSION_ERROR = 5,
  USAGE_ERROR = 6,
  INTERNAL_ERROR = 7,
}

/**
 * Base abstract class for standard CLI exceptions.
 */
export abstract class CliError extends Error {
  abstract readonly exitCode: CliExitCode;

  constructor(message: string) {
    super(message);
    this.name = 'CliError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

export class ValidationError extends CliError {
  readonly exitCode = CliExitCode.VALIDATION_FAILURE;
  constructor(message: string) {
    super(message);
    this.name = 'ValidationError';
  }
}

export class ConfigurationError extends CliError {
  readonly exitCode = CliExitCode.CONFIGURATION_ERROR;
  constructor(message: string) {
    super(message);
    this.name = 'ConfigurationError';
  }
}

export class FilesystemError extends CliError {
  readonly exitCode = CliExitCode.PERMISSION_ERROR;
  constructor(message: string) {
    super(message);
    this.name = 'FilesystemError';
  }
}

export class CommandError extends CliError {
  readonly exitCode = CliExitCode.COMMAND_ERROR;
  constructor(message: string) {
    super(message);
    this.name = 'CommandError';
  }
}

export class UsageError extends CliError {
  readonly exitCode = CliExitCode.USAGE_ERROR;
  constructor(message: string) {
    super(message);
    this.name = 'UsageError';
  }
}

export class InternalError extends CliError {
  readonly exitCode = CliExitCode.INTERNAL_ERROR;
  constructor(message: string) {
    super(message);
    this.name = 'InternalError';
  }
}

export class HttpError extends CliError {
  readonly exitCode = CliExitCode.COMMAND_ERROR;
  constructor(
    message: string,
    public readonly statusCode: number,
    public readonly method: string,
    public readonly url: string,
    public readonly body?: unknown,
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

