import { MemoraMcpServer } from '../src/server';
import { McpTransport } from '../src/transport';
import { Transport } from '@modelcontextprotocol/sdk/shared/transport.js';

jest.mock('@modelcontextprotocol/sdk/types.js', () => {
  return {
    CallToolRequestSchema: { method: 'tools/call' },
    ListToolsRequestSchema: { method: 'tools/list' },
    ListResourcesRequestSchema: { method: 'resources/list' },
    ReadResourceRequestSchema: { method: 'resources/read' },
  };
});

jest.mock('@modelcontextprotocol/sdk/server/index.js', () => {
  return {
    Server: jest.fn().mockImplementation(() => {
      return {
        connect: jest.fn().mockResolvedValue(undefined),
        close: jest.fn().mockResolvedValue(undefined),
        onerror: jest.fn(),
        onclose: jest.fn(),
        setRequestHandler: jest.fn(),
      };
    }),
  };
});

describe('Readiness & Validation Inspection', () => {
  const testConfig = {
    serverName: 'test-mcp',
    version: '1.0.0',
    host: '127.0.0.1',
    port: 8082,
    environment: 'test' as const,
    timeout: 5000,
    logLevel: 'error' as const,
  };

  let mockTransport: jest.Mocked<McpTransport>;

  beforeEach(() => {
    mockTransport = {
      type: 'stdio',
      initialize: jest.fn().mockResolvedValue(undefined),
      getTransportInstance: jest.fn().mockReturnValue({} as unknown as Transport),
      close: jest.fn().mockResolvedValue(undefined),
    };
  });

  it('should generate complete readiness reports', async () => {
    const server = new MemoraMcpServer(testConfig, mockTransport);
    
    // Check initial state
    let report = server.generateReadinessReport();
    expect(report.lifecycleState).toBe('UNINITIALIZED');
    expect(report.loggerReady).toBe(true);
    expect(report.registriesReady).toBe(true);
    expect(report.transportReady).toBe(true);

    // Initialize and check state
    server.initialize();
    report = server.generateReadinessReport();
    expect(report.lifecycleState).toBe('INITIALIZED');

    // Start and check state
    await server.start();
    report = server.generateReadinessReport();
    expect(report.lifecycleState).toBe('STARTED');
  });

  it('should correctly measure registry compile and transport load durations', async () => {
    const server = new MemoraMcpServer(testConfig, mockTransport);
    server.initialize();
    await server.start();

    const metrics = server.getStartupMetrics();
    expect(metrics).not.toBeNull();
    expect(metrics?.configLoadMs).toBeGreaterThan(0);
    expect(metrics?.registryInitMs).toBeGreaterThanOrEqual(0);
    expect(metrics?.transportInitMs).toBeGreaterThanOrEqual(0);
    expect(metrics?.totalStartupMs).toBeGreaterThan(0);
  });
});
