export type ExportDestination =
  | 'stdout'
  | 'stderr'
  | { readonly type: 'file'; readonly path: string }
  | { readonly type: 'clipboard' }
  | { readonly type: 'html'; readonly path: string }
  | { readonly type: 'pdf'; readonly path: string }
  | { readonly type: 'csv'; readonly path: string };

export interface RenderRequest {
  readonly mode: 'pretty' | 'json' | 'markdown' | 'table';
  readonly data: unknown;
  readonly destination: ExportDestination;
  readonly options?: {
    readonly theme?: string;
    readonly colors?: boolean;
  };
  readonly metadata?: Record<string, unknown>;
}

export interface RenderResult {
  readonly output: string;
  readonly request: RenderRequest;
  readonly metadata: {
    readonly timestamp: Date;
    readonly format: string;
    readonly length: number;
  };
}

/**
 * Deep freezes an object recursively to guarantee absolute immutability.
 */
export function deepFreeze<T>(obj: T): T {
  if (obj === null || obj === undefined) {
    return obj;
  }
  Object.freeze(obj);
  Object.getOwnPropertyNames(obj).forEach((prop) => {
    const value = (obj as Record<string, unknown>)[prop];
    if (
      value !== null &&
      (typeof value === 'object' || typeof value === 'function') &&
      !Object.isFrozen(value)
    ) {
      deepFreeze(value);
    }
  });
  return obj;
}
