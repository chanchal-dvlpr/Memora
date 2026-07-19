export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

/**
 * Strongly typed read-only schema representation for Memora CLI configurations.
 */
export interface AppConfig {
  readonly backend: {
    readonly url: string;
    readonly timeoutMs: number;
    readonly retryCount: number;
  };
  readonly logging: {
    readonly level: 'TRACE' | 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  };
  readonly output: {
    readonly mode: 'text' | 'json';
  };
  readonly workspace: {
    readonly path: string;
  };
}

/**
 * Sensible default configuration parameters.
 */
export const DEFAULT_CONFIG: AppConfig = {
  backend: {
    url: 'http://localhost:8080',
    timeoutMs: 2000,
    retryCount: 3,
  },
  logging: {
    level: 'INFO',
  },
  output: {
    mode: 'text',
  },
  workspace: {
    path: process.cwd(),
  },
};
