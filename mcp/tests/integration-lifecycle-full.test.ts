import { MemoraMcpServer } from '../src/server';
import { ConfigLoader } from '../src/config';
import { StdioTransport } from '../src/transport';

describe('Integration: Full Server Lifecycle & Restart (Phase 13.9.6)', () => {
  it('should transition through UNINITIALIZED -> INITIALIZED -> STOPPED -> INITIALIZED lifecycle sequence', async () => {
    const config = { ...ConfigLoader.load(), environment: 'test' as const, usePlaceholder: true };
    const transport = new StdioTransport();
    const server = new MemoraMcpServer(config, transport);

    expect(server.getLifecycleState().toLowerCase()).toBe('uninitialized');

    server.initialize();
    expect(server.getLifecycleState().toLowerCase()).toBe('initialized');

    const health = server.getHealthManager().generateHealthReport();
    expect(health.status).toBe('healthy');

    await server.stop();
    expect(server.getLifecycleState().toLowerCase()).toBe('stopped');

    // Restart sequence with fresh server instance
    const newServer = new MemoraMcpServer(config, transport);
    newServer.initialize();
    expect(newServer.getLifecycleState().toLowerCase()).toBe('initialized');

    await newServer.stop();
    expect(newServer.getLifecycleState().toLowerCase()).toBe('stopped');
  });
});
