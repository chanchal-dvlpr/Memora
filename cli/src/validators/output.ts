import * as fs from 'fs';
import * as path from 'path';
import { ValidationError } from '../errors/errors';
import { RenderRequest } from '../output/types';

/**
 * Validates a RenderRequest before execution.
 */
export function validateRenderRequest(request: RenderRequest): void {
  if (!request) {
    throw new ValidationError('RenderRequest is null or undefined');
  }

  // 1. Validate Mode
  const validModes = ['pretty', 'json', 'markdown', 'table'];
  if (!validModes.includes(request.mode)) {
    throw new ValidationError(`Unsupported renderer mode: ${request.mode}`);
  }

  // 2. Validate Data Model
  if (request.data === undefined) {
    throw new ValidationError('RenderRequest data is undefined');
  }

  // 3. Validate Mode compatibility with Data
  if (request.mode === 'table') {
    const data = request.data as Record<string, unknown>;
    if (!data || typeof data !== 'object' || !Array.isArray(data.columns) || !Array.isArray(data.rows)) {
      throw new ValidationError('Table renderer requires data with columns and rows arrays');
    }
  }

  // 4. Validate Destination
  const dest = request.destination;
  if (!dest) {
    throw new ValidationError('RenderRequest destination is undefined');
  }

  if (typeof dest === 'string') {
    if (dest !== 'stdout' && dest !== 'stderr') {
      throw new ValidationError(`Unsupported destination: ${dest}`);
    }
  } else if (typeof dest === 'object') {
    const type = dest.type;
    const allowedTypes = ['file', 'clipboard', 'html', 'pdf', 'csv'];
    if (!allowedTypes.includes(type)) {
      throw new ValidationError(`Unsupported export destination type: ${type}`);
    }

    if (type === 'file' || type === 'html' || type === 'pdf' || type === 'csv') {
      const destPath = (dest as Record<string, unknown>).path;
      if (!destPath || typeof destPath !== 'string') {
        throw new ValidationError(`Path is required for ${type} destination`);
      }
      // Check if parent directory exists and is writeable
      const resolved = path.resolve(destPath);
      const parentDir = path.dirname(resolved);
      if (!fs.existsSync(parentDir)) {
        throw new ValidationError(`Parent directory does not exist: ${parentDir}`);
      }
      try {
        fs.accessSync(parentDir, fs.constants.W_OK);
      } catch (err) {
        throw new ValidationError(`Parent directory is not writeable: ${parentDir}`);
      }
    }
  } else {
    throw new ValidationError('Invalid destination type');
  }
}
