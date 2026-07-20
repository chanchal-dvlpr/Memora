import { MemoraMcpServer } from '../src/server';
import { McpTransport } from '../src/transport';
import {
  ConfigurationValidationError,
  TransportInitializationError,
} from '../src/errors';
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

describe('Startup Verification Flow', () => {
  const baseConfig = {
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

  it('should pass on correct config and initialization', async () => {
    const server = new MemoraMcpServer(baseConfig, mockTransport);
    server.initialize();
    await server.start();

    const metrics = server.getStartupMetrics();
    expect(metrics).not.toBeNull();
    expect(metrics?.registryInitMs).toBeDefined();
    expect(metrics?.transportInitMs).toBeDefined();
  });

  it('should throw ConfigurationValidationError on empty server name', () => {
    const invalidConfig = { ...baseConfig, serverName: '' };
    const server = new MemoraMcpServer(invalidConfig, mockTransport);

    expect(() => server.initialize()).toThrow(ConfigurationValidationError);
  });

  it('should throw ConfigurationValidationError on invalid port number', () => {
    const invalidConfig = { ...baseConfig, port: 99999 };
    const server = new MemoraMcpServer(invalidConfig, mockTransport);

    expect(() => server.initialize()).toThrow(ConfigurationValidationError);
  });

  it('should throw ConfigurationValidationError on invalid environment', () => {
    const invalidConfig = { ...baseConfig, environment: 'invalid-env' as unknown as 'test' };
    const server = new MemoraMcpServer(invalidConfig, mockTransport);

    expect(() => server.initialize()).toThrow(ConfigurationValidationError);
  });

  it('should throw ConfigurationValidationError on invalid log level', () => {
    const invalidConfig = { ...baseConfig, logLevel: 'invalid-level' as unknown as 'info' };
    const server = new MemoraMcpServer(invalidConfig, mockTransport);

    expect(() => server.initialize()).toThrow(ConfigurationValidationError);
  });

  it('should throw ConfigurationValidationError on negative timeout value', () => {
    const invalidConfig = { ...baseConfig, timeout: -100 };
    const server = new MemoraMcpServer(invalidConfig, mockTransport);

    expect(() => server.initialize()).toThrow(ConfigurationValidationError);
  });

  it('should throw TransportInitializationError if transport initialization fails', async () => {
    const failingTransport: McpTransport = {
      type: 'stdio',
      initialize: jest.fn().mockRejectedValue(new Error('Device locked')),
      getTransportInstance: jest.fn().mockReturnValue({} as unknown as Transport),
      close: jest.fn().mockResolvedValue(undefined),
    };

    const server = new MemoraMcpServer(baseConfig, failingTransport);
    server.initialize();

    await expect(server.start()).rejects.toThrow(TransportInitializationError);
  });
});
