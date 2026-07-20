import { ResourceRegistry } from '../src/registry/resource';
import { ResourceValidator, ResourceExecutor, ResourceDispatcher } from '../src/resource';
import { ResourceCategory, ResourceVisibility } from '../src/types/resource';
import {
  ResourceRegistrationError,
  ResourceValidationError,
  ResourceNotFoundError,
  ResourceExecutionError,
  ResourceOutputValidationError,
} from '../src/errors';
import { MemoraMcpServer } from '../src/server';
import { StructuredLogger } from '../src/logging/logger';
import { ListResourcesRequestSchema, ReadResourceRequestSchema } from '@modelcontextprotocol/sdk/types.js';

// Mock MCP SDK modules
jest.mock('@modelcontextprotocol/sdk/types.js', () => {
  return {
    CallToolRequestSchema: { method: 'tools/call' },
    ListToolsRequestSchema: { method: 'tools/list' },
    ListResourcesRequestSchema: { method: 'resources/list' },
    ReadResourceRequestSchema: { method: 'resources/read' },
  };
});

// Mock Server to capture and execute registered handlers
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockHandlers = new Map<any, any>();
jest.mock('@modelcontextprotocol/sdk/server/index.js', () => {
  return {
    Server: jest.fn().mockImplementation(() => {
      return {
        connect: jest.fn().mockResolvedValue(undefined),
        close: jest.fn().mockResolvedValue(undefined),
        onerror: jest.fn(),
        onclose: jest.fn(),
        setRequestHandler: jest.fn().mockImplementation((schema, handler) => {
          mockHandlers.set(schema, handler);
        }),
      };
    }),
  };
});

