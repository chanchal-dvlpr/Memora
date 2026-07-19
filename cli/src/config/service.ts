import * as fs from 'fs';
import { AppConfig, DEFAULT_CONFIG, DeepPartial } from './schema';
import { ConfigLoader } from './loader';
import { getResolvedConfigPath, saveConfigFile } from './storage';
import { validateConfig } from './validator';
import { ConfigurationError } from '../errors/errors';
import { ConfigEventEmitter } from './events';
import {
  DefaultConfigSource,
  FileConfigSource,
  EnvConfigSource,
  CliOptionConfigSource,
} from './source';

/**
 * Centered configuration service acting as the single source of truth for CLI settings.
 */
export class ConfigService {
  public readonly events = new ConfigEventEmitter();
  private cache: AppConfig | null = null;
  private lastCliOptions: Record<string, unknown> = {};

  constructor(private loader: ConfigLoader) {}

  /**
   * Initializes the service, loading configuration sources and populating cache.
   */
  public async load(cliOptions: Record<string, unknown> = {}): Promise<AppConfig> {
    this.lastCliOptions = cliOptions;
    try {
      this.cache = await this.loader.load(cliOptions);
      this.events.emit({ type: 'ConfigurationLoaded', config: this.cache });
      return this.cache;
    } catch (err: unknown) {
      const errorObj = err instanceof Error ? err : new Error(String(err));
      this.events.emit({ type: 'ConfigurationValidationFailed', error: errorObj });
      throw err;
    }
  }

  /**
   * Returns the deep-frozen active configuration cache.
   */
  public getConfig(): AppConfig {
    if (!this.cache) {
      throw new ConfigurationError('Configuration is not loaded. Call load() first.');
    }
    return this.cache;
  }

  /**
   * Invalidates cache and triggers a fresh orchestration reload.
   */
  public async reload(cliOptions: Record<string, unknown> = {}): Promise<AppConfig> {
    this.cache = null;
    try {
      const config = await this.load(cliOptions);
      this.events.emit({ type: 'ConfigurationReloaded', config });
      return config;
    } catch (err: unknown) {
      const errorObj = err instanceof Error ? err : new Error(String(err));
      this.events.emit({ type: 'ConfigurationValidationFailed', error: errorObj });
      throw err;
    }
  }

  /**
   * Performs persistent updates on disk, invalidates in-memory caches,
   * and requires an explicit call to reload().
   */
  public async updateFile(updatedData: DeepPartial<AppConfig>): Promise<void> {
    const configPath = getResolvedConfigPath(this.lastCliOptions);

    let currentFileContent: Record<string, unknown> = {};
    if (fs.existsSync(configPath)) {
      try {
        const fileContent = fs.readFileSync(configPath, 'utf8');
        currentFileContent = JSON.parse(fileContent) as Record<string, unknown>;
      } catch (err) {
        // Fallback to empty if file is corrupt
      }
    }

    const merged = this.deepMerge(currentFileContent, updatedData as Record<string, unknown>);
    try {
      validateConfig(merged);
    } catch (err: unknown) {
      const errorObj = err instanceof Error ? err : new Error(String(err));
      this.events.emit({ type: 'ConfigurationValidationFailed', error: errorObj });
      throw err;
    }

    saveConfigFile(configPath, JSON.stringify(merged, null, 2));
    this.events.emit({ type: 'ConfigurationUpdated', config: merged as AppConfig });

    this.cache = null;
    this.events.emit({ type: 'ConfigurationCacheInvalidated' });
  }

  /**
   * Resets settings on disk back to default parameters, invalidating cache.
   */
  public async reset(): Promise<void> {
    const configPath = getResolvedConfigPath(this.lastCliOptions);
    saveConfigFile(configPath, JSON.stringify(DEFAULT_CONFIG, null, 2));
    this.events.emit({ type: 'ConfigurationReset' });

    this.cache = null;
    this.events.emit({ type: 'ConfigurationCacheInvalidated' });
  }

  /**
   * Performs deep merging on configuration trees.
   */
  private deepMerge(
    target: Record<string, unknown>,
    source: Record<string, unknown>,
  ): Record<string, unknown> {
    const output = { ...target };
    for (const key of Object.keys(source)) {
      const sourceVal = source[key];
      const targetVal = target[key];

      if (sourceVal && typeof sourceVal === 'object' && !Array.isArray(sourceVal)) {
        output[key] = this.deepMerge(
          targetVal && typeof targetVal === 'object' ? (targetVal as Record<string, unknown>) : {},
          sourceVal as Record<string, unknown>,
        );
      } else if (sourceVal !== undefined) {
        output[key] = sourceVal;
      }
    }
    return output;
  }
}

// Export default singleton instance configured with standard precedence sources
export const configService = new ConfigService(
  new ConfigLoader([
    new DefaultConfigSource(),
    new FileConfigSource(),
    new EnvConfigSource(),
    new CliOptionConfigSource(),
  ]),
);
export default configService;
