import { ResourceValidationError, ResourceOutputValidationError } from '../errors';

const SUPPORTED_MIME_TYPES = new Set([
  'text/plain',
  'text/markdown',
  'application/json',
  'application/yaml',
  'text/html',
  'application/octet-stream',
]);

const KNOWN_SCHEMES = new Set(['memora', 'http', 'https', 'file', 'ftp', 'ws', 'wss']);

export function normalizeUri(uri: string): string {
  const normalized = uri.trim();
  const regex = /^([a-zA-Z][a-zA-Z0-9+.-]*):\/\/([^/?#]*)([^?#]*)(?:\?([^#]*))?(?:#(.*))?$/;
  const match = normalized.match(regex);
  if (match) {
    const scheme = match[1].toLowerCase();
    const authority = KNOWN_SCHEMES.has(scheme) ? match[2].toLowerCase() : match[2];
    const path = match[3];
    const query = match[4];
    const fragment = match[5];

    let queryString = '';
    if (query) {
      const params = new URLSearchParams(query);
      params.sort();
      queryString = '?' + params.toString();
    }

    let fragmentString = '';
    if (fragment && fragment !== '#') {
      fragmentString = '#' + fragment;
    }

    return `${scheme}://${authority}${path}${queryString}${fragmentString}`;
  }
  return normalized;
}

/**
 * Checks if a given object contains circular references.
 */
export function hasCircularReference(obj: unknown): boolean {
  const seen = new WeakSet();
  function detect(value: unknown): boolean {
    if (value && typeof value === 'object') {
      if (seen.has(value)) {
        return true;
      }
      seen.add(value);
      for (const key of Object.keys(value)) {
        if (detect((value as Record<string, unknown>)[key])) {
          return true;
        }
      }
      seen.delete(value);
    }
    return false;
  }
  return detect(obj);
}

export class ResourceValidator {
  /**
   * Validates URI structure and normalized representation.
   */
  public validateUri(uri: string): string {
    if (!uri || typeof uri !== 'string') {
      throw new ResourceValidationError('Resource URI cannot be empty.');
    }

    const uriRegex = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\/.+/;
    if (!uriRegex.test(uri)) {
      throw new ResourceValidationError(`Invalid resource URI format: "${uri}"`);
    }

    try {
      const parsed = new URL(uri);
      if (!parsed.protocol) {
        throw new ResourceValidationError(`URI protocol is required: "${uri}"`);
      }
      return normalizeUri(uri);
    } catch (err) {
      throw new ResourceValidationError(
        `Malformed URI: "${uri}". Error: ${err instanceof Error ? err.message : String(err)}`
      );
    }
  }

  /**
   * Validates if a MIME type is supported by the framework.
   */
  public validateMimeType(mimeType: string | undefined): void {
    if (mimeType !== undefined && !SUPPORTED_MIME_TYPES.has(mimeType)) {
      throw new ResourceValidationError(`Unsupported MIME type: "${mimeType}"`);
    }
  }

  /**
   * Validates that the handler execution result matches ResourceContents specifications.
   */
  public validateContents(contents: unknown, expectedUri: string): void {
    if (hasCircularReference(contents)) {
      throw new ResourceOutputValidationError('Resource handler result contains circular references.');
    }

    if (!Array.isArray(contents)) {
      throw new ResourceOutputValidationError('Resource handler must return an array of contents.');
    }

    if (contents.length === 0) {
      throw new ResourceOutputValidationError('Resource handler returned an empty contents array.');
    }

    const canonicalExpectedUri = normalizeUri(expectedUri);

    for (const item of contents) {
      if (!item || typeof item !== 'object') {
        throw new ResourceOutputValidationError('Resource contents item must be a valid object.');
      }

      const contentObj = item as Record<string, unknown>;

      if (typeof contentObj.uri !== 'string' || !contentObj.uri) {
        throw new ResourceOutputValidationError('Each resource contents item must have a valid non-empty "uri" string.');
      }

      const canonicalItemUri = normalizeUri(contentObj.uri);
      if (canonicalItemUri !== canonicalExpectedUri) {
        throw new ResourceOutputValidationError(
          `Resource contents URI mismatch. Expected "${canonicalExpectedUri}", but handler returned "${canonicalItemUri}".`
        );
      }

      const mimeType = contentObj.mimeType as string | undefined;
      this.validateMimeType(mimeType);

      const hasText = typeof contentObj.text === 'string';
      const hasBlob = typeof contentObj.blob === 'string';

      if (!hasText && !hasBlob) {
        throw new ResourceOutputValidationError(
          'Each resource contents item must contain either a "text" string or a "blob" base64 string.'
        );
      }

      // MIME-specific serialization compatibility checks
      if (mimeType === 'application/json' && hasText) {
        try {
          JSON.parse(contentObj.text as string);
        } catch (err) {
          throw new ResourceOutputValidationError('Resource content with application/json MIME type is not valid JSON.');
        }
      }
    }
  }
}
