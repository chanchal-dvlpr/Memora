import { AppConfig } from './schema';
import { validateConfig } from './validator';
import { CliConfigSource } from './source';

/**
 * Deep freezes an object to enforce CLI configuration immutability.
 */
function deepFreeze<T extends object>(obj: T): T {
  const propNames = Reflect.ownKeys(obj);
  for (const name of propNames) {
    const value = Reflect.get(obj, name) as unknown;
    if (value && typeof value === 'object') {
      deepFreeze(value);
    }
  }
  return Object.freeze(obj);
}

/**
 * Pure orchestration layer that merges settings from independent abstract sources.
 */
export class ConfigLoader {
  constructor(private sources: CliConfigSource[]) {}

  /**
   * Executes sources in order, deep-merges keys, and validates output.
   */
  public async load(cliOptions: Record<string, unknown> = {}): Promise<AppConfig> {
    let config: Record<string, unknown> = {};

    for (const source of this.sources) {
      const data = await source.load(cliOptions);
      config = this.deepMerge(config, data as Record<string, unknown>);
    }

    validateConfig(config);
    return deepFreeze(config as AppConfig);
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
