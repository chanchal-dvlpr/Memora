import { MessageRouter, MessageDispatcher, ProtocolState } from '../src/protocol';
import { JsonRpcErrorResponse } from '../src/types/jsonrpc';
import { StructuredLogger } from '../src/logging/logger';
import { ToolRegistry } from '../src/registry/tool';

describe('MCP Lifecycle & Handshake Flow', () => {
  let router: MessageRouter;
  let dispatcher: MessageDispatcher;

  const serverInfo = {
    name: 'memora-mcp-server',
    version: '1.0.0',
  };

  beforeEach(() => {
    router = new MessageRouter();
    dispatcher = new MessageDispatcher(
      router,
      serverInfo,
      new StructuredLogger('test', 'error'),
      new ToolRegistry()
    );
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const send = async (payload: string): Promise<any> => {
    const res = await dispatcher.dispatch(payload);
    return res ? JSON.parse(res) : null;
  };

  it('should initialize and negotiate capabilities successfully', async () => {
    const initPayload = JSON.stringify({
      jsonrpc: '2.0',
      id: 1,
      method: 'initialize',
      params: {
        protocolVersion: '2024-11-05',
        clientInfo: { name: 'test-client', version: '2.0.0' },
        capabilities: { roots: { listChanged: true } },
      },
    });

    const res = await send(initPayload);
    expect(res.error).toBeUndefined();
    expect(res.id).toBe(1);

    const result = res.result;
    expect(result.protocolVersion).toBe('2024-11-05');
    expect(result.serverInfo).toEqual(serverInfo);
    // Negotiated capabilities should all be false/empty
    expect(result.capabilities).toEqual({});

    expect(dispatcher.getSession().getState()).toBe(ProtocolState.INITIALIZING);
  });

  it('should reject unsupported protocol version', async () => {
    const initPayload = JSON.stringify({
      jsonrpc: '2.0',
      id: 1,
      method: 'initialize',
      params: {
        protocolVersion: '1.0.0',
        clientInfo: { name: 'test-client', version: '2.0.0' },
        capabilities: {},
      },
    });

    const res = (await send(initPayload)) as JsonRpcErrorResponse;
    expect(res.error).toBeDefined();
    expect(res.error.code).toBe(-32602); // Invalid Params
    expect(res.error.message).toContain('Unsupported protocol version');
  });

  it('should reject missing clientInfo', async () => {
    const initPayload = JSON.stringify({
      jsonrpc: '2.0',
      id: 1,
      method: 'initialize',
      params: {
        protocolVersion: '2024-11-05',
        capabilities: {},
      },
    });

    const res = (await send(initPayload)) as JsonRpcErrorResponse;
    expect(res.error).toBeDefined();
    expect(res.error.code).toBe(-32602);
  });

  it('should reject duplicate initialize requests', async () => {
    const initPayload = JSON.stringify({
      jsonrpc: '2.0',
      id: 1,
      method: 'initialize',
      params: {
        protocolVersion: '2024-11-05',
        clientInfo: { name: 'test-client', version: '2.0.0' },
        capabilities: {},
      },
    });

    await send(initPayload);
    const dupRes = (await send(initPayload)) as JsonRpcErrorResponse;

    expect(dupRes.error.message).toContain('initializing state');
  });

  it('should transition to INITIALIZED upon initialized notification', async () => {
    // 1. initialize
    await send(
      JSON.stringify({
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
          protocolVersion: '2024-11-05',
          clientInfo: { name: 'test-client', version: '2.0.0' },
          capabilities: {},
        },
      })
    );

    // 2. initialized
    const notifyRes = await send(
      JSON.stringify({
        jsonrpc: '2.0',
        method: 'initialized',
      })
    );

    expect(notifyRes).toBeNull(); // Notifications don't return response
    expect(dispatcher.getSession().getState()).toBe(ProtocolState.INITIALIZED);
  });

  it('should reject requests before initialize request', async () => {
    const customRequest = JSON.stringify({
      jsonrpc: '2.0',
      id: 10,
      method: 'custom-query',
    });

    const res = (await send(customRequest)) as JsonRpcErrorResponse;
    expect(res.error).toBeDefined();
    expect(res.error.code).toBe(-32600);
    expect(res.error.message).toContain('not initialized');
  });

  it('should support shutdown transition', async () => {
    // Handshake
    await send(
      JSON.stringify({
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
          protocolVersion: '2024-11-05',
          clientInfo: { name: 'test-client', version: '2.0.0' },
          capabilities: {},
        },
      })
    );
    await send(JSON.stringify({ jsonrpc: '2.0', method: 'initialized' }));

    // Shutdown
    const res = await send(
      JSON.stringify({
        jsonrpc: '2.0',
        id: 2,
        method: 'shutdown',
      })
    );

    expect(res.error).toBeUndefined();
    expect(res.result).toEqual({});
    expect(dispatcher.getSession().getState()).toBe(ProtocolState.SHUTDOWN_REQUESTED);
  });

  it('should throw Method Not Found on unknown methods', async () => {
    // Handshake
    await send(
      JSON.stringify({
        jsonrpc: '2.0',
        id: 1,
        method: 'initialize',
        params: {
          protocolVersion: '2024-11-05',
          clientInfo: { name: 'test-client', version: '2.0.0' },
          capabilities: {},
        },
      })
    );
    await send(JSON.stringify({ jsonrpc: '2.0', method: 'initialized' }));

    const res = await send(
      JSON.stringify({
        jsonrpc: '2.0',
        id: 5,
        method: 'non-existent-method',
      })
    );

    expect(res.error).toBeDefined();
    expect(res.error.code).toBe(-32601);
  });
});
