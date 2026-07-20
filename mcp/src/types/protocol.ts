import { JsonRpcRequest, JsonRpcSuccessResponse } from './jsonrpc';

export type ProtocolVersion = '2024-11-05';

export interface ClientCapabilities {
  experimental?: Record<string, Record<string, unknown>>;
  sampling?: Record<string, unknown>;
  roots?: {
    listChanged?: boolean;
  };
}

export interface ServerCapabilities {
  experimental?: Record<string, Record<string, unknown>>;
  logging?: Record<string, unknown>;
  prompts?: {
    listChanged?: boolean;
  };
  resources?: {
    subscribe?: boolean;
    listChanged?: boolean;
  };
  tools?: {
    listChanged?: boolean;
  };
}

export interface ImplementationInfo {
  name: string;
  version: string;
}

export interface InitializeParams {
  protocolVersion: ProtocolVersion | string;
  capabilities: ClientCapabilities;
  clientInfo: ImplementationInfo;
}

export interface InitializeResult {
  protocolVersion: ProtocolVersion | string;
  capabilities: ServerCapabilities;
  serverInfo: ImplementationInfo;
}

export interface InitializeRequest extends JsonRpcRequest {
  method: 'initialize';
  params: InitializeParams;
}

export interface InitializeResponse extends JsonRpcSuccessResponse {
  result: InitializeResult;
}

export interface RequestContext {
  transportType: string;
  startTime: number;
  metadata?: Record<string, unknown>;
}

export interface ResponseContext {
  durationMs: number;
  metadata?: Record<string, unknown>;
}

export interface ProtocolMetadata {
  sequenceId: number;
  clientInfo?: ImplementationInfo;
}
