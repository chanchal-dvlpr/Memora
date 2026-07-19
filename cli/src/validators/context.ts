import { ValidationError } from '../errors/errors';

/**
 * Validates that project ID is a non-empty alphanumeric or dash string.
 */
export function validateProjectId(projectId: string): void {
  if (!projectId || typeof projectId !== 'string' || projectId.trim() === '') {
    throw new ValidationError('Project ID must be a non-empty string.');
  }
  if (!/^[a-zA-Z0-9-_]+$/.test(projectId)) {
    throw new ValidationError('Project ID contains invalid characters.');
  }
}

/**
 * Validates export formats.
 */
export function validateFormat(format: string): void {
  const normalized = format.toLowerCase();
  if (normalized !== 'markdown' && normalized !== 'json') {
    throw new ValidationError(`Unsupported export format: ${format}`);
  }
}

/**
 * Validates backend JSON DTO responses to ensure structure matches expectation.
 */
export function validateApiResponse(data: unknown): void {
  if (!data || typeof data !== 'object') {
    throw new ValidationError('Invalid response from backend server.');
  }
  const obj = data as Record<string, unknown>;
  if (typeof obj.projectId !== 'string' || obj.projectId.trim() === '') {
    throw new ValidationError('Backend response is missing required field: projectId.');
  }
  if (typeof obj.content !== 'string') {
    throw new ValidationError('Backend response is missing required field: content.');
  }
}
