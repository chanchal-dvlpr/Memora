import * as fs from 'fs';
import * as path from 'path';
import { AppConfig } from './schema';
import { ConfigurationError } from '../errors/errors';

/**
 * Validates untrusted configuration structures, asserting matching properties types
 * and enum definitions. Throws ConfigurationError on failures.
 */
export function validateConfig(config: unknown): asserts config is AppConfig {
  if (typeof config !== 'object' || config === null) {
    throw new ConfigurationError('Configuration must be a valid JSON object.');
  }

  const obj = config as Record<string, unknown>;

  // Assert unknown sections
  const allowedSections = ['backend', 'logging', 'output', 'workspace'];
  for (const key of Object.keys(obj)) {
    if (!allowedSections.includes(key)) {
      throw new ConfigurationError(`Unknown configuration section: "${key}".`);
    }
  }

  // Validate backend section
  if (obj.backend !== undefined) {
    if (typeof obj.backend !== 'object' || obj.backend === null) {
      throw new ConfigurationError('Backend configuration must be an object.');
    }
    const backend = obj.backend as Record<string, unknown>;
    const allowedBackendKeys = ['url', 'timeoutMs', 'retryCount'];
    for (const key of Object.keys(backend)) {
      if (!allowedBackendKeys.includes(key)) {
        throw new ConfigurationError(`Unknown property inside backend config: "${key}".`);
      }
    }

    if (backend.url !== undefined) {
      if (typeof backend.url !== 'string') {
        throw new ConfigurationError('Backend URL must be a string.');
      }
      try {
        new URL(backend.url);
      } catch (err) {
        throw new ConfigurationError(`Invalid backend URL: "${backend.url}".`);
      }
    }

    if (backend.timeoutMs !== undefined) {
      if (
        typeof backend.timeoutMs !== 'number' ||
        backend.timeoutMs <= 0 ||
        !Number.isInteger(backend.timeoutMs)
      ) {
        throw new ConfigurationError('Backend timeoutMs must be a positive integer.');
      }
    }

    if (backend.retryCount !== undefined) {
      if (
        typeof backend.retryCount !== 'number' ||
        backend.retryCount < 0 ||
        !Number.isInteger(backend.retryCount)
      ) {
        throw new ConfigurationError('Backend retryCount must be a non-negative integer.');
      }
    }
  }

  // Validate logging section
  if (obj.logging !== undefined) {
    if (typeof obj.logging !== 'object' || obj.logging === null) {
      throw new ConfigurationError('Logging configuration must be an object.');
    }
    const logging = obj.logging as Record<string, unknown>;
    for (const key of Object.keys(logging)) {
      if (key !== 'level') {
        throw new ConfigurationError(`Unknown property inside logging config: "${key}".`);
      }
    }
    if (logging.level !== undefined) {
      const allowedLevels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];
      if (typeof logging.level !== 'string' || !allowedLevels.includes(logging.level)) {
        throw new ConfigurationError(
          `Invalid logging level: "${logging.level}". Allowed values: TRACE, DEBUG, INFO, WARN, ERROR.`,
        );
      }
    }
  }

  // Validate output section
  if (obj.output !== undefined) {
    if (typeof obj.output !== 'object' || obj.output === null) {
      throw new ConfigurationError('Output configuration must be an object.');
    }
    const output = obj.output as Record<string, unknown>;
    for (const key of Object.keys(output)) {
      if (key !== 'mode') {
        throw new ConfigurationError(`Unknown property inside output config: "${key}".`);
      }
    }
    if (output.mode !== undefined) {
      const allowedModes = ['text', 'json'];
      if (typeof output.mode !== 'string' || !allowedModes.includes(output.mode)) {
        throw new ConfigurationError(
          `Invalid output mode: "${output.mode}". Allowed values: text, json.`,
        );
      }
    }
  }

  // Validate workspace section
  if (obj.workspace !== undefined) {
    if (typeof obj.workspace !== 'object' || obj.workspace === null) {
      throw new ConfigurationError('Workspace configuration must be an object.');
    }
    const workspace = obj.workspace as Record<string, unknown>;
    for (const key of Object.keys(workspace)) {
      if (key !== 'path') {
        throw new ConfigurationError(`Unknown property inside workspace config: "${key}".`);
      }
    }
    if (workspace.path !== undefined) {
      if (typeof workspace.path !== 'string') {
        throw new ConfigurationError('Workspace path must be a string.');
      }
      const resolvedPath = path.resolve(workspace.path);
      if (!fs.existsSync(resolvedPath)) {
        throw new ConfigurationError(`Workspace path "${workspace.path}" does not exist.`);
      }
      if (!fs.statSync(resolvedPath).isDirectory()) {
        throw new ConfigurationError(`Workspace path "${workspace.path}" is not a directory.`);
      }
    }
  }
}
