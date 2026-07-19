import { ValidationError } from '../errors/errors';

/**
 * Validates that knowledge ID is a non-empty alphanumeric or dash string.
 */
export function validateKnowledgeId(id: string): void {
  if (!id || typeof id !== 'string' || id.trim() === '') {
    throw new ValidationError('Knowledge ID must be a non-empty string.');
  }
  if (!/^[a-zA-Z0-9-_]+$/.test(id)) {
    throw new ValidationError('Knowledge ID contains invalid characters.');
  }
}

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
 * Validates that search query is non-empty.
 */
export function validateSearchQuery(query: string): void {
  if (!query || typeof query !== 'string' || query.trim() === '') {
    throw new ValidationError('Search query must be a non-empty string.');
  }
}

/**
 * Validates backend responses to ensure structure matches expectation.
 */
export function validateApiResponse(data: unknown): void {
  if (!data || typeof data !== 'object') {
    throw new ValidationError('Invalid response from backend server.');
  }
}
