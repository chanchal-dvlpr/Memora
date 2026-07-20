import { MemoraMcpServer } from '../src/server';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { McpTransport } from '../src/transport';
import { Transport } from '@modelcontextprotocol/sdk/shared/transport.js';

jest.mock('@modelcontextprotocol/sdk/types.js', () => {
  return {
    CallToolRequestSchema: { method: 'tools/call' },
    ListToolsRequestSchema: { method: 'tools/list' },
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

describe('MemoraMcpServer Bootstrap', () => {
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
    jest.clearAllMocks();

    mockTransport = {
      type: 'stdio',
      initialize: jest.fn().mockResolvedValue(undefined),
      getTransportInstance: jest.fn().mockReturnValue({} as unknown as Transport),
      close: jest.fn().mockResolvedValue(undefined),
    };
  });

  it('should initialize successfully', () => {
    const serverWrapper = new MemoraMcpServer(testConfig, mockTransport);
    expect(serverWrapper.getServerInstance()).toBeNull();

    serverWrapper.initialize();
    expect(serverWrapper.getServerInstance()).not.toBeNull();
    expect(Server).toHaveBeenCalledWith(
      { name: 'test-mcp', version: '1.0.0' },
      { capabilities: { tools: {}, resources: {}, prompts: {} } }
    );
  });

  it('should start successfully when initialized', async () => {
    const serverWrapper = new MemoraMcpServer(testConfig, mockTransport);
    serverWrapper.initialize();

    const serverInstance = serverWrapper.getServerInstance()!;
    
    await serverWrapper.start();
    expect(mockTransport.initialize).toHaveBeenCalled();
    expect(mockTransport.getTransportInstance).toHaveBeenCalled();
    expect(serverInstance.connect).toHaveBeenCalledWith(mockTransport.getTransportInstance());
  });

  it('should throw error when starting uninitialized server', async () => {
    const serverWrapper = new MemoraMcpServer(testConfig, mockTransport);
    await expect(serverWrapper.start()).rejects.toThrow(
      'Server is UNINITIALIZED. Call initialize() first.'
    );
  });

  it('should stop successfully and clear instances', async () => {
    const serverWrapper = new MemoraMcpServer(testConfig, mockTransport);
    serverWrapper.initialize();
    await serverWrapper.start();

    const serverInstance = serverWrapper.getServerInstance()!;

    await serverWrapper.stop();
    expect(serverWrapper.getServerInstance()).toBeNull();
    expect(serverInstance.close).toHaveBeenCalled();
    expect(mockTransport.close).toHaveBeenCalled();
  });
});
