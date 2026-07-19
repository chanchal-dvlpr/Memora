import {
  CliError,
  HttpError,
  ValidationError,
  CommandError,
  InternalError,
} from '../errors/errors';

/**
 * Maps standard HTTP failure status codes into corresponding CLI exceptions.
 */
export function mapHttpError(
  status: number,
  method: string,
  url: string,
  responseBody?: unknown,
): CliError {
  const bodyText =
    responseBody && typeof responseBody === 'object'
      ? JSON.stringify(responseBody)
      : String(responseBody || '');
  const prefix = `HTTP ${method} ${url} failed with status ${status}: `;

  switch (status) {
    case 400:
      return new CommandError(`${prefix}Bad request. ${bodyText}`);
    case 401:
    case 403:
      return new CommandError(`${prefix}Unauthorized access. ${bodyText}`);
    case 404:
      return new CommandError(`${prefix}Resource not found. ${bodyText}`);
    case 409:
      return new CommandError(`${prefix}Conflict occurred. ${bodyText}`);
    case 422:
      return new ValidationError(`${prefix}Validation failed. ${bodyText}`);
    case 429:
      return new CommandError(`${prefix}Rate limit exceeded. ${bodyText}`);
    case 500:
      return new InternalError(`${prefix}Internal server error. ${bodyText}`);
    case 502:
    case 503:
    case 504:
      return new CommandError(`${prefix}Service unavailable / Gateway timeout. ${bodyText}`);
    default:
      return new HttpError(
        `${prefix}Unexpected response code.`,
        status,
        method,
        url,
        responseBody,
      );
  }
}

/**
 * Translates connection drops, timeout bounds, and address lookups errors.
 */
export function mapNetworkError(err: unknown, url: string): CliError {
  const msg = err instanceof Error ? err.message : String(err);
  if (msg.includes('ENOTFOUND') || msg.includes('getaddrinfo') || msg.includes('dns')) {
    return new CommandError(`Network error: DNS lookup failed for ${url}`);
  }
  if (msg.includes('ECONNREFUSED') || msg.includes('connect ECONNREFUSED')) {
    return new CommandError(`Network error: Connection refused at host ${url}`);
  }
  return new CommandError(`Network error: ${msg}`);
}
export default mapHttpError;
