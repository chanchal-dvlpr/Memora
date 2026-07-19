import { EventEmitter } from 'events';
import { AppConfig } from './schema';

/**
 * Strongly typed configurations state change event structures.
 */
export type ConfigEvent =
  | { readonly type: 'ConfigurationLoaded'; readonly config: AppConfig }
  | { readonly type: 'ConfigurationReloaded'; readonly config: AppConfig }
  | { readonly type: 'ConfigurationUpdated'; readonly config: AppConfig }
  | { readonly type: 'ConfigurationReset' }
  | { readonly type: 'ConfigurationValidationFailed'; readonly error: Error }
  | { readonly type: 'ConfigurationCacheInvalidated' };

export type ConfigEventListener = (event: ConfigEvent) => void | Promise<void>;

/**
 * Event broker for internal configuration state transitions.
 */
export class ConfigEventEmitter {
  private emitter = new EventEmitter();

  public subscribe(listener: ConfigEventListener): void {
    this.emitter.on('change', listener);
  }

  public unsubscribe(listener: ConfigEventListener): void {
    this.emitter.off('change', listener);
  }

  public emit(event: ConfigEvent): void {
    this.emitter.emit('change', event);
  }
}
