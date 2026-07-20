import { SecurityContext } from './security';
import { SessionContext } from './session';

export enum ToolCategory {
  SYSTEM = 'system',
  PROJECT = 'project',
  SEARCH = 'search',
  UTILITY = 'utility',
}

export enum ToolVisibility {
  PUBLIC = 'public',
  INTERNAL = 'internal',
}

export interface ToolInputSchema {
  type?: 'object';
  properties?: Record<string, unknown>;
  required?: string[];
}

export interface ToolOutputSchema {
  type: 'object';
  properties?: Record<string, unknown>;
}

export interface ToolDefinition {
  name: string;
  description?: string;
  inputSchema: ToolInputSchema;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  handler: (params: Record<string, unknown>) => Promise<any>;
  requiredPermission?: string;
}

export interface ToolMetadata {
  displayName?: string;
  description?: string;
  longDescription?: string;
  version?: string;
  author?: string;
  categories: ToolCategory[];
  tags?: string[];
  examples?: Array<{ input: Record<string, unknown>; output: string }>;
  annotations?: Record<string, string>;
  deprecationFlag?: boolean;
  visibility: ToolVisibility;
  experimentalFlag?: boolean;
}

export interface ToolExecutionContext {
  requestId: string;
  sessionId: string;
  protocolVersion: string;
  timestamp: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  logger: any;
  params: Record<string, unknown>;
  cancellationToken?: { isCancelled: boolean };
  metadata: Map<string, unknown>;
  securityContext?: SecurityContext;
  sessionContext?: SessionContext;
}

export interface ToolExecutionResult {
  content: Array<{
    type: string;
    text: string;
  }>;
  isError?: boolean;
}