describe('MCP Resource Framework tests', () => {
  let registry: ResourceRegistry;
  let validator: ResourceValidator;
  let executor: ResourceExecutor;
  let dispatcher: ResourceDispatcher;
  const logger = new StructuredLogger('test', 'error');

  const mockContext = (params: Record<string, unknown> = {}) => ({
    requestId: 'req-123',
    sessionId: 'session-123',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger,
    params,
    metadata: new Map<string, unknown>(),
  });

  beforeEach(() => {
    registry = new ResourceRegistry();
    validator = new ResourceValidator();
    executor = new ResourceExecutor(registry);
    dispatcher = new ResourceDispatcher(registry);
    mockHandlers.clear();
  });

  describe('ResourceRegistry', () => {
    it('should register and retrieve resource definitions and metadata', () => {
      const mockResource = {
        uri: 'memora://test',
        name: 'test-res',
        handler: jest.fn().mockResolvedValue([]),
      };
      const mockMeta = {
        displayName: 'Test Display',
        categories: [ResourceCategory.PROJECT],
        visibility: ResourceVisibility.PUBLIC,
      };

      registry.registerResource(mockResource, mockMeta);
      expect(registry.hasResource('memora://test')).toBe(true);
      expect(registry.getResource('memora://test')).toBe(mockResource);
      expect(registry.getMetadata('memora://test')).toBe(mockMeta);
    });

    it('should throw ResourceRegistrationError on duplicate registrations', () => {
      const mockResource = {
        uri: 'memora://duplicate',
        name: 'dup',
        handler: jest.fn().mockResolvedValue([]),
      };

      registry.registerResource(mockResource);
      expect(() => registry.registerResource(mockResource)).toThrow(ResourceRegistrationError);
    });

    it('should unregister resources successfully', () => {
      const mockResource = {
        uri: 'memora://removable',
        name: 'rem',
        handler: jest.fn().mockResolvedValue([]),
      };

      registry.registerResource(mockResource);
      expect(registry.hasResource('memora://removable')).toBe(true);
      registry.unregisterResource('memora://removable');
      expect(registry.hasResource('memora://removable')).toBe(false);
    });

    it('should throw ResourceNotFoundError when unregistering unknown resource', () => {
      expect(() => registry.unregisterResource('memora://non-existent')).toThrow(ResourceNotFoundError);
    });

    it('should list all registered resources sorted alphabetically by URI', () => {
      const resB = { uri: 'memora://b', name: 'B', handler: jest.fn().mockResolvedValue([]) };
      const resA = { uri: 'memora://a', name: 'A', handler: jest.fn().mockResolvedValue([]) };
      const resC = { uri: 'memora://c', name: 'C', handler: jest.fn().mockResolvedValue([]) };

      registry.registerResource(resB);
      registry.registerResource(resA);
      registry.registerResource(resC);

      const list = registry.listResources();
      expect(list).toHaveLength(3);
      expect(list[0].uri).toBe('memora://a');
      expect(list[1].uri).toBe('memora://b');
      expect(list[2].uri).toBe('memora://c');
    });

    it('should support backward compatible register, lookup, and getAll methods', () => {
      const mockResource = {
        uri: 'memora://compat',
        name: 'compat',
        handler: jest.fn().mockResolvedValue('compat text contents'),
      };

      registry.register(mockResource);
      const found = registry.lookup('memora://compat');
      expect(found).toBeDefined();
      expect(found!.uri).toBe('memora://compat');

      const all = registry.getAll();
      expect(all).toHaveLength(1);
    });
  });

  describe('ResourceValidator', () => {
    it('should validate valid URI schemes and throw on malformed URIs', () => {
      expect(() => validator.validateUri('memora://project')).not.toThrow();
      expect(() => validator.validateUri('http://localhost:8080/res')).not.toThrow();
      expect(() => validator.validateUri('file:///var/tmp')).not.toThrow();
      
      expect(() => validator.validateUri('')).toThrow(ResourceValidationError);
      expect(() => validator.validateUri('invalid-uri-no-scheme')).toThrow(ResourceValidationError);
      expect(() => validator.validateUri('://missing-scheme')).toThrow(ResourceValidationError);
    });

    it('should validate standard ResourceContents structure and detect output malformations', () => {
      const validContents = [
        { uri: 'memora://res', mimeType: 'text/plain', text: 'hello' },
        { uri: 'memora://res', mimeType: 'application/octet-stream', blob: 'YmFzZTY0' },
      ];
      expect(() => validator.validateContents(validContents, 'memora://res')).not.toThrow();

      // Empty contents array
      expect(() => validator.validateContents([], 'memora://res')).toThrow(ResourceOutputValidationError);
      // Non-array results
      expect(() => validator.validateContents({ uri: 'memora://res', text: 'hi' }, 'memora://res')).toThrow(ResourceOutputValidationError);
      // Mismatching URI
      expect(() => validator.validateContents([{ uri: 'memora://other', text: 'hi' }], 'memora://res')).toThrow(ResourceOutputValidationError);
      // Missing both text and blob
      expect(() => validator.validateContents([{ uri: 'memora://res' }], 'memora://res')).toThrow(ResourceOutputValidationError);
    });
  });

  describe('ResourceExecutor & Dispatcher', () => {
    it('should execute registered resource handler and return result', async () => {
      const mockHandler = jest.fn().mockResolvedValue([
        { uri: 'memora://exec', text: 'successfully executed' },
      ]);
      registry.registerResource({
        uri: 'memora://exec',
        name: 'exec',
        handler: mockHandler,
      });

      const result = await executor.execute('memora://exec', mockContext());
      expect(result.contents[0].text).toBe('successfully executed');
      expect(mockHandler).toHaveBeenCalled();
    });

    it('should map unknown resource reads to ResourceNotFoundError', async () => {
      await expect(dispatcher.dispatchRead('memora://unknown', mockContext())).rejects.toThrow(ResourceNotFoundError);
    });

    it('should convert handler execution crashes into ResourceExecutionError', async () => {
      registry.registerResource({
        uri: 'memora://broken',
        name: 'broken',
        handler: jest.fn().mockRejectedValue(new Error('Internal database timeout')),
      });

      await expect(dispatcher.dispatchRead('memora://broken', mockContext())).rejects.toThrow(ResourceExecutionError);
    });
  });

  describe('Server & Protocol Integration', () => {
    it('should register placeholder resources returning placeholder texts', async () => {
      const baseConfig = {
        serverName: 'test-mcp',
        version: '1.0.0',
        host: '127.0.0.1',
        port: 8082,
        environment: 'test' as const,
        timeout: 5000,
        logLevel: 'error' as const,
        usePlaceholder: true,
      };
      const mockMcpTransport = {
        type: 'stdio' as const,
        initialize: jest.fn().mockResolvedValue(undefined),
        getTransportInstance: jest.fn().mockReturnValue({}),
        close: jest.fn().mockResolvedValue(undefined),
      };

      const server = new MemoraMcpServer(baseConfig, mockMcpTransport);
      server.initialize();

      const serverRegistry = server.getResourceRegistry();
      expect(serverRegistry.hasResource('memora://project')).toBe(true);
      expect(serverRegistry.hasResource('memora://architecture')).toBe(true);
      expect(serverRegistry.hasResource('memora://knowledge')).toBe(true);
      expect(serverRegistry.hasResource('memora://tasks')).toBe(true);
      expect(serverRegistry.hasResource('memora://decisions')).toBe(true);

      const res = await serverRegistry.getResource('memora://project')!.handler({}, mockContext());
      expect(res[0].text).toBe('This resource is not implemented yet.');
    });

    it('should respond to resources/list protocol requests', async () => {
      const baseConfig = {
        serverName: 'test-mcp',
        version: '1.0.0',
        host: '127.0.0.1',
        port: 8082,
        environment: 'test' as const,
        timeout: 5000,
        logLevel: 'error' as const,
        usePlaceholder: true,
      };
      const mockMcpTransport = {
        type: 'stdio' as const,
        initialize: jest.fn().mockResolvedValue(undefined),
        getTransportInstance: jest.fn().mockReturnValue({}),
        close: jest.fn().mockResolvedValue(undefined),
      };

      const server = new MemoraMcpServer(baseConfig, mockMcpTransport);
      server.initialize();

      const listHandler = mockHandlers.get(ListResourcesRequestSchema);
      expect(listHandler).toBeDefined();

      const listResult = await listHandler();
      expect(listResult.resources).toBeDefined();
      expect(listResult.resources.length).toBeGreaterThanOrEqual(5);
      expect(listResult.resources.map((r: { name: string }) => r.name)).toContain('project');
    });

    it('should respond to resources/read protocol requests', async () => {
      const baseConfig = {
        serverName: 'test-mcp',
        version: '1.0.0',
        host: '127.0.0.1',
        port: 8082,
        environment: 'test' as const,
        timeout: 5000,
        logLevel: 'error' as const,
        usePlaceholder: true,
      };
      const mockMcpTransport = {
        type: 'stdio' as const,
        initialize: jest.fn().mockResolvedValue(undefined),
        getTransportInstance: jest.fn().mockReturnValue({}),
        close: jest.fn().mockResolvedValue(undefined),
      };

      const server = new MemoraMcpServer(baseConfig, mockMcpTransport);
      server.initialize();

      const readHandler = mockHandlers.get(ReadResourceRequestSchema);
      expect(readHandler).toBeDefined();

      const readResult = await readHandler({ params: { uri: 'memora://architecture' } });
      expect(readResult.contents).toBeDefined();
      expect(readResult.contents[0].text).toBe('This resource is not implemented yet.');
    });
  });
});
