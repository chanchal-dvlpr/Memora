import { SecurityContext, SecurityAction } from '../../src/types/security';
import { Session } from '../../src/types/session';

export const mockCredentials = {
  token: 'valid-mock-token',
};

export const mockSecurityContext: SecurityContext = {
  principal: {
    id: 'user-123',
    name: 'Alice Developer',
    roles: ['developer', 'admin'],
    metadata: new Map([['permissions', [{ name: '*', scope: { target: '*', actions: [SecurityAction.READ] } }]]]),
  },
  roles: ['developer', 'admin'],
  permissions: [{ name: '*', scope: { target: '*', actions: [SecurityAction.READ] } }],
  sessionId: 'test-session-123',
  requestId: 'req-test-123',
  metadata: new Map(),
  timestamp: Date.now(),
};

export const mockSession: Session = {
  id: 'test-session-123',
  state: 'active',
  metadata: {
    createdAt: Date.now(),
  },
  attributes: {
    clientId: 'test-client',
  },
  context: {
    correlationId: 'corr-123',
    requestMetadata: {},
  },
  lastAccessedAt: Date.now(),
};

export const mockJsonRpcPayloads = {
  initialize: {
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
      protocolVersion: '2024-11-05',
      capabilities: {},
      clientInfo: { name: 'mcp-test-client', version: '1.0.0' },
    },
  },
  listTools: {
    jsonrpc: '2.0',
    id: 2,
    method: 'tools/list',
    params: {},
  },
  callStatusTool: {
    jsonrpc: '2.0',
    id: 3,
    method: 'tools/call',
    params: {
      name: 'status',
      arguments: {},
    },
  },
  listResources: {
    jsonrpc: '2.0',
    id: 4,
    method: 'resources/list',
    params: {},
  },
  readProjectResource: {
    jsonrpc: '2.0',
    id: 5,
    method: 'resources/read',
    params: {
      uri: 'memora://project',
    },
  },
  listPrompts: {
    jsonrpc: '2.0',
    id: 6,
    method: 'prompts/list',
    params: {},
  },
  getHandoffPrompt: {
    jsonrpc: '2.0',
    id: 7,
    method: 'prompts/get',
    params: {
      name: 'generate-handoff',
      arguments: {},
    },
  },
};
