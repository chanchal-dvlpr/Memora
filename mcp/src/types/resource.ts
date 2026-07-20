import { SecurityContext } from './security';
import { SessionContext } from './session';

export enum ResourceCategory {
  SYSTEM = 'system',
  PROJECT = 'project',
  SEARCH = 'search',
  UTILITY = 'utility',
}

export enum ResourceVisibility {
  PUBLIC = 'public',
  INTERNAL = 'internal',
}

export interface ResourceContents {
  uri: string;
  mimeType?: string;
  text?: string;
  blob?: string;
}

export interface ResourceExecutionContext {
  requestId: string;
  sessionId: string;
  protocolVersion: string;
  timestamp: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  logger: any;
  params: Record<string, unknown>;
  metadata: Map<string, unknown>;
  cancellationToken?: { isCancelled: boolean };
  securityContext?: SecurityContext;
  sessionContext?: SessionContext;
}

export interface ResourceDefinition {
  uri: string;
  name: string;
  description?: string;
  mimeType?: string;
  handler: (
    params: Record<string, unknown>,
    context: ResourceExecutionContext
  ) => Promise<ResourceContents[]>;
  requiredPermission?: string;
}

export interface ResourceMetadata {
  displayName?: string;
  description?: string;
  longDescription?: string;
  version?: string;
  author?: string;
  category?: ResourceCategory;
  categories: ResourceCategory[];
  tags?: string[];
  annotations?: Record<string, string>;
  examples?: Array<{ uri: string; output: string }>;
  mimeType?: string;
  visibility: ResourceVisibility;
  experimentalFlag?: boolean;
  deprecationFlag?: boolean;
}

export interface ResourceReadRequest {
  uri: string;
}

export interface ResourceReadResult {
  contents: ResourceContents[];
}

// --- Caching Abstraction ---

export interface CacheEntry<T = unknown> {
  readonly key: string;
  readonly data: T;
  readonly createdAt: number;
  readonly expiresAt?: number;
  readonly etag?: string;
}

export interface CachePolicy {
  readonly ttlMs?: number;
  readonly maxSize?: number;
  readonly useEtag?: boolean;
}

export interface ResourceCache<T = unknown> {
  lookup(key: string): Promise<CacheEntry<T> | undefined>;
  store(key: string, data: T, policy?: CachePolicy): Promise<void>;
  invalidate(key: string): Promise<void>;
  clear(): Promise<void>;
}

/**
 * Deep freezes an object to enforce resource metadata and policy immutability.
 */
export function deepFreeze<T extends object>(obj: T): T {
  const propNames = Reflect.ownKeys(obj);
  for (const name of propNames) {
    const value = Reflect.get(obj, name) as unknown;
    if (value && typeof value === 'object') {
      deepFreeze(value as object);
    }
  }
  return Object.freeze(obj);
}
