import { MessageParser, MessageSerializer, MessageRouter, MessageDispatcher } from '../src/protocol';
import { StructuredLogger } from '../src/logging/logger';
import { ToolRegistry } from '../src/registry/tool';
import { performance } from 'perf_hooks';

describe('MCP Pipeline Performance Benchmarks', () => {
  const serverInfo = { name: 'benchmark-mcp', version: '1.0.0' };
  const logger = new StructuredLogger('benchmark', 'error');
  


  it('should parse 1000 JSON-RPC messages rapidly', () => {
    const payload = JSON.stringify({
      jsonrpc: '2.0',
      id: 'perf-1',
      method: 'testMethod',
      params: { x: 1, y: 2 },
    });

    const start = performance.now();
    for (let i = 0; i < 1000; i++) {
      const parsed = MessageParser.parse(payload);
      expect(parsed).toBeDefined();
    }
    const end = performance.now();
    const duration = end - start;

    logger.info(`Parser 1000 runs took: ${duration.toFixed(2)}ms`);
    // Safe execution threshold: 1000 parses must complete within 500ms in ordinary test nodes
    expect(duration).toBeLessThan(500);
  });

  it('should serialize 1000 JSON-RPC messages rapidly', () => {
    const msg = {
      jsonrpc: '2.0' as const,
      id: 'perf-2',
      result: { status: 'success', values: [1, 2, 3] },
    };

    const start = performance.now();
    for (let i = 0; i < 1000; i++) {
      const serialized = MessageSerializer.serialize(msg);
      expect(serialized).toBeDefined();
    }
    const end = performance.now();
    const duration = end - start;

    logger.info(`Serializer 1000 runs took: ${duration.toFixed(2)}ms`);
    expect(duration).toBeLessThan(500);
  });

  it('should process and dispatch 500 complete handshake sequences rapidly', async () => {
    const initPayload = JSON.stringify({
      jsonrpc: '2.0',
      id: 1,
      method: 'initialize',
      params: {
        protocolVersion: '2024-11-05',
        clientInfo: { name: 'benchmark-client', version: '1.0.0' },
        capabilities: {},
      },
    });

    const initializedPayload = JSON.stringify({
      jsonrpc: '2.0',
      method: 'initialized',
    });

    const start = performance.now();
    for (let i = 0; i < 500; i++) {
      // Re-instantiate to reset state
      const runRouter = new MessageRouter();
      const runDispatcher = new MessageDispatcher(runRouter, serverInfo, logger, new ToolRegistry());
      
      await runDispatcher.dispatch(initPayload);
      await runDispatcher.dispatch(initializedPayload);
    }
    const end = performance.now();
    const duration = end - start;

    logger.info(`500 full handshakes took: ${duration.toFixed(2)}ms`);
    // 500 dispatcher handshake sequences must finish in less than 500ms
    expect(duration).toBeLessThan(500);
  });
});
