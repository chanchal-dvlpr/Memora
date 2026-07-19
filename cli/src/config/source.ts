import * as fs from 'fs';
import { AppConfig, DEFAULT_CONFIG, DeepPartial } from './schema';
import { validateConfig } from './validator';
import { ConfigurationError } from '../errors/errors';
import { getResolvedConfigPath, isFileWritableOnlyByOwner } from './storage';

/**
 * Formal contract interface for configuration data providers.
 */
export interface CliConfigSource {
  readonly name: string;
  load(
    cliOptions?: Record<string, unknown>,
  ): DeepPartial<AppConfig> | Promise<DeepPartial<AppConfig>>;
}

/**
 * Default parameters configuration source.
 */
export class DefaultConfigSource implements CliConfigSource {
  readonly name = 'Defaults';
  load(): DeepPartial<AppConfig> {
    return DEFAULT_CONFIG;
  }
}

/**
 * Platform-specific JSON file settings source.
 */
export class FileConfigSource implements CliConfigSource {
  readonly name = 'File';
  load(cliOptions: Record<string, unknown> = {}): DeepPartial<AppConfig> {
    const configPath = getResolvedConfigPath(cliOptions);

    if (fs.existsSync(configPath)) {
      isFileWritableOnlyByOwner(configPath);

      try {
        const fileContent = fs.readFileSync(configPath, 'utf8');
        const parsed = JSON.parse(fileContent) as unknown;

        validateConfig(parsed);
        return parsed as DeepPartial<AppConfig>;
      } catch (err: unknown) {
        if (err instanceof ConfigurationError) {
          throw err;
        }
        const msg = err instanceof Error ? err.message : String(err);
        throw new ConfigurationError(`Failed to load config file at "${configPath}": ${msg}`);
      }
    } else {
      if (process.env.MEMORA_CONFIG || cliOptions.config) {
        throw new ConfigurationError(`Configuration file not found at: "${configPath}"`);
      }
    }
    return {};
  }
}

/**
 * CLI Environment variable overrides source.
 */
export class EnvConfigSource implements CliConfigSource {
  readonly name = 'Environment';
  load(): DeepPartial<AppConfig> {
    const backend: Record<string, unknown> = {};
    const logging: Record<string, unknown> = {};
    const output: Record<string, unknown> = {};
    const workspace: Record<string, unknown> = {};
    const config: Record<string, unknown> = {};

    if (process.env.MEMORA_BACKEND_URL) {
      backend.url = process.env.MEMORA_BACKEND_URL;
      config.backend = backend;
    }
    if (process.env.MEMORA_LOG_LEVEL) {
      logging.level = process.env.MEMORA_LOG_LEVEL;
      config.logging = logging;
    }
    if (process.env.MEMORA_OUTPUT) {
      output.mode = process.env.MEMORA_OUTPUT;
      config.output = output;
    }
    if (process.env.MEMORA_WORKSPACE) {
      workspace.path = process.env.MEMORA_WORKSPACE;
      config.workspace = workspace;
    }

    return config as DeepPartial<AppConfig>;
  }
}

/**
 * Command-line parsed arguments overrides source.
 */
export class CliOptionConfigSource implements CliConfigSource {
  readonly name = 'CliOptions';
  load(cliOptions: Record<string, unknown> = {}): DeepPartial<AppConfig> {
    const logging: Record<string, unknown> = {};
    const output: Record<string, unknown> = {};
    const config: Record<string, unknown> = {};

    if (cliOptions.json) {
      output.mode = 'json';
      config.output = output;
    }
    if (cliOptions.verbose) {
      logging.level = 'TRACE';
      config.logging = logging;
    }
    if (cliOptions.quiet) {
      logging.level = 'ERROR';
      config.logging = logging;
    }

    return config as DeepPartial<AppConfig>;
  }
}
