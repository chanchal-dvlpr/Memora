import { ResourceRegistry } from '../src/registry/resource';
import { ResourceValidator, ResourceDispatcher, normalizeUri, hasCircularReference } from '../src/resource';
import { ResourceCategory, ResourceVisibility, deepFreeze, ResourceCache, CacheEntry } from '../src/types/resource';
import {
  ResourceValidationError,
  ResourceOutputValidationError,
} from '../src/errors';
import { MemoraMcpServer } from '../src/server';
import { StructuredLogger } from '../src/logging/logger';
import { ListResourcesRequestSchema } from '@modelcontextprotocol/sdk/types.js';

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

describe('Advanced MCP Resource Framework', () => {
  let registry: ResourceRegistry;
  let validator: ResourceValidator;
  let dispatcher: ResourceDispatcher;
  const logger = new StructuredLogger('test', 'error');

  const mockContext = (params: Record<string, unknown> = {}) => ({
    requestId: 'advanced-req',
    sessionId: 'advanced-session',
    protocolVersion: '2024-11-05',
    timestamp: Date.now(),
    logger,
    params,
    metadata: new Map<string, unknown>(),
  });

  beforeEach(() => {
    registry = new ResourceRegistry();
    validator = new ResourceValidator();
    dispatcher = new ResourceDispatcher(registry);
    mockHandlers.clear();
  });

  describe('ResourceMetadata Immutability', () => {
    it('should freeze metadata using deepFreeze and prevent modifications', () => {
      const meta = {
        displayName: 'Test Meta',
        categories: [ResourceCategory.SYSTEM],
        visibility: ResourceVisibility.PUBLIC,
        annotations: { status: 'draft' },
      };

      const frozen = deepFreeze(meta);
      expect(Object.isFrozen(frozen)).toBe(true);
      expect(Object.isFrozen(frozen.annotations)).toBe(true);
      
      expect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (frozen as any).displayName = 'Changed';
      }).toThrow();

      expect(() => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (frozen.annotations as any).status = 'published';
      }).toThrow();
    });
  });

  describe('URI Normalization', () => {
    it('should normalize equivalent URIs canonically', () => {
      // Uppercase scheme/host normalization
      expect(normalizeUri('MEMORA://Project/Tasks')).toBe('memora://project/Tasks');
      
      // Sort query parameters
      expect(normalizeUri('memora://project?b=2&a=1')).toBe('memora://project?a=1&b=2');

      // Strip empty hash fragments
      expect(normalizeUri('memora://project#')).toBe('memora://project');
      
      // Basic normalization on invalid/custom URIs
      expect(normalizeUri('CUSTOM://Path')).toBe('custom://Path');
    });

    it('should reject malformed URIs during validation', () => {
      expect(() => validator.validateUri('no-scheme-uri')).toThrow(ResourceValidationError);
      expect(() => validator.validateUri('scheme://')).toThrow(ResourceValidationError);
    });
  });

  describe('MIME Type Support & Serialization Checks', () => {
    it('should validate supported MIME types and reject unknown ones', () => {
      expect(() => validator.validateMimeType('text/plain')).not.toThrow();
      expect(() => validator.validateMimeType('application/json')).not.toThrow();
      
      expect(() => validator.validateMimeType('image/png')).toThrow(ResourceValidationError);
    });

    it('should validate JSON serialization for application/json type', () => {
      const validJson = [{ uri: 'memora://res', mimeType: 'application/json', text: '{"key": "value"}' }];
      expect(() => validator.validateContents(validJson, 'memora://res')).not.toThrow();

      const invalidJson = [{ uri: 'memora://res', mimeType: 'application/json', text: 'invalid json' }];
      expect(() => validator.validateContents(invalidJson, 'memora://res')).toThrow(ResourceOutputValidationError);
    });
  });

  describe('Circular Reference Detection', () => {
    it('should detect circular references in objects', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const obj: any = { a: 1 };
      obj.self = obj;
      expect(hasCircularReference(obj)).toBe(true);

      const cleanObj = { a: 1, b: { c: 2 } };
      expect(hasCircularReference(cleanObj)).toBe(false);
    });

    it('should reject handler results containing circular references', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const circularContents: any = [{ uri: 'memora://res', text: 'ok' }];
      circularContents[0].self = circularContents;

      expect(() => validator.validateContents(circularContents, 'memora://res')).toThrow(ResourceOutputValidationError);
    });
  });

  describe('Middleware Execution ordering', () => {
    it('should execute middleware stack in deterministic onion order', async () => {
      const executionOrder: number[] = [];

      registry.registerResource({
        uri: 'memora://mid-test',
        name: 'mid-test',
        handler: async () => {
          executionOrder.push(3);
          return [{ uri: 'memora://mid-test', text: 'final response' }];
        },
      });

      dispatcher.use(async (_ctx, _res, next) => {
        executionOrder.push(1);
        const r = await next();
        executionOrder.push(5);
        return r;
      });

      dispatcher.use(async (_ctx, _res, next) => {
        executionOrder.push(2);
        const r = await next();
        executionOrder.push(4);
        return r;
      });

      const result = await dispatcher.dispatchRead('memora://mid-test', mockContext());
      expect(result.contents[0].text).toBe('final response');
      expect(executionOrder).toEqual([1, 2, 3, 4, 5]);
    });
  });

  describe('Caching Abstraction Interface', () => {
    it('should support CacheEntry, CachePolicy, and ResourceCache shapes', async () => {
      // Mock implementation of a simple cache
      const localStore = new Map<string, CacheEntry<string>>();
      const cache: ResourceCache<string> = {
        lookup: async (key) => localStore.get(key),
        store: async (key, data, policy) => {
          const ttl = policy?.ttlMs || 1000;
          localStore.set(key, {
            key,
            data,
            createdAt: Date.now(),
            expiresAt: Date.now() + ttl,
            etag: 'mock-etag',
          });
        },
        invalidate: async (key) => {
          localStore.delete(key);
        },
        clear: async () => {
          localStore.clear();
        },
      };

      await cache.store('key1', 'cached data', { ttlMs: 2000 });
      const entry = await cache.lookup('key1');
      expect(entry).toBeDefined();
      expect(entry!.data).toBe('cached data');
      expect(entry!.etag).toBe('mock-etag');

      await cache.invalidate('key1');
      const clearedEntry = await cache.lookup('key1');
      expect(clearedEntry).toBeUndefined();
    });
  });

  describe('Resource Discovery Metadata Verification', () => {
    it('should return categories, tags, annotations, and examples in resources/list', async () => {
      const baseConfig = {
        serverName: 'test-mcp',
        version: '1.0.0',
        host: '127.0.0.1',
        port: 8082,
        environment: 'test' as const,
        timeout: 5000,
        logLevel: 'error' as const,
      };
      const mockMcpTransport = {
        type: 'stdio' as const,
        initialize: jest.fn().mockResolvedValue(undefined),
        getTransportInstance: jest.fn().mockReturnValue({}),
        close: jest.fn().mockResolvedValue(undefined),
      };

      const server = new MemoraMcpServer(baseConfig, mockMcpTransport);
      server.initialize();

      // Register a custom resource with detailed metadata
      const serverRegistry = server.getResourceRegistry();
      serverRegistry.registerResource(
        {
          uri: 'memora://detailed',
          name: 'detailed',
          handler: async () => [{ uri: 'memora://detailed', text: 'data' }],
        },
        {
          displayName: 'Detailed Resource',
          categories: [ResourceCategory.PROJECT],
          visibility: ResourceVisibility.PUBLIC,
          tags: ['knowledge', 'core'],
          annotations: { stability: 'stable' },
          examples: [{ uri: 'memora://detailed', output: 'data' }],
        }
      );

      const listHandler = mockHandlers.get(ListResourcesRequestSchema);
      expect(listHandler).toBeDefined();

      const listResult = await listHandler();
      const detailedItem = listResult.resources.find((r: { name: string }) => r.name === 'detailed');
      expect(detailedItem).toBeDefined();
      expect(detailedItem.category).toBe(ResourceCategory.PROJECT);
      expect(detailedItem.tags).toContain('core');
      expect(detailedItem.annotations.stability).toBe('stable');
      expect(detailedItem.examples[0].uri).toBe('memora://detailed');
    });
  });
});
