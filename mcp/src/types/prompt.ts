import { SecurityContext } from './security';
import { SessionContext } from './session';

export enum PromptCategory {
  SYSTEM = 'system',
  CODE = 'code',
  WRITING = 'writing',
  QUERY = 'query',
}

export enum PromptVisibility {
  PUBLIC = 'public',
  INTERNAL = 'internal',
}

export enum PromptArgumentType {
  STRING = 'string',
  NUMBER = 'number',
  BOOLEAN = 'boolean',
  ARRAY = 'array',
  OBJECT = 'object',
}

export interface PromptArgument {
  name: string;
  description?: string;
  required?: boolean;
  type?: PromptArgumentType;
  defaultValue?: string;
  enum?: string[];
  pattern?: string; // regex pattern
  minimum?: number;
  maximum?: number;
  minLength?: number;
  maxLength?: number;
  items?: {
    type: PromptArgumentType;
  };
  properties?: Record<string, PromptArgument>;
}

export interface PromptExample {
  arguments: Record<string, string>;
  description?: string;
}

export interface PromptAnnotation {
  stability?: string;
  category?: string;
  [key: string]: string | undefined;
}

export interface PromptMetadata {
  displayName?: string;
  description?: string;
  longDescription?: string;
  version?: string;
  author?: string;
  category?: string;
  categories?: PromptCategory[];
  tags?: string[];
  examples?: PromptExample[];
  annotations?: PromptAnnotation;
  visibility: PromptVisibility;
  experimentalFlag?: boolean;
  deprecationFlag?: boolean;
}

export type PromptRole = 'user' | 'assistant' | 'system';

export interface TextContent {
  type: 'text';
  text: string;
}

export interface ImageContent {
  type: 'image';
  data: string;
  mimeType: string;
}

export interface EmbeddedResource {
  type: 'resource';
  resource: {
    uri: string;
    mimeType?: string;
    text?: string;
    blob?: string;
  };
}

export interface PromptMessage {
  role: PromptRole;
  content: TextContent | ImageContent | EmbeddedResource;
}

export interface PromptDefinition {
  name: string;
  description?: string;
  arguments?: PromptArgument[];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  handler: (args: Record<string, any>, context: PromptExecutionContext) => Promise<PromptMessage[] | PromptInvocationResult>;
  requiredPermission?: string;
}

export interface CancellationToken {
  readonly isCancellationRequested: boolean;
  onCancellationRequested(callback: () => void): void;
}

export interface PromptExecutionContext {
  requestId: string;
  sessionId: string;
  protocolVersion: string;
  timestamp: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  logger: any;
  params: Record<string, unknown>;
  metadata: Map<string, unknown>;
  cancellationToken?: CancellationToken;
  securityContext?: SecurityContext;
  sessionContext?: SessionContext;
}

export interface PromptInvocationRequest {
  name: string;
  arguments?: Record<string, string>;
}

export interface PromptInvocationResult {
  description?: string;
  messages: PromptMessage[];
}

// --- Prompt Caching Abstractions ---

export interface PromptCacheEntry {
  key: string;
  result: PromptInvocationResult;
  timestamp: number;
  expiresAt?: number;
}

export interface PromptCachePolicy {
  ttl?: number;
  maxEntries?: number;
}

export interface PromptCache {
  lookup(key: string): Promise<PromptCacheEntry | undefined>;
  store(key: string, entry: PromptCacheEntry, policy?: PromptCachePolicy): Promise<void>;
  invalidate(key: string): Promise<void>;
  clear(): Promise<void>;
}
